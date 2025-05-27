package org.dromara.autotable.core.strategy.dm;

import lombok.NonNull;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.Utils;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.strategy.dm.builder.DmCreateTableSqlBuilder;
import org.dromara.autotable.core.strategy.dm.builder.DmModifyTableSqlBuilder;
import org.dromara.autotable.core.strategy.dm.builder.DmTableMetadataBuilder;
import org.dromara.autotable.core.strategy.dm.data.DmCompareTableInfo;
import org.dromara.autotable.core.strategy.dm.data.DmDefaultTypeEnum;
import org.dromara.autotable.core.strategy.dm.data.dbdata.DmDbColumn;
import org.dromara.autotable.core.strategy.dm.data.dbdata.DmDbIndex;
import org.dromara.autotable.core.strategy.dm.data.dbdata.DmDbPrimary;
import org.dromara.autotable.core.strategy.dm.mapper.DmTablesMapper;
import org.dromara.autotable.core.utils.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 达梦数据库策略实现
 */
public class DmStrategy implements IStrategy<DefaultTableMetadata, DmCompareTableInfo> {

    private final DmTablesMapper mapper = new DmTablesMapper();

    public static String withSchemaName(String schema, String... names) {
        String name = String.join(".", names);
        return StringUtils.hasText(schema) ? "\"" + schema + "\"" + "." + "\"" + name + "\"" : "\"" + name + "\"";
    }

    @Override
    public String databaseDialect() {
        return DatabaseDialect.DM;
    }

    @Override
    public Map<Class<?>, DefaultTypeEnumInterface> typeMapping() {
        return new HashMap<Class<?>, DefaultTypeEnumInterface>(32) {{
            put(String.class, DmDefaultTypeEnum.VARCHAR);
            put(Character.class, DmDefaultTypeEnum.CHAR);
            put(char.class, DmDefaultTypeEnum.CHAR);

            put(Short.class, DmDefaultTypeEnum.SMALLINT);
            put(short.class, DmDefaultTypeEnum.SMALLINT);
            put(int.class, DmDefaultTypeEnum.INTEGER);
            put(Integer.class, DmDefaultTypeEnum.INTEGER);
            put(Long.class, DmDefaultTypeEnum.BIGINT);
            put(long.class, DmDefaultTypeEnum.BIGINT);
            put(BigInteger.class, DmDefaultTypeEnum.BIGINT);

            put(Boolean.class, DmDefaultTypeEnum.BIT);
            put(boolean.class, DmDefaultTypeEnum.BIT);

            put(Float.class, DmDefaultTypeEnum.FLOAT);
            put(float.class, DmDefaultTypeEnum.FLOAT);
            put(Double.class, DmDefaultTypeEnum.DOUBLE);
            put(double.class, DmDefaultTypeEnum.DOUBLE);
            put(BigDecimal.class, DmDefaultTypeEnum.DECIMAL);

            put(Date.class, DmDefaultTypeEnum.TIMESTAMP);
            put(java.sql.Date.class, DmDefaultTypeEnum.DATE);
            put(java.sql.Timestamp.class, DmDefaultTypeEnum.TIMESTAMP);
            put(LocalDateTime.class, DmDefaultTypeEnum.TIMESTAMP);
            put(LocalDate.class, DmDefaultTypeEnum.DATE);
            put(LocalTime.class, DmDefaultTypeEnum.TIME);
            put(java.sql.Time.class, DmDefaultTypeEnum.TIME);
        }};
    }

    @Override
    public String dropTable(String schema, String tableName) {
        return String.format("DROP TABLE IF EXISTS \"%s\"", withSchemaName(schema, tableName));
    }

    @Override
    public @NonNull DefaultTableMetadata analyseClass(Class<?> beanClass) {
        return new DmTableMetadataBuilder().build(beanClass);
    }

    @Override
    public List<String> createTable(DefaultTableMetadata tableMetadata) {
        return DmCreateTableSqlBuilder.buildSql(tableMetadata);
    }

    @Override
    public @NonNull DmCompareTableInfo compareTable(DefaultTableMetadata tableMetadata) {
        String tableName = tableMetadata.getTableName();
        String schema = tableMetadata.getSchema();

        DmCompareTableInfo compareInfo = new DmCompareTableInfo(tableName, schema);

        // 比较表基本信息
        compareTableInfo(tableMetadata, compareInfo);

        // 比较字段信息
        compareColumnInfo(tableMetadata, compareInfo);

        // 比较索引信息
        compareIndexInfo(tableMetadata, compareInfo);

        return compareInfo;
    }

    @Override
    public boolean checkTableNotExist(String schema, String tableName) {
        return DataSourceManager.useConnection(connection -> {
            try {
                boolean exist = Utils.tableIsExists(connection, schema, tableName, new String[]{"TABLE", "PARTITIONED" +
                        " TABLE"}, true);
                return !exist;
            } catch (SQLException e) {
                throw new RuntimeException("判断数据库是否存在出错", e);
            }
        });
    }

    private void compareTableInfo(DefaultTableMetadata metadata, DmCompareTableInfo compareInfo) {
        String tableComment = mapper.selectTableDescription(metadata.getSchema(), metadata.getTableName());
        if (!Objects.equals(tableComment, metadata.getComment())) {
            compareInfo.setComment(metadata.getComment());
        }
    }

