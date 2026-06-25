package org.dromara.autotable.strategy.sqlserver.builder;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.strategy.sqlserver.data.SqlServerCompareTableInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 改表 SQL 生成器（SQLServer）。
 *
 * <p>返回 {@code List<String>}，每个元素为一条独立语句（SQLServer JDBC 默认单次 execute 只执行一条语句）。</p>
 *
 * <p>SQLServer 方言要点：</p>
 * <ul>
 *   <li>改类型/非空：{@code ALTER COLUMN [col] <type> [NOT NULL]}（无 USING 子句）</li>
 *   <li>改默认值：默认值是具名约束，需先 {@code DROP CONSTRAINT [defaultConstraintName]} 再 {@code ADD DEFAULT <v> FOR [col]}</li>
 *   <li>重命名列：{@code EXEC sp_rename}</li>
 *   <li>删索引：{@code DROP INDEX [idx] ON [schema].[table]}（必须带 ON table）</li>
 *   <li>注释：{@code sp_addextendedproperty}（不存在）或 {@code sp_updateextendedproperty}（已存在）</li>
 * </ul>
 *
 * @author don
 */
@Slf4j
public class ModifyTableSqlBuilder {

    private ModifyTableSqlBuilder() {
    }

    /**
     * 构建修改表的全部 SQL。
     *
     * @param compareTableInfo 差异信息
     * @return sql 列表，每条为独立语句
     */
    public static List<String> buildSql(SqlServerCompareTableInfo compareTableInfo) {

        String schema = compareTableInfo.getSchema();
        String tableName = compareTableInfo.getName();
        String wrappedTable = IStrategy.concatWrapIdentifiers(schema, tableName);

        List<String> sqlList = new ArrayList<>();

        // 1. 删除索引（须在删列前，避免列被索引引用导致删列失败）
        for (String indexName : compareTableInfo.getDropIndexList()) {
            sqlList.add("DROP INDEX " + IStrategy.wrapIdentifiers(indexName) + " ON " + wrappedTable);
        }

        // 2. 删除主键约束
        String primaryKeyName = compareTableInfo.getDropPrimaryKeyName();
        if (StringUtils.hasText(primaryKeyName)) {
            sqlList.add("ALTER TABLE " + wrappedTable + " DROP CONSTRAINT " + IStrategy.wrapIdentifiers(primaryKeyName));
        }

        // 3. 删除列
        for (String columnName : compareTableInfo.getDropColumnList()) {
            sqlList.add("ALTER TABLE " + wrappedTable + " DROP COLUMN " + IStrategy.wrapIdentifiers(columnName));
        }

        // 4. 重命名列（逻辑删除）
        Map<String, String> renameColumnMap = compareTableInfo.getRenameColumnMap();
        renameColumnMap.forEach((oldName, newName) -> {
            // EXEC sp_rename N'schema.table.old', N'new', N'COLUMN'
            // sp_rename 第一个参数为对象名字符串（不带方括号包裹），格式 schema.table.column
            String qualifiedName = schema + "." + tableName + "." + oldName;
            sqlList.add("EXEC sp_rename N'" + escapeSingleQuote(qualifiedName) + "', N'" + escapeSingleQuote(newName) + "', N'COLUMN'");
        });

        // 5. 新增列
        for (ColumnMetadata column : compareTableInfo.getNewColumnMetadataList()) {
            sqlList.add("ALTER TABLE " + wrappedTable + " ADD " + ColumnSqlBuilder.buildSql(column));
        }

        // 6. 修改列（类型/非空/默认值）
        for (SqlServerCompareTableInfo.SqlServerModifyColumnMetadata modifyColumn : compareTableInfo.getModifyColumnMetadataList()) {
            ColumnMetadata columnMetadata = modifyColumn.getColumnMetadata();
            String columnName = columnMetadata.getName();
            String wrappedColumn = IStrategy.wrapIdentifiers(columnName);

            // 6.1 默认值变更：先 drop 旧默认约束
            //     SQLServer 中列存在默认约束时 ALTER COLUMN 改类型/非空可能失败，故先解除约束再改列
            if (modifyColumn.isDefaultChanged()) {
                String oldDefaultConstraintName = modifyColumn.getDefaultConstraintName();
                if (StringUtils.hasText(oldDefaultConstraintName)) {
                    sqlList.add("ALTER TABLE " + wrappedTable + " DROP CONSTRAINT " + IStrategy.wrapIdentifiers(oldDefaultConstraintName));
                }
            }

            // 6.2 类型或非空变更：合并为一条 ALTER COLUMN（IDENTITY 列不能通过 ALTER COLUMN 改类型，跳过）
            if ((modifyColumn.isTypeChanged() || modifyColumn.isNotNullChanged()) && !columnMetadata.isAutoIncrement()) {
                // 改类型或非空均需带上完整类型与 NULL/NOT NULL
                String fullType = columnMetadata.getType().getDefaultFullType();
                sqlList.add("ALTER TABLE " + wrappedTable + " ALTER COLUMN " + wrappedColumn + " " + fullType + (columnMetadata.isNotNull() ? " NOT NULL" : " NULL"));
            } else if (modifyColumn.isTypeChanged() && columnMetadata.isAutoIncrement()) {
                log.warn("SQLServer 不支持通过 ALTER COLUMN 修改自增列[{}]的类型，已跳过", columnName);
            }

            // 6.3 默认值变更：add 新默认值
            if (modifyColumn.isDefaultChanged()) {
                String newDefaultVal = resolveDefaultValue(columnMetadata);
                if (StringUtils.hasText(newDefaultVal)) {
                    // 匿名默认约束，SQLServer 自动生成名，便于后续 compare 查询
                    sqlList.add("ALTER TABLE " + wrappedTable + " ADD DEFAULT " + newDefaultVal + " FOR " + wrappedColumn);
                }
            }
        }

        // 7. 添加主键
        List<ColumnMetadata> newPrimaries = compareTableInfo.getNewPrimaries();
        if (!newPrimaries.isEmpty()) {
            String primaryColumns = newPrimaries.stream()
                    .map(ColumnMetadata::getName)
                    .map(IStrategy::wrapIdentifiers)
                    .collect(Collectors.joining(", "));
            // 无名主键约束，SQLServer 自动生成名
            sqlList.add("ALTER TABLE " + wrappedTable + " ADD PRIMARY KEY (" + primaryColumns + ")");
        }

        // 8. 创建索引（新增/修改）
        sqlList.addAll(CreateTableSqlBuilder.getCreateIndexSql(schema, tableName, compareTableInfo.getIndexMetadataList()));

        // 9. 注释（表/列/索引），区分 add 与 update
        sqlList.addAll(buildCommentSql(compareTableInfo, schema, tableName));

        return sqlList;
    }

