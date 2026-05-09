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

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SqliteTablesMapper mapper = new SqliteTablesMapper();

    @Override
    public String databaseDialect() {
        return DatabaseDialect.SQLite;
    }

    @Override
    public Map<Class<?>, DefaultTypeEnumInterface> typeMapping() {
        return new HashMap<Class<?>, DefaultTypeEnumInterface>(32) {{
            put(String.class, SqliteDefaultTypeEnum.TEXT);
            put(Character.class, SqliteDefaultTypeEnum.TEXT);
            put(char.class, SqliteDefaultTypeEnum.TEXT);

            put(BigInteger.class, SqliteDefaultTypeEnum.INTEGER);
            put(Long.class, SqliteDefaultTypeEnum.INTEGER);
            put(long.class, SqliteDefaultTypeEnum.INTEGER);

            put(Integer.class, SqliteDefaultTypeEnum.INTEGER);
            put(int.class, SqliteDefaultTypeEnum.INTEGER);

            put(Boolean.class, SqliteDefaultTypeEnum.INTEGER);
            put(boolean.class, SqliteDefaultTypeEnum.INTEGER);

            put(Float.class, SqliteDefaultTypeEnum.REAL);
            put(float.class, SqliteDefaultTypeEnum.REAL);
            put(Double.class, SqliteDefaultTypeEnum.REAL);
            put(double.class, SqliteDefaultTypeEnum.REAL);
            put(BigDecimal.class, SqliteDefaultTypeEnum.REAL);

            put(Date.class, SqliteDefaultTypeEnum.TEXT);
            put(java.sql.Date.class, SqliteDefaultTypeEnum.TEXT);
            put(java.sql.Timestamp.class, SqliteDefaultTypeEnum.TEXT);
            put(java.sql.Time.class, SqliteDefaultTypeEnum.TEXT);
            put(LocalDateTime.class, SqliteDefaultTypeEnum.TEXT);
            put(LocalDate.class, SqliteDefaultTypeEnum.TEXT);
            put(LocalTime.class, SqliteDefaultTypeEnum.TEXT);

            put(Short.class, SqliteDefaultTypeEnum.INTEGER);
            put(short.class, SqliteDefaultTypeEnum.INTEGER);
        }};
    }

    @Override
    public String dropTable(String schema, String tableName) {
        return String.format("drop table if exists %s;", tableName);
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
            for (SqliteMaster sqliteMaster : orgBuildIndexSqlList) {
                String indexName = sqliteMaster.getName();
                String newBuildIndexSql = rebuildIndexMap.remove(indexName);
                boolean exit = newBuildIndexSql != null;
                // 如果最新构建标记上没有该注解的标记了，则说明该注解需要删除了
                if (!exit) {
                    sqliteCompareTableInfo.getDeleteIndexList().add(indexName);
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
                sqlList.add(String.format("drop index if exists %s;", deleteIndexName));
            }
        }

        // 重命名列（逻辑删除）
        Map<String, String> renameColumnMap = sqliteCompareTableInfo.getRenameColumnMap();
        if (!renameColumnMap.isEmpty()) {
            String tableName = sqliteCompareTableInfo.getName();
            for (Map.Entry<String, String> entry : renameColumnMap.entrySet()) {
                String oldName = entry.getKey();
                String newName = entry.getValue();
                sqlList.add(String.format("ALTER TABLE %s RENAME COLUMN %s TO %s;", tableName, oldName, newName));
            }
        }

        // 重建表
        String rebuildTableSql = sqliteCompareTableInfo.getRebuildTableSql();
        if (StringUtils.hasText(rebuildTableSql)) {
            String orgTableName = sqliteCompareTableInfo.getName();
            String backupTableName = getBackupTableName(orgTableName);
            // 备份表
            sqlList.add(String.format("ALTER TABLE %s RENAME TO %s;", orgTableName, backupTableName));
            // 重新建表
            sqlList.add(rebuildTableSql);
            List<String> dataMigrationColumnList = sqliteCompareTableInfo.getDataMigrationColumnList();
            if(!dataMigrationColumnList.isEmpty()) {
                // 迁移数据
                String columns = String.join(",", dataMigrationColumnList);
                sqlList.add(String.format("INSERT INTO %s (%s) SELECT %s FROM %s;", orgTableName, columns, columns, backupTableName));
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
                .replace("{datetime}", LocalDateTime.now().format(dateTimeFormatter));
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
