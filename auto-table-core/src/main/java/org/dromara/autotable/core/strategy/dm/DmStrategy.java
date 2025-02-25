package org.dromara.autotable.core.strategy.dm;

import lombok.NonNull;
import org.apache.ibatis.session.Configuration;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.Utils;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
import org.dromara.autotable.core.dynamicds.SqlSessionFactoryManager;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.strategy.dm.builder.DamengCreateTableSqlBuilder;
import org.dromara.autotable.core.strategy.dm.builder.DamengModifyTableSqlBuilder;
import org.dromara.autotable.core.strategy.dm.builder.DamengTableMetadataBuilder;
import org.dromara.autotable.core.strategy.dm.data.DamengCompareTableInfo;
import org.dromara.autotable.core.strategy.dm.data.DamengDefaultTypeEnum;
import org.dromara.autotable.core.strategy.dm.data.dbdata.DamengDbColumn;
import org.dromara.autotable.core.strategy.dm.data.dbdata.DamengDbIndex;
import org.dromara.autotable.core.strategy.dm.data.dbdata.DamengDbPrimary;
import org.dromara.autotable.core.strategy.dm.mapper.DamengTablesMapper;
import org.dromara.autotable.core.utils.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
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
public class DmStrategy implements IStrategy<DefaultTableMetadata, DamengCompareTableInfo, DamengTablesMapper> {

    public static String withSchemaName(String schema, String... names) {
        String name = String.join(".", names);
        return StringUtils.hasText(schema) ? schema + "." + name : name;
    }

    @Override
    public String databaseDialect() {
        return DatabaseDialect.DM;
    }

    @Override
    public Map<Class<?>, DefaultTypeEnumInterface> typeMapping() {
        return new HashMap<Class<?>, DefaultTypeEnumInterface>(32) {{
            put(String.class, DamengDefaultTypeEnum.VARCHAR2);
            put(Character.class, DamengDefaultTypeEnum.CHAR);
            put(char.class, DamengDefaultTypeEnum.CHAR);

            put(Short.class, DamengDefaultTypeEnum.SMALLINT);
            put(short.class, DamengDefaultTypeEnum.SMALLINT);
            put(int.class, DamengDefaultTypeEnum.INTEGER);
            put(Integer.class, DamengDefaultTypeEnum.INTEGER);
            put(Long.class, DamengDefaultTypeEnum.BIGINT);
            put(long.class, DamengDefaultTypeEnum.BIGINT);
            put(BigInteger.class, DamengDefaultTypeEnum.BIGINT);

            put(Boolean.class, DamengDefaultTypeEnum.BOOLEAN);
            put(boolean.class, DamengDefaultTypeEnum.BOOLEAN);

            put(Float.class, DamengDefaultTypeEnum.FLOAT);
            put(float.class, DamengDefaultTypeEnum.FLOAT);
            put(Double.class, DamengDefaultTypeEnum.DOUBLE);
            put(double.class, DamengDefaultTypeEnum.DOUBLE);
            put(BigDecimal.class, DamengDefaultTypeEnum.DECIMAL);

            put(Date.class, DamengDefaultTypeEnum.TIMESTAMP);
            put(java.sql.Date.class, DamengDefaultTypeEnum.DATE);
            put(java.sql.Timestamp.class, DamengDefaultTypeEnum.TIMESTAMP);
            put(LocalDateTime.class, DamengDefaultTypeEnum.TIMESTAMP);
            put(LocalDate.class, DamengDefaultTypeEnum.DATE);
            put(LocalTime.class, DamengDefaultTypeEnum.TIME);
            put(java.sql.Time.class, DamengDefaultTypeEnum.TIME);
        }};
    }

    @Override
    public String dropTable(String schema, String tableName) {
        return String.format("DROP TABLE IF EXISTS %s", withSchemaName(schema, tableName));
    }

    @Override
    public @NonNull DefaultTableMetadata analyseClass(Class<?> beanClass) {
        return new DamengTableMetadataBuilder().build(beanClass);
    }