    private void compareColumnInfo(DefaultTableMetadata metadata, DmCompareTableInfo compareInfo) {
        String schema = metadata.getSchema();
        String tableName = metadata.getTableName();

        // 获取数据库字段信息
        List<DmDbColumn> dbColumns = mapper.selectTableColumns(schema, tableName);
        Map<String, DmDbColumn> columnMap = dbColumns.stream()
                .collect(Collectors.toMap(DmDbColumn::getName, Function.identity()));

        // 处理字段差异
        for (ColumnMetadata column : metadata.getColumnMetadataList()) {
            String colName = column.getName();
            DmDbColumn dbColumn = columnMap.remove(colName);

            if (dbColumn == null) {
                // 新增字段
                compareInfo.addNewColumn(column);
                compareInfo.addColumnComment(colName, column.getComment());
                continue;
            }

            // 检查注释变更
            if (!Objects.equals(dbColumn.getComment(), column.getComment())) {
                compareInfo.addColumnComment(colName, column.getComment());
            }

            // 检查字段定义变更
            if (isColumnDefinitionChanged(column, dbColumn)) {
                compareInfo.addModifyColumn(column);
            }
        }

        // 处理需要删除的字段
        if (AutoTableGlobalConfig.instance().getAutoTableProperties().getAutoDropColumn()) {
            compareInfo.addDropColumns(columnMap.keySet());
        }

        // 处理主键变更
        handlePrimaryKeyChange(metadata, compareInfo, schema, tableName);
    }

    private boolean isColumnDefinitionChanged(ColumnMetadata newCol, DmDbColumn oldCol) {
        // 类型检查
        String newType = newCol.getType().getDefaultFullType().toUpperCase();
        String oldType = oldCol.getType().toUpperCase();
        if (!newType.equals(oldType)) {
            return true;
        }

        // 非空检查
        boolean newNotNull = newCol.isNotNull();
        boolean oldNotNull = "N".equals(oldCol.getNullable());
        if (newNotNull != oldNotNull) {
            return true;
        }

        // 默认值检查
        String newDefault = getProcessedDefault(newCol);
        String oldDefault = processDbDefault(oldCol.getDefaultValue());
        return !Objects.equals(newDefault, oldDefault);
    }

    private String getProcessedDefault(ColumnMetadata column) {
        if (column.isAutoIncrement()) {
            return null;
        }
        return column.getDefaultValue();
    }

    private String processDbDefault(String dbDefault) {
        if (dbDefault == null) {
            return null;
        }
        // 处理达梦默认值中的函数调用
        if (dbDefault.startsWith("NEXTVAL(")) {
            return dbDefault;
        }
        return dbDefault.replace("'", "");
    }

    private void handlePrimaryKeyChange(DefaultTableMetadata metadata, DmCompareTableInfo compareInfo,
                                        String schema, String tableName) {
        // 获取数据库主键信息
        DmDbPrimary dbPrimary = mapper.selectPrimaryKey(schema, tableName);
        Set<String> dbPkColumns = dbPrimary != null ?
                new HashSet<>(Arrays.asList(dbPrimary.getColumns().split(","))) : Collections.emptySet();

        // 获取新主键信息
        Set<String> newPkColumns = metadata.getColumnMetadataList().stream()
                .filter(ColumnMetadata::isPrimary)
                .map(ColumnMetadata::getName)
                .collect(Collectors.toSet());

        // 主键变更处理
        if (!dbPkColumns.equals(newPkColumns)) {
            if (dbPrimary != null) {
                compareInfo.setDropPrimaryKeyName(dbPrimary.getPrimaryName());
            }
            if (!newPkColumns.isEmpty()) {
                compareInfo.addNewPrimary(metadata.getColumnMetadataList().stream()
                        .filter(ColumnMetadata::isPrimary)
                        .collect(Collectors.toList()));
            }
        }
    }

    private void compareIndexInfo(DefaultTableMetadata metadata, DmCompareTableInfo compareInfo) {
        String schema = metadata.getSchema();
        String tableName = metadata.getTableName();

        // 获取数据库索引信息
        List<DmDbIndex> dbIndexes = mapper.selectTableIndexes(schema, tableName);
        Map<String, DmDbIndex> indexMap = dbIndexes.stream()
                .collect(Collectors.toMap(DmDbIndex::getIndexName, Function.identity()));

        // 处理索引差异
        for (IndexMetadata newIndex : metadata.getIndexMetadataList()) {
            String indexName = newIndex.getName();
            DmDbIndex dbIndex = indexMap.remove(indexName);

            if (dbIndex == null) {
                compareInfo.addNewIndex(newIndex);
                continue;
            }

            // 检查索引定义是否变更
            if (isIndexChanged(newIndex, dbIndex)) {
                compareInfo.addModifyIndex(newIndex);
            }
        }

        // 处理需要删除的索引
        if (AutoTableGlobalConfig.instance().getAutoTableProperties().getAutoDropIndex()) {
            compareInfo.addDropIndexes(indexMap.keySet());
        }
    }

    private boolean isIndexChanged(IndexMetadata newIndex, DmDbIndex oldIndex) {
        // 检查索引类型
        if (!newIndex.getType().name().equals(oldIndex.getIndexType())) {
            return true;
        }
        // 检查包含字段
        String newColumns = newIndex.getColumns().stream()
                .map(c -> c.getColumn() + (c.getSort() != null ? " " + c.getSort() : ""))
                .collect(Collectors.joining(", "));
        return !newColumns.equals(oldIndex.getColumns());
    }

    @Override
    public List<String> modifyTable(DmCompareTableInfo compareInfo) {
        return DmModifyTableSqlBuilder.buildSql(compareInfo);
    }
}