    /**
     * 解析列的新默认值字面量，无默认值返回 null（表示删除默认值）。
     */
    private static String resolveDefaultValue(ColumnMetadata columnMetadata) {
        DefaultValueEnum defaultValueType = columnMetadata.getDefaultValueType();
        if (DefaultValueEnum.EMPTY_STRING == defaultValueType) {
            return "''";
        }
        if (DefaultValueEnum.NULL == defaultValueType) {
            return "NULL";
        }
        String defaultValue = columnMetadata.getDefaultValue();
        if (StringUtils.hasText(defaultValue)) {
            return defaultValue;
        }
        return null;
    }

    /**
     * 构建注释 SQL（区分 sp_addextendedproperty 与 sp_updateextendedproperty）。
     * <p>复用 compare 阶段记录的 DB 注释存在性（{@link SqlServerCompareTableInfo#isTableCommentExists()} 等），
     * 不再重复查询系统目录。</p>
     */
    private static List<String> buildCommentSql(SqlServerCompareTableInfo compareTableInfo, String schema, String tableName) {

        List<String> sqlList = new ArrayList<>();

        String tableComment = compareTableInfo.getComment();
        Map<String, String> columnComment = compareTableInfo.getColumnComment();
        Map<String, String> indexComment = compareTableInfo.getIndexComment();

        if (!StringUtils.hasText(tableComment) && columnComment.isEmpty() && indexComment.isEmpty()) {
            return sqlList;
        }

        // 复用 compare 阶段已查询的 DB 注释存在性，避免此处重复查询系统目录
        boolean tableCommentExists = compareTableInfo.isTableCommentExists();
        Map<String, Boolean> columnCommentExists = compareTableInfo.getColumnCommentExists();
        Map<String, Boolean> indexCommentExists = compareTableInfo.getIndexCommentExists();

        // 表注释
        if (StringUtils.hasText(tableComment)) {
            if (tableCommentExists) {
                sqlList.add(CreateTableSqlBuilder.getUpdateExtendedPropertySql(tableComment, schema, tableName, null));
            } else {
                sqlList.add(CreateTableSqlBuilder.getAddExtendedPropertySql(tableComment, schema, tableName, null));
            }
        }

        // 列注释
        columnComment.forEach((columnName, newComment) -> {
            if (columnCommentExists.getOrDefault(columnName, false)) {
                sqlList.add(CreateTableSqlBuilder.getUpdateExtendedPropertySql(newComment, schema, tableName, columnName));
            } else {
                sqlList.add(CreateTableSqlBuilder.getAddExtendedPropertySql(newComment, schema, tableName, columnName));
            }
        });

        // 索引注释
        indexComment.forEach((indexName, newComment) -> {
            if (indexCommentExists.getOrDefault(indexName, false)) {
                sqlList.add(CreateTableSqlBuilder.getUpdateExtendedPropertySql(newComment, schema, tableName, indexName, true));
            } else {
                sqlList.add(CreateTableSqlBuilder.getAddExtendedPropertySql(newComment, schema, tableName, indexName, true));
            }
        });

        return sqlList;
    }

    /**
     * 单引号转义为两个单引号，用于 sp_rename 的 N'...' 字符串参数（遵循 SQL 字符串转义规范）。
     */
    private static String escapeSingleQuote(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}
