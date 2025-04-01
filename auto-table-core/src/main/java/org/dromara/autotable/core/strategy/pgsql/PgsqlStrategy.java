package org.dromara.autotable.core.strategy.pgsql;

import lombok.NonNull;
import org.apache.ibatis.session.Configuration;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.Utils;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
import org.dromara.autotable.core.dynamicds.SqlSessionFactoryManager;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.strategy.pgsql.builder.CreateTableSqlBuilder;
import org.dromara.autotable.core.strategy.pgsql.builder.ModifyTableSqlBuilder;
import org.dromara.autotable.core.strategy.pgsql.builder.PgsqlTableMetadataBuilder;
import org.dromara.autotable.core.strategy.pgsql.data.PgsqlCompareTableInfo;
import org.dromara.autotable.core.strategy.pgsql.data.PgsqlDefaultTypeEnum;
import org.dromara.autotable.core.strategy.pgsql.data.dbdata.PgsqlDbColumn;
import org.dromara.autotable.core.strategy.pgsql.data.dbdata.PgsqlDbIndex;
import org.dromara.autotable.core.strategy.pgsql.data.dbdata.PgsqlDbPrimary;
import org.dromara.autotable.core.strategy.pgsql.mapper.PgsqlTablesMapper;
import org.dromara.autotable.core.utils.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author don
 */
public class PgsqlStrategy implements IStrategy<DefaultTableMetadata, PgsqlCompareTableInfo, PgsqlTablesMapper> {

    @Override
    public String databaseDialect() {
        return DatabaseDialect.PostgreSQL;
    }

    @Override
    public Map<Class<?>, DefaultTypeEnumInterface> typeMapping() {
        return new HashMap<Class<?>, DefaultTypeEnumInterface>(32) {{
            put(String.class, PgsqlDefaultTypeEnum.VARCHAR);
            put(Character.class, PgsqlDefaultTypeEnum.CHAR);
            put(char.class, PgsqlDefaultTypeEnum.CHAR);

            put(Short.class, PgsqlDefaultTypeEnum.INT2);
            put(short.class, PgsqlDefaultTypeEnum.INT2);
            put(int.class, PgsqlDefaultTypeEnum.INT4);
            put(Integer.class, PgsqlDefaultTypeEnum.INT4);
            put(Long.class, PgsqlDefaultTypeEnum.INT8);
            put(long.class, PgsqlDefaultTypeEnum.INT8);
            put(BigInteger.class, PgsqlDefaultTypeEnum.INT8);

            put(Boolean.class, PgsqlDefaultTypeEnum.BOOL);
            put(boolean.class, PgsqlDefaultTypeEnum.BOOL);

            put(Float.class, PgsqlDefaultTypeEnum.FLOAT4);
            put(float.class, PgsqlDefaultTypeEnum.FLOAT4);
            put(Double.class, PgsqlDefaultTypeEnum.FLOAT8);
            put(double.class, PgsqlDefaultTypeEnum.FLOAT8);
            put(BigDecimal.class, PgsqlDefaultTypeEnum.NUMERIC);

            put(Date.class, PgsqlDefaultTypeEnum.TIMESTAMP);
            put(java.sql.Date.class, PgsqlDefaultTypeEnum.TIMESTAMP);
            put(java.sql.Timestamp.class, PgsqlDefaultTypeEnum.TIMESTAMP);
            put(LocalDateTime.class, PgsqlDefaultTypeEnum.TIMESTAMP);
            put(LocalDate.class, PgsqlDefaultTypeEnum.DATE);
            put(LocalTime.class, PgsqlDefaultTypeEnum.TIME);
            put(java.sql.Time.class, PgsqlDefaultTypeEnum.TIME);
            put(OffsetTime.class, PgsqlDefaultTypeEnum.TIMETZ);
        }};
    }

    @Override
    public String dropTable(String schema, String tableName) {
        return String.format("DROP TABLE IF EXISTS %s", withSchemaName(schema, tableName));
    }

