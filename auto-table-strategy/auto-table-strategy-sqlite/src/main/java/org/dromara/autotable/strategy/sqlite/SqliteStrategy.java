package org.dromara.autotable.strategy.sqlite;

import lombok.NonNull;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.strategy.sqlite.builder.CreateTableSqlBuilder;
import org.dromara.autotable.strategy.sqlite.builder.SqliteTableMetadataBuilder;
import org.dromara.autotable.strategy.sqlite.data.SqliteCompareTableInfo;
import org.dromara.autotable.strategy.sqlite.data.SqliteDefaultTypeEnum;
import org.dromara.autotable.strategy.sqlite.data.dbdata.SqliteColumns;
import org.dromara.autotable.strategy.sqlite.data.dbdata.SqliteMaster;
import org.dromara.autotable.strategy.sqlite.mapper.SqliteTablesMapper;
import org.dromara.autotable.core.utils.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author don
 */
public class SqliteStrategy implements IStrategy<DefaultTableMetadata, SqliteCompareTableInfo> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SqliteTablesMapper mapper = new SqliteTablesMapper();

    @Override
    public String databaseDialect() {
        return DatabaseDialect.SQLite;
    }

    private static final Map<Class<?>, DefaultTypeEnumInterface> TYPE_MAPPING = new HashMap<>(32);

    static {
        TYPE_MAPPING.put(String.class, SqliteDefaultTypeEnum.TEXT);
        TYPE_MAPPING.put(Character.class, SqliteDefaultTypeEnum.TEXT);
        TYPE_MAPPING.put(char.class, SqliteDefaultTypeEnum.TEXT);

        TYPE_MAPPING.put(BigInteger.class, SqliteDefaultTypeEnum.INTEGER);
        TYPE_MAPPING.put(Long.class, SqliteDefaultTypeEnum.INTEGER);
        TYPE_MAPPING.put(long.class, SqliteDefaultTypeEnum.INTEGER);

        TYPE_MAPPING.put(Integer.class, SqliteDefaultTypeEnum.INTEGER);
        TYPE_MAPPING.put(int.class, SqliteDefaultTypeEnum.INTEGER);

        TYPE_MAPPING.put(Short.class, SqliteDefaultTypeEnum.INTEGER);
        TYPE_MAPPING.put(short.class, SqliteDefaultTypeEnum.INTEGER);

        TYPE_MAPPING.put(Byte.class, SqliteDefaultTypeEnum.INTEGER);
        TYPE_MAPPING.put(byte.class, SqliteDefaultTypeEnum.INTEGER);

        TYPE_MAPPING.put(Boolean.class, SqliteDefaultTypeEnum.INTEGER);
        TYPE_MAPPING.put(boolean.class, SqliteDefaultTypeEnum.INTEGER);

        TYPE_MAPPING.put(Float.class, SqliteDefaultTypeEnum.REAL);
        TYPE_MAPPING.put(float.class, SqliteDefaultTypeEnum.REAL);
        TYPE_MAPPING.put(Double.class, SqliteDefaultTypeEnum.REAL);
        TYPE_MAPPING.put(double.class, SqliteDefaultTypeEnum.REAL);
        TYPE_MAPPING.put(BigDecimal.class, SqliteDefaultTypeEnum.REAL);

        TYPE_MAPPING.put(Date.class, SqliteDefaultTypeEnum.TEXT);
        TYPE_MAPPING.put(java.sql.Date.class, SqliteDefaultTypeEnum.TEXT);
        TYPE_MAPPING.put(java.sql.Timestamp.class, SqliteDefaultTypeEnum.TEXT);
        TYPE_MAPPING.put(java.sql.Time.class, SqliteDefaultTypeEnum.TEXT);
        TYPE_MAPPING.put(LocalDateTime.class, SqliteDefaultTypeEnum.TEXT);
        TYPE_MAPPING.put(LocalDate.class, SqliteDefaultTypeEnum.TEXT);
        TYPE_MAPPING.put(LocalTime.class, SqliteDefaultTypeEnum.TEXT);
    }

    @Override
    public Map<Class<?>, DefaultTypeEnumInterface> typeMapping() {
        return Collections.unmodifiableMap(TYPE_MAPPING);
    }

    @Override
    public String dropTable(String schema, String tableName) {
        return "DROP TABLE IF EXISTS " + IStrategy.wrapIdentifiers(tableName) + ";";
    }

    @Override
    public @NonNull DefaultTableMetadata analyseClass(Class<?> beanClass) {

        return new SqliteTableMetadataBuilder().build(beanClass);
    }

    @Override
    public List<String> createTable(DefaultTableMetadata tableMetadata) {

        List<String> sqlList = new ArrayList<>();
        String createTableSql = CreateTableSqlBuilder.buildTableSql(tableMetadata.getTableName(), tableMetadata.getComment(), tableMetadata.getColumnMetadataList());
        sqlList.add(createTableSql);
        List<String> createIndexSqlList = CreateTableSqlBuilder.buildIndexSql(tableMetadata.getTableName(), tableMetadata.getIndexMetadataList());
        sqlList.addAll(createIndexSqlList);
        return sqlList;
    }

    @Override
    public @NonNull SqliteCompareTableInfo compareTable(DefaultTableMetadata tableMetadata) {

        String tableName = tableMetadata.getTableName();
        String schema = tableMetadata.getSchema();
        SqliteCompareTableInfo sqliteCompareTableInfo = new SqliteCompareTableInfo(tableName, schema);

        // 获取配置
        PropertyConfig properties = AutoTableGlobalConfig.instance().getAutoTableProperties();
        String logicDropColumnPrefix = properties.getLogicDropColumnPrefix();

        // 判断表是否需要重建
        String orgBuildTableSql = mapper.queryBuildTableSql(tableName);
        List<ColumnMetadata> columnMetadataList = tableMetadata.getColumnMetadataList();
        String newBuildTableSql = CreateTableSqlBuilder.buildTableSql(tableMetadata.getTableName(), tableMetadata.getComment(), columnMetadataList);

        // 查询现有表列
        List<SqliteColumns> oldColumns = mapper.queryTableColumns(tableName);
        Set<String> oldColumnNames = oldColumns.stream().map(SqliteColumns::getName).collect(Collectors.toSet());
        Set<String> newColumnNames = columnMetadataList.stream().map(ColumnMetadata::getName).collect(Collectors.toSet());

        // 处理多余字段（逻辑删除）
        Set<String> extraColumnNames = new HashSet<>(oldColumnNames);
        extraColumnNames.removeAll(newColumnNames);

        boolean hasLogicDropColumns = false;
        for (String extraColumn : extraColumnNames) {
            // 已带前缀的字段跳过不处理
            if (StringUtils.hasText(logicDropColumnPrefix) && extraColumn.startsWith(logicDropColumnPrefix)) {
                continue;
            }
            // 逻辑删除：重命名字段
            if (StringUtils.hasText(logicDropColumnPrefix)) {
                String newColumnName = logicDropColumnPrefix + extraColumn;
                sqliteCompareTableInfo.getRenameColumnMap().put(extraColumn, newColumnName);
                hasLogicDropColumns = true;
            }
        }

        boolean needRebuildTable = !Objects.equals(orgBuildTableSql + ";", newBuildTableSql);
        // 如果配置了逻辑删除，且所有额外列都已标记为逻辑删除，则不需要重建表
        if (needRebuildTable && hasLogicDropColumns) {
            Set<String> effectiveOldColumns = new HashSet<>(oldColumnNames);
            for (Map.Entry<String, String> entry : sqliteCompareTableInfo.getRenameColumnMap().entrySet()) {
                effectiveOldColumns.remove(entry.getKey());
            }
            // 移除已带前缀的列
            if (StringUtils.hasText(logicDropColumnPrefix)) {
                effectiveOldColumns.removeIf(col -> col.startsWith(logicDropColumnPrefix));
            }
            if (effectiveOldColumns.equals(newColumnNames)) {
                needRebuildTable = false;
            }
        }

        if (needRebuildTable) {

            // 筛选出数据迁移的列
            List<String> validColumnNames = newColumnNames.stream().filter(oldColumnNames::contains).collect(Collectors.toList());
            sqliteCompareTableInfo.setDataMigrationColumnList(validColumnNames);

            // 该情况下无需单独分析索引了，因为sqlite的表修改方式为重建整个表，索引需要全部删除，重新创建
            sqliteCompareTableInfo.setRebuildTableSql(newBuildTableSql);
            // 删除当前所有索引
            List<SqliteMaster> orgBuildIndexSqlList = mapper.queryBuildIndexSql(tableName);
            for (SqliteMaster sqliteMaster : orgBuildIndexSqlList) {
                sqliteCompareTableInfo.getDeleteIndexList().add(sqliteMaster.getName());
            }
            // 添加新建索引的sql
            List<String> buildIndexSqlList = CreateTableSqlBuilder.buildIndexSql(tableName, tableMetadata.getIndexMetadataList());
            for (String buildIndexSql : buildIndexSqlList) {
                sqliteCompareTableInfo.getBuildIndexSqlList().add(buildIndexSql);
            }
        } else {
            // 不需要重建表的情况下，才有必要单独判断索引的更新情况
            // 判断索引是否需要重建 <索引name，索引sql>
            Map<String, String> rebuildIndexMap = tableMetadata.getIndexMetadataList().stream()
                    .collect(Collectors.toMap(
                            IndexMetadata::getName,
                            indexMetadata -> CreateTableSqlBuilder.getIndexSql(tableName, indexMetadata)
                    ));
            // 遍历所有数据库存在的索引，判断有没有变化
            List<SqliteMaster> orgBuildIndexSqlList = mapper.queryBuildIndexSql(tableName);
            PropertyConfig autoTableProperties = AutoTableGlobalConfig.instance().getAutoTableProperties();
            for (SqliteMaster sqliteMaster : orgBuildIndexSqlList) {
                String indexName = sqliteMaster.getName();
                String newBuildIndexSql = rebuildIndexMap.remove(indexName);
                boolean exit = newBuildIndexSql != null;
                // 如果最新构建标记上没有该注解的标记了，则说明该注解需要删除了
                if (!exit) {
                    // 根据配置决定是否删除多余的索引
                    if (autoTableProperties.getAutoDropIndex() && indexName.startsWith(autoTableProperties.getIndexPrefix())) {
                        sqliteCompareTableInfo.getDeleteIndexList().add(indexName);
                    } else if (autoTableProperties.getAutoDropCustomIndex() && !indexName.startsWith(autoTableProperties.getIndexPrefix())) {
                        sqliteCompareTableInfo.getDeleteIndexList().add(indexName);
                    }
                }
                // 新的索引构建语句中存在相同名称的索引，且内容不一致，需要重新构建
                String createIndexSqlRecord = sqliteMaster.getSql() + ";";
                if (exit && !Objects.equals(newBuildIndexSql, createIndexSqlRecord)) {
                    sqliteCompareTableInfo.getDeleteIndexList().add(indexName);
                    sqliteCompareTableInfo.getBuildIndexSqlList().add(newBuildIndexSql);
                }
            }
            // 筛选完，剩下的，是需要新增的索引
            Map<String, String> needNewIndexes = rebuildIndexMap;
            if (!needNewIndexes.isEmpty()) {
                sqliteCompareTableInfo.getBuildIndexSqlList().addAll(needNewIndexes.values());
            }
        }

        return sqliteCompareTableInfo;
    }

    @Override
    public List<String> modifyTable(SqliteCompareTableInfo sqliteCompareTableInfo) {

        List<String> sqlList = new ArrayList<>();

        // 删除索引
        List<String> deleteIndexList = sqliteCompareTableInfo.getDeleteIndexList();
        if (!deleteIndexList.isEmpty()) {
            for (String deleteIndexName : deleteIndexList) {
                sqlList.add("DROP INDEX IF EXISTS " + IStrategy.wrapIdentifiers(deleteIndexName) + ";");
            }
        }

        // 重命名列（逻辑删除）
        Map<String, String> renameColumnMap = sqliteCompareTableInfo.getRenameColumnMap();
        if (!renameColumnMap.isEmpty()) {
            String tableName = sqliteCompareTableInfo.getName();
            for (Map.Entry<String, String> entry : renameColumnMap.entrySet()) {
                String oldName = entry.getKey();
                String newName = entry.getValue();
                sqlList.add("ALTER TABLE " + IStrategy.wrapIdentifiers(tableName)
                        + " RENAME COLUMN " + IStrategy.wrapIdentifiers(oldName)
                        + " TO " + IStrategy.wrapIdentifiers(newName) + ";");
            }
        }

        // 重建表
        String rebuildTableSql = sqliteCompareTableInfo.getRebuildTableSql();
        if (StringUtils.hasText(rebuildTableSql)) {
            String orgTableName = sqliteCompareTableInfo.getName();
            String backupTableName = getBackupTableName(orgTableName);
            String wrapOrgTableName = IStrategy.wrapIdentifiers(orgTableName);
            String wrapBackupTableName = IStrategy.wrapIdentifiers(backupTableName);
            // 备份表
            sqlList.add("ALTER TABLE " + wrapOrgTableName + " RENAME TO " + wrapBackupTableName + ";");
            // 重新建表
            sqlList.add(rebuildTableSql);
            List<String> dataMigrationColumnList = sqliteCompareTableInfo.getDataMigrationColumnList();
            if (!dataMigrationColumnList.isEmpty()) {
                // 迁移数据
                String columns = IStrategy.customConcatWrapIdentifiers(",", dataMigrationColumnList);
                sqlList.add("INSERT INTO " + wrapOrgTableName + " (" + columns + ") SELECT " + columns + " FROM " + wrapBackupTableName + ";");
            }
        }

        // 创建索引
        List<String> buildIndexSqlList = sqliteCompareTableInfo.getBuildIndexSqlList();
        sqlList.addAll(buildIndexSqlList);

        return sqlList;
    }

    private String getBackupTableName(String orgTableName) {

        int offset = 0;
        String name = "_{orgTableName}_old_{datetime}"
                .replace("{orgTableName}", orgTableName)
                .replace("{datetime}", LocalDateTime.now().format(DATE_TIME_FORMATTER));
        StringBuilder backupName = new StringBuilder(name);
        while (true) {
            if (offset > 0) {
                backupName.append("_").append(offset);
            }
            String finalBackupName = backupName.toString();
            boolean tableNotExist = this.checkTableNotExist("", finalBackupName);
            if (tableNotExist) {
                return backupName.toString();
            } else {
                offset++;
            }
        }
    }
}
