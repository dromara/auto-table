package org.dromara.autotable.strategy.pgsql.builder;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.strategy.pgsql.PgsqlStrategy;
import org.dromara.autotable.strategy.pgsql.data.PgsqlCompareTableInfo;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author don
 */
@Slf4j
public class ModifyTableSqlBuilder {

    /**
     * 构建创建新表的SQL
     *
     * @param pgsqlCompareTableInfo 参数
     * @return sql
     */
    public static String buildSql(PgsqlCompareTableInfo pgsqlCompareTableInfo) {

        String tableName = pgsqlCompareTableInfo.getName();
        String schema = pgsqlCompareTableInfo.getSchema();

        String tableComment = pgsqlCompareTableInfo.getComment();
        Map<String, String> columnComment = pgsqlCompareTableInfo.getColumnComment();
        Map<String, String> indexComment = pgsqlCompareTableInfo.getIndexComment();

        /* 修改字段 */
        List<String> alterTableSqlList = new ArrayList<>();
        // 删除主键
        String primaryKeyName = pgsqlCompareTableInfo.getDropPrimaryKeyName();
        if (StringUtils.hasText(primaryKeyName)) {
            alterTableSqlList.add(String.format("  DROP CONSTRAINT \"%s\"", primaryKeyName));
        }
        // 删除列
        List<String> dropColumnList = pgsqlCompareTableInfo.getDropColumnList();
        dropColumnList.stream()
                .map(columnName -> String.format("  DROP COLUMN \"%s\"", columnName))
                .forEach(alterTableSqlList::add);
        // 新增列
        List<ColumnMetadata> newColumnList = pgsqlCompareTableInfo.getNewColumnMetadataList();
        newColumnList.stream()
                .map(column -> String.format("  ADD COLUMN %s", ColumnSqlBuilder.buildSql(column)))
                .forEach(alterTableSqlList::add);
        // 修改列
        List<ColumnMetadata> modifyColumnList = pgsqlCompareTableInfo.getModifyColumnMetadataList();
        for (ColumnMetadata columnMetadata : modifyColumnList) {
            // 修改字段
            String columnName = columnMetadata.getName();
            // 类型
            String newFullType = columnMetadata.getType().getDefaultFullType();
            alterTableSqlList.add(String.format("  ALTER COLUMN \"%s\" TYPE %s USING \"%s\"::%s", columnName, newFullType, columnName, newFullType));
            // 非空
            alterTableSqlList.add(String.format("  ALTER COLUMN \"%s\" %s NOT NULL", columnName, columnMetadata.isNotNull() ? "SET" : "DROP"));
            // 默认值
            String defaultVal = null;
            DefaultValueEnum defaultValueType = columnMetadata.getDefaultValueType();
            if (DefaultValueEnum.EMPTY_STRING == defaultValueType) {
                defaultVal = "''";
            } else if (DefaultValueEnum.NULL == defaultValueType) {
                defaultVal = "NULL";
            } else {
                String defaultValue = columnMetadata.getDefaultValue();
                if (StringUtils.hasText(defaultValue)) {
                    defaultVal = defaultValue;
                }
            }
            if (StringUtils.hasText(defaultVal)) {
                // 设置默认值
                alterTableSqlList.add(String.format("  ALTER COLUMN \"%s\" SET DEFAULT %s", columnName, defaultVal));
            } else {
                // 删除默认值
                alterTableSqlList.add(String.format("  ALTER COLUMN \"%s\" DROP DEFAULT", columnName));
            }
        }
        // 添加主键
        List<ColumnMetadata> newPrimaries = pgsqlCompareTableInfo.getNewPrimaries();
        if (!newPrimaries.isEmpty()) {
            String primaryColumns = newPrimaries.stream().map(ColumnMetadata::getName).collect(Collectors.joining("\", \""));
            if (StringUtils.hasText(primaryKeyName)) {
                // 修改主键
                alterTableSqlList.add(String.format("  ADD CONSTRAINT \"%s\" PRIMARY KEY (\"%s\")", primaryKeyName, primaryColumns));
            } else {
                // 新增主键
                alterTableSqlList.add(String.format("  ADD PRIMARY KEY (\"%s\")", primaryColumns));
            }
        }
        // 组合sql
        String alterTableSql = "";
        if (!alterTableSqlList.isEmpty()) {
            alterTableSql = String.format("ALTER TABLE %s \n%s;", PgsqlStrategy.withSchemaName(schema, tableName), String.join(",\n", alterTableSqlList));
        }

        /* 为 表、字段、索引 添加注释 */
        String addColumnCommentSql = CreateTableSqlBuilder.getAddColumnCommentSql(schema, tableName, tableComment, columnComment, indexComment);

        /* 修改 索引 */
        // 删除索引
        List<String> dropIndexList = pgsqlCompareTableInfo.getDropIndexList();
        String dropIndexSql = dropIndexList.stream().map(indexName -> String.format("DROP INDEX %s;", PgsqlStrategy.withSchemaName(schema, indexName))).collect(Collectors.joining("\n"));
        // 添加索引
        String createIndexSql = CreateTableSqlBuilder.getCreateIndexSql(schema, tableName, pgsqlCompareTableInfo.getIndexMetadataList());

        return Stream.of(dropIndexSql, alterTableSql, createIndexSql, addColumnCommentSql)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
    }
}