    @Override
    public @NonNull DefaultTableMetadata analyseClass(Class<?> beanClass) {

        return new PgsqlTableMetadataBuilder().build(beanClass);
    }

    @Override
    public List<String> createTable(DefaultTableMetadata tableMetadata) {
        String sql = CreateTableSqlBuilder.buildSql(tableMetadata);
        return Collections.singletonList(sql);
    }

    @Override
    public @NonNull PgsqlCompareTableInfo compareTable(DefaultTableMetadata tableMetadata) {

        String tableName = tableMetadata.getTableName();
        String schema = tableMetadata.getSchema();

        PgsqlCompareTableInfo pgsqlCompareTableInfo = new PgsqlCompareTableInfo(tableName, schema);

        // 比较表信息
        compareTableInfo(tableMetadata, pgsqlCompareTableInfo);

        // 比较字段信息
        compareColumnInfo(tableMetadata, pgsqlCompareTableInfo);

        // 比较索引信息
        compareIndexInfo(tableMetadata, pgsqlCompareTableInfo);

        return pgsqlCompareTableInfo;
    }

    @Override
    public boolean checkTableNotExist(String schema, String tableName) {
        // 获取Configuration对象
        Configuration configuration = SqlSessionFactoryManager.getSqlSessionFactory().getConfiguration();
        try (Connection connection = configuration.getEnvironment().getDataSource().getConnection()) {
            if (!StringUtils.hasText(schema)) {
                schema = connection.getSchema();
            }
            boolean exist = Utils.tableIsExists(connection, schema, tableName, new String[]{"TABLE", "PARTITIONED TABLE"}, true);
            return !exist;
        } catch (SQLException e) {
            throw new RuntimeException("判断数据库是否存在出错", e);
        }
    }

    private void compareIndexInfo(DefaultTableMetadata tableMetadata, PgsqlCompareTableInfo pgsqlCompareTableInfo) {

        String tableName = tableMetadata.getTableName();
        String schema = tableMetadata.getSchema();

        List<PgsqlDbIndex> pgsqlDbIndices = executeReturn(pgsqlTablesMapper -> pgsqlTablesMapper.selectTableIndexesDetail(schema, tableName));
        Map<String, PgsqlDbIndex> pgsqlDbIndexMap = pgsqlDbIndices.stream()
                .collect(Collectors.toMap(PgsqlDbIndex::getIndexName, Function.identity()));

        List<IndexMetadata> indexMetadataList = tableMetadata.getIndexMetadataList();
        for (IndexMetadata indexMetadata : indexMetadataList) {
            String indexName = indexMetadata.getName();
            String comment = indexMetadata.getComment();
            // 尝试从索引标记集合中删除索引
            PgsqlDbIndex dbIndex = pgsqlDbIndexMap.remove(indexName);
            // 删除失败，表示是新增的索引
            boolean isNewIndex = dbIndex == null;
            if (isNewIndex) {
                // 标记注释
                if (StringUtils.hasText(comment)) {
                    pgsqlCompareTableInfo.addIndexComment(indexMetadata.getName(), comment);
                }
                // 标记索引信息
                pgsqlCompareTableInfo.addNewIndex(indexMetadata);
                continue;
            }
            // 修改索引注释
            boolean anyOneIsValid = StringUtils.hasText(dbIndex.getDescription()) || StringUtils.hasText(comment);
            if (anyOneIsValid && !Objects.equals(dbIndex.getDescription(), comment)) {
                pgsqlCompareTableInfo.addIndexComment(indexName, comment);
            }

            // 获取索引定义语句，进行比较  CREATE UNIQUE INDEX mpe_idx_phone_index ON "public".my_pgsql_table USING btree (phone DESC)
            String dbIndexdef = dbIndex.getIndexdef().replace("\"", "") + ";";
            String newIndexdef = CreateTableSqlBuilder.getCreateIndexSql(schema, tableName, indexMetadata).replace("\"", "");
            if (!newIndexdef.equals(dbIndexdef)) {
                pgsqlCompareTableInfo.addModifyIndex(indexMetadata);
            }
        }

        // 需要删除的索引
        Set<String> needDropIndexes = pgsqlDbIndexMap.keySet();
        if (!needDropIndexes.isEmpty()) {
            PropertyConfig autoTableProperties = AutoTableGlobalConfig.getAutoTableProperties();
            // 删除autotable创建的索引
            if (autoTableProperties.getAutoDropIndex()) {
                List<String> autoTableCreateIndexes = needDropIndexes.stream().filter(indexName -> indexName.startsWith(autoTableProperties.getIndexPrefix())).collect(Collectors.toList());
                pgsqlCompareTableInfo.getDropIndexList().addAll(autoTableCreateIndexes);
            }
            // 删除手动创建的索引
            if (autoTableProperties.getAutoDropCustomIndex()) {
                List<String> customCreateIndexes = needDropIndexes.stream().filter(indexName -> !indexName.startsWith(autoTableProperties.getIndexPrefix())).collect(Collectors.toList());
                pgsqlCompareTableInfo.getDropIndexList().addAll(customCreateIndexes);
            }
        }
    }

