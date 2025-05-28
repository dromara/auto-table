package org.dromara.autotable.core.strategy.oracle.builder;

import lombok.NonNull;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.annotation.enums.IndexSortTypeEnum;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.strategy.oracle.data.OracleColumnMetadata;
import org.dromara.autotable.core.strategy.oracle.data.OracleCompareTableInfo;
import org.dromara.autotable.core.strategy.oracle.data.OracleTableMetadata;
import org.dromara.autotable.core.strategy.oracle.data.dbdata.InformationSchemaColumn;
import org.dromara.autotable.core.strategy.oracle.data.dbdata.InformationSchemaConstraint;
import org.dromara.autotable.core.strategy.oracle.data.dbdata.InformationSchemaIndex;
import org.dromara.autotable.core.strategy.oracle.data.dbdata.InformationSchemaTable;
import org.dromara.autotable.core.strategy.oracle.mapper.OracleTablesMapper;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CompareTableBuilder {
    private final static OracleTablesMapper MAPPER = new OracleTablesMapper();

    public static @NonNull OracleCompareTableInfo build(OracleTableMetadata tableMetadata) {
        OracleCompareTableInfo oracleCompareTableInfo = new OracleCompareTableInfo(tableMetadata.getTableName(), tableMetadata.getSchema());

        String tableName = tableMetadata.getTableName();
        // 查询库中表定义信息
        InformationSchemaTable informationSchemaTable = MAPPER.findTableByTableName(tableName);
        List<InformationSchemaColumn> tableColumns = MAPPER.findTableColumnByTableName(tableName);
        List<InformationSchemaIndex> tableIndexes = MAPPER.findTableIndexByTableName(tableName);
        List<InformationSchemaConstraint> primaryKeys = MAPPER.findTableConstraintByTableName(tableName, "P");

        compareTableDef(oracleCompareTableInfo, tableMetadata, informationSchemaTable);
        compareTableColumnDef(oracleCompareTableInfo, tableMetadata, tableColumns);
        compareTableIndexDef(oracleCompareTableInfo, tableMetadata, tableIndexes);
        compareTablePkDef(oracleCompareTableInfo, tableMetadata, primaryKeys);

        return oracleCompareTableInfo;
    }

    private static void compareTablePkDef(OracleCompareTableInfo oracleCompareTableInfo, OracleTableMetadata tableMetadata, List<InformationSchemaConstraint> primaryKeys) {
        // 比较主键的方法
        // 1. 获取实体类中的主键信息
        List<OracleColumnMetadata> entityPrimaryKeys = tableMetadata.getColumnMetadataList().stream().filter(ColumnMetadata::isPrimary).collect(Collectors.toList());
        Set<String> entityPkNames = entityPrimaryKeys.stream()
                .map(pk -> pk.getName().toUpperCase())
                .collect(Collectors.toSet());

        // 2. 获取数据库中的主键信息
        Set<String> dbPkNames = primaryKeys == null ? Collections.emptySet() :
                primaryKeys.stream()
                        .map(pk -> pk.getColumnName().toUpperCase())
                        .collect(Collectors.toSet());

        // 3. 判断是否需要删除主键（数据库有，实体没有，或主键字段不一致）
        boolean needDropPrimary = false;
        if (!dbPkNames.isEmpty()) {
            if (entityPkNames.isEmpty() || !dbPkNames.equals(entityPkNames)) {
                needDropPrimary = true;
            }
        }
        oracleCompareTableInfo.setDropPrimary(needDropPrimary);

        // 4. 判断是否需要新增主键（实体有，数据库没有，或主键字段不一致）
        if (!entityPkNames.isEmpty()) {
            if (dbPkNames.isEmpty() || !dbPkNames.equals(entityPkNames)) {
                // 需要新增主键
                oracleCompareTableInfo.setNewPrimaries(entityPrimaryKeys);
            }
        }
    }

    private static void compareTableIndexDef(OracleCompareTableInfo oracleCompareTableInfo, OracleTableMetadata tableMetadata, List<InformationSchemaIndex> tableIndexs) {
        // 比较实体类与数据库中的索引定义，找出需要新增、删除或修改的索引
        // 1. 获取实体类中的索引定义
        List<IndexMetadata> entityIndexes = tableMetadata.getIndexMetadataList();
        Map<String, IndexMetadata> entityIndexMap = entityIndexes.stream().collect(Collectors.toMap(it -> it.getName().toUpperCase(), Function.identity()));

        // 2. 获取数据库中的索引定义
        Map<String, IndexMetadata> dbIndexMap = tableIndexs.stream().collect(Collectors.groupingBy(InformationSchemaIndex::getIndexName))
                .values()
                .stream().map(it -> {
                    IndexMetadata indexMetadata = new IndexMetadata();
                    indexMetadata.setType(it.get(0).isUnique() ? IndexTypeEnum.UNIQUE : IndexTypeEnum.NORMAL);
                    indexMetadata.setName(it.get(0).getIndexName());
                    indexMetadata.setColumns(it.stream().map(col -> IndexMetadata.IndexColumnParam.newInstance(col.getColumnName(), IndexSortTypeEnum.valueOf(col.getDescend()))).collect(Collectors.toList()));
                    return indexMetadata;
                }).collect(Collectors.toMap(IndexMetadata::getName, Function.identity()));

        // 3. 检查数据库中存在但实体类中不存在的索引（需要删除）
        for (String dbIndex : dbIndexMap.keySet()) {
            String dbIndexName = dbIndex.toUpperCase();
            if (!entityIndexMap.containsKey(dbIndexName)) {
                oracleCompareTableInfo.getDropIndexList().add(dbIndexName);
            }
        }

        // 4. 检查实体类中存在但数据库中不存在的索引（需要新增）
        for (IndexMetadata entityIndex : entityIndexes) {
            String entityIndexName = entityIndex.getName().toUpperCase();
            if (!dbIndexMap.containsKey(entityIndexName)) {
                oracleCompareTableInfo.getIndexMetadataList().add(entityIndex);
            } else {
                // 5. 检查索引定义是否有变化（如字段、唯一性等）
                IndexMetadata dbIndex = dbIndexMap.get(entityIndexName);
                boolean uniqueChanged = entityIndex.isUnique() != dbIndex.isUnique();
                // 字段顺序和内容是否一致
                List<String> entityColumns = entityIndex.getColumns().stream().map(it -> it.getSort() + "-" + it.getColumn()).collect(Collectors.toList());
                List<String> dbColumns = dbIndex.getColumns().stream().map(it -> it.getSort() + "-" + it.getColumn()).collect(Collectors.toList());
                boolean columnsChanged = !entityColumns.equals(dbColumns);

                if (uniqueChanged || columnsChanged) {
                    // 先删除再新增
                    oracleCompareTableInfo.getDropIndexList().add(entityIndexName);
                    oracleCompareTableInfo.getIndexMetadataList().add(entityIndex);
                }
            }
        }
    }

    private static void compareTableColumnDef(OracleCompareTableInfo oracleCompareTableInfo, OracleTableMetadata tableMetadata, List<InformationSchemaColumn> tableColumns) {
        // 实体类识别到的字段定义
        List<OracleColumnMetadata> columnMetadataList = tableMetadata.getColumnMetadataList();
        Map<String, OracleColumnMetadata> columnParamMap = columnMetadataList.stream().collect(Collectors.toMap(it -> it.getName().toUpperCase(), Function.identity()));
        // 字段位置变更处理
        ColumnPositionHelper.generateChangePosition(tableColumns, columnMetadataList);
        for (InformationSchemaColumn informationSchemaColumn : tableColumns) {
            String columnName = informationSchemaColumn.getColumnName();
            // 以数据库字段名，从当前Bean上取信息，获取到就从中剔除
            OracleColumnMetadata columnMetadata = columnParamMap.remove(columnName);
            if (columnMetadata != null) {
                // 取到了，则进行字段配置的比对
                boolean columnPositionChanged = columnMetadata.getNewPreColumn() != null;
                boolean commentChanged = isCommentChanged(informationSchemaColumn, columnMetadata);
                boolean fieldTypeChanged = isFieldTypeChanged(informationSchemaColumn, columnMetadata);
                boolean notNullChanged = columnMetadata.isNotNull() != informationSchemaColumn.isNotNull();
                boolean defaultValueChanged = isDefaultValueChanged(informationSchemaColumn, columnMetadata);
                if (columnPositionChanged || fieldTypeChanged || notNullChanged || defaultValueChanged) {
                    // 任何一项有变化，则说明需要更新该字段
                    columnMetadata.setOriginalColumn(informationSchemaColumn);
                    oracleCompareTableInfo.addEditColumnMetadata(columnMetadata);
                }
                // 注释
                if (commentChanged) {
                    oracleCompareTableInfo.addColumnComment(tableMetadata, columnMetadata);
                }
            } else {
                // 没有取到对应字段，说明库中存在的字段，Bean上不存在，根据配置，决定是否删除库上的多余字段
                if (AutoTableGlobalConfig.getAutoTableProperties().getAutoDropColumn()) {
                    oracleCompareTableInfo.getDropColumnList().add(columnName);
                }
            }
        }
        // 因为按照表中字段已经晒过一轮Bean上的字段了，同名可以取到的均删除了，剩下的都是表中字段不存在的，需要新增
        Collection<OracleColumnMetadata> needNewColumns = columnParamMap.values();
        for (OracleColumnMetadata needNewColumn : needNewColumns) {
            oracleCompareTableInfo.addNewColumnMetadata(needNewColumn);
            oracleCompareTableInfo.addColumnComment(tableMetadata, needNewColumn);
        }
    }

    private static void compareTableDef(OracleCompareTableInfo oracleCompareTableInfo, OracleTableMetadata tableMetadata, InformationSchemaTable informationSchemaTable) {
        if (!Objects.equals(tableMetadata.getComment(), informationSchemaTable.getComments())) {
            oracleCompareTableInfo.addTableComment(tableMetadata);
        }
    }


    private static boolean isCommentChanged(InformationSchemaColumn informationSchemaColumn, OracleColumnMetadata oracleColumnMetadata) {
        String fieldComment = oracleColumnMetadata.getComment();
        String dbColumnComment = informationSchemaColumn.getComments();
        return (StringUtils.hasText(fieldComment) || StringUtils.hasText(dbColumnComment)) && !fieldComment.equals(dbColumnComment);
    }

    /**
     * 字段类型比对是否需要改变
     */
    private static boolean isFieldTypeChanged(InformationSchemaColumn informationSchemaColumn, OracleColumnMetadata mysqlColumnMetadata) {

        DatabaseTypeAndLength fieldType = mysqlColumnMetadata.getType();
        String dataType = informationSchemaColumn.getDataType();
        Integer dataLength = informationSchemaColumn.getDataLength();
        Integer dataPrecision = informationSchemaColumn.getDataPrecision();

        // 字段类型比对逻辑
        // 1. 比较类型名（忽略大小写）
        if (!fieldType.getType().equalsIgnoreCase(dataType)) {
            return true;
        }
        // 2. 比较长度（对于有长度的类型）
        // 处理字符类型
        if (fieldType.getLength() != null && dataLength != null) {
            if (!fieldType.getLength().equals(dataLength)) {
                return true;
            }
        }
        // 3. 比较精度（对于数字类型）
        if (fieldType.getDecimalLength() != null && dataPrecision != null) {
            if (!fieldType.getDecimalLength().equals(dataPrecision)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDefaultValueChanged(InformationSchemaColumn informationSchemaColumn, OracleColumnMetadata mysqlColumnMetadata) {
        String columnDefault = informationSchemaColumn.getDataDefault();
        DefaultValueEnum defaultValueType = mysqlColumnMetadata.getDefaultValueType();
        if (DefaultValueEnum.isValid(defaultValueType)) {
            // 需要设置为null，但是数据库当前不是null
            if (defaultValueType == DefaultValueEnum.NULL) {
                return columnDefault != null;
            }
            // 需要设置为空字符串，但是数据库当前不是空字符串
            if (defaultValueType == DefaultValueEnum.EMPTY_STRING) {
                return !"".equals(columnDefault);
            }
        } else {
            String defaultValue = ColumnDefaultBuilder.handle(mysqlColumnMetadata.getDefaultValue(), mysqlColumnMetadata.getFieldType());
            return !Objects.equals(defaultValue, columnDefault);
        }
        return false;
    }
}