    @Override
    public List<String> createTable(DefaultTableMetadata tableMetadata) {
        String sql = DamengCreateTableSqlBuilder.buildSql(tableMetadata);
        return Collections.singletonList(sql);
    }

    @Override
    public @NonNull DamengCompareTableInfo compareTable(DefaultTableMetadata tableMetadata) {
        String tableName = tableMetadata.getTableName();
        String schema = tableMetadata.getSchema();

        DamengCompareTableInfo compareInfo = new DamengCompareTableInfo(tableName, schema);

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
        Configuration configuration = SqlSessionFactoryManager.getSqlSessionFactory().getConfiguration();
        try (Connection connection = configuration.getEnvironment().getDataSource().getConnection()) {
            if (!StringUtils.hasText(schema)) {
                schema = connection.getMetaData().getUserName();
            }
            return !Utils.tableIsExists(connection, schema, tableName, new String[]{"TABLE"}, true);
        } catch (SQLException e) {
            throw new RuntimeException("检查表存在性失败", e);
        }
    }

    private void compareTableInfo(DefaultTableMetadata metadata, DamengCompareTableInfo compareInfo) {
        String tableComment = executeReturn(mapper ->
                mapper.selectTableComment(metadata.getSchema(), metadata.getTableName()));
        if (!Objects.equals(tableComment, metadata.getComment())) {
            compareInfo.setComment(metadata.getComment());
        }
    }

    private void compareColumnInfo(DefaultTableMetadata metadata, DamengCompareTableInfo compareInfo) {
        String schema = metadata.getSchema();
        String tableName = metadata.getTableName();

        // 获取数据库字段信息
        List<DamengDbColumn> dbColumns = executeReturn(mapper ->
                mapper.selectTableColumns(schema, tableName));
        Map<String, DamengDbColumn> columnMap = dbColumns.stream()
                .collect(Collectors.toMap(DamengDbColumn::getName, Function.identity()));

        // 处理字段差异
        for (ColumnMetadata column : metadata.getColumnMetadataList()) {
            String colName = column.getName();
            DamengDbColumn dbColumn = columnMap.remove(colName);

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
        if (AutoTableGlobalConfig.getAutoTableProperties().getAutoDropColumn()) {
            compareInfo.addDropColumns(columnMap.keySet());
        }

        // 处理主键变更
        handlePrimaryKeyChange(metadata, compareInfo, schema, tableName);
    }

    private boolean isColumnDefinitionChanged(ColumnMetadata newCol, DamengDbColumn oldCol) {
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

    private void handlePrimaryKeyChange(DefaultTableMetadata metadata, DamengCompareTableInfo compareInfo,
                                        String schema, String tableName) {
        // 获取数据库主键信息
        DamengDbPrimary dbPrimary = executeReturn(mapper ->
                mapper.selectPrimaryKey(schema, tableName));
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

    private void compareIndexInfo(DefaultTableMetadata metadata, DamengCompareTableInfo compareInfo) {
        String schema = metadata.getSchema();
        String tableName = metadata.getTableName();

        // 获取数据库索引信息
        List<DamengDbIndex> dbIndexes = executeReturn(mapper ->
                mapper.selectTableIndexes(schema, tableName));
        Map<String, DamengDbIndex> indexMap = dbIndexes.stream()
                .collect(Collectors.toMap(DamengDbIndex::getIndexName, Function.identity()));

        // 处理索引差异
        for (IndexMetadata newIndex : metadata.getIndexMetadataList()) {
            String indexName = newIndex.getName();
            DamengDbIndex dbIndex = indexMap.remove(indexName);

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
        if (AutoTableGlobalConfig.getAutoTableProperties().getAutoDropIndex()) {
            compareInfo.addDropIndexes(indexMap.keySet());
        }
    }

    private boolean isIndexChanged(IndexMetadata newIndex, DamengDbIndex oldIndex) {
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
    public List<String> modifyTable(DamengCompareTableInfo compareInfo) {
        String sql = DamengModifyTableSqlBuilder.buildSql(compareInfo);
        return Collections.singletonList(sql);
    }
}