    private void compareColumnInfo(DefaultTableMetadata tableMetadata, PgsqlCompareTableInfo pgsqlCompareTableInfo) {

        String tableName = tableMetadata.getTableName();
        String schema = tableMetadata.getSchema();
        // 数据库字段元信息
        List<PgsqlDbColumn> pgsqlDbColumns = executeReturn(pgsqlTablesMapper -> pgsqlTablesMapper.selectTableFieldDetail(schema, tableName));
        Map<String, PgsqlDbColumn> pgsqlFieldDetailMap = pgsqlDbColumns.stream().collect(Collectors.toMap(PgsqlDbColumn::getColumnName, Function.identity()));
        // 当前字段信息
        List<ColumnMetadata> columnMetadataList = tableMetadata.getColumnMetadataList();

        for (ColumnMetadata columnMetadata : columnMetadataList) {
            String columnName = columnMetadata.getName();
            PgsqlDbColumn pgsqlDbColumn = pgsqlFieldDetailMap.remove(columnName);
            // 新增字段
            String fieldComment = columnMetadata.getComment();
            if (pgsqlDbColumn == null) {
                // 标记注释
                pgsqlCompareTableInfo.addColumnComment(columnMetadata.getName(), fieldComment);
                // 标记字段信息
                pgsqlCompareTableInfo.addNewColumn(columnMetadata);
                continue;
            }
            /* 修改的字段 */
            // 修改了字段注释
            String dbColumnComment = pgsqlDbColumn.getDescription();
            if ((StringUtils.hasText(dbColumnComment) || StringUtils.hasText(fieldComment)) && !Objects.equals(dbColumnComment, fieldComment)) {
                pgsqlCompareTableInfo.addColumnComment(columnName, fieldComment);
            }
            // 主键忽略判断，单独处理
            if (!columnMetadata.isPrimary()) {
                // 字段类型不同
                boolean isTypeDiff = isTypeDiff(columnMetadata, pgsqlDbColumn);
                // 非null不同
                boolean isNotnullDiff = columnMetadata.isNotNull() != Objects.equals(pgsqlDbColumn.getIsNullable(), "NO");
                // 默认值不同
                boolean isDefaultDiff = isDefaultDiff(columnMetadata, pgsqlDbColumn);
                if (isTypeDiff || isNotnullDiff || isDefaultDiff) {
                    pgsqlCompareTableInfo.addModifyColumn(columnMetadata);
                }
            }
        }
        // 需要删除的字段
        Set<String> needRemoveColumns = pgsqlFieldDetailMap.keySet();
        if (!needRemoveColumns.isEmpty()) {
            // 根据配置，决定是否删除库上的多余字段
            if (AutoTableGlobalConfig.getAutoTableProperties().getAutoDropColumn()) {
                pgsqlCompareTableInfo.addDropColumns(needRemoveColumns);
            }
        }

        /* 处理主键 */
        // 获取所有主键
        List<ColumnMetadata> primaryColumnList = columnMetadataList.stream().filter(ColumnMetadata::isPrimary).collect(Collectors.toList());
        Set<String> newPrimaryColumns = primaryColumnList.stream().map(ColumnMetadata::getName).collect(Collectors.toSet());
        // 查询数据库主键信息
        PgsqlDbPrimary pgsqlDbPrimary = executeReturn(pgsqlTablesMapper -> pgsqlTablesMapper.selectPrimaryKeyName(schema, tableName));
        HashSet<String> dbPrimaryColumns = pgsqlDbPrimary == null ? new HashSet<>() : new HashSet<>(Arrays.asList(pgsqlDbPrimary.getColumns().split(",")));

        boolean primaryChange = !dbPrimaryColumns.equals(newPrimaryColumns);
        if (primaryChange) {
            // 标记待删除的主键
            pgsqlCompareTableInfo.setDropPrimaryKeyName(pgsqlDbPrimary.getPrimaryName());
        }
        boolean newPrimary = !primaryColumnList.isEmpty() && pgsqlDbPrimary == null;
        if (newPrimary || primaryChange) {
            // 标记新创建的主键
            pgsqlCompareTableInfo.addNewPrimary(primaryColumnList);
        }
    }

    private boolean isTypeDiff(ColumnMetadata columnMetadata, PgsqlDbColumn pgsqlDbColumn) {
        String dataTypeFormat = pgsqlDbColumn.getDataTypeFormat();
        String fullType = columnMetadata.getType().getDefaultFullType().toLowerCase();
        // 数字类型的，默认没有长度，但是数据库查询出来的有长度。 "int4(32)".startWith("int4")
        if (dataTypeFormat.startsWith("int")) {
            return !dataTypeFormat.startsWith(fullType);
        }
        return !Objects.equals(fullType, dataTypeFormat);
    }

    private boolean isDefaultDiff(ColumnMetadata columnMetadata, PgsqlDbColumn pgsqlDbColumn) {

        String columnDefault = pgsqlDbColumn.getColumnDefault();
        // 纠正default值，去掉类型转换
        if (columnDefault != null) {
            int castChart = columnDefault.indexOf("::");
            if (castChart > 0) {
                columnDefault = columnDefault.substring(0, castChart);
            }
        }

        DefaultValueEnum defaultValueType = columnMetadata.getDefaultValueType();

        if (DefaultValueEnum.isValid(defaultValueType)) {
            if (defaultValueType == DefaultValueEnum.EMPTY_STRING) {
                return !"''".equals(columnDefault);
            }
            if (defaultValueType == DefaultValueEnum.NULL) {
                return columnDefault != null && !"NULL".equalsIgnoreCase(columnDefault);
            }
        } else {
            String defaultValue = columnMetadata.getDefaultValue();
            return !Objects.equals(defaultValue, columnDefault);
        }
        return false;
    }

    private void compareTableInfo(DefaultTableMetadata tableMetadata, PgsqlCompareTableInfo pgsqlCompareTableInfo) {

        String tableName = tableMetadata.getTableName();
        String schema = tableMetadata.getSchema();

        String tableDescription = executeReturn(pgsqlTablesMapper -> pgsqlTablesMapper.selectTableDescription(schema, tableName));
        if (!Objects.equals(tableDescription, tableMetadata.getComment())) {
            pgsqlCompareTableInfo.setComment(tableMetadata.getComment());
        }
    }

    @Override
    public List<String> modifyTable(PgsqlCompareTableInfo pgsqlCompareTableInfo) {
        String sql = ModifyTableSqlBuilder.buildSql(pgsqlCompareTableInfo);
        return Collections.singletonList(sql);
    }

    public static String withSchemaName(String schema, String... names) {

        String name = String.join(".", names);

        if (StringUtils.hasText(schema)) {
            return schema + "." + name;
        }

        return name;
    }
}
