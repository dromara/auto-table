package org.dromara.autotable.core.strategy.dm.builder;

/**
 * @author Min, Freddy
 * @date: 2025/2/25 23:06
 */

import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.dm.DmStrategy;
import org.dromara.autotable.core.strategy.dm.data.DmCompareTableInfo;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 达梦表修改SQL生成器
 */
public class DmModifyTableSqlBuilder {

    public static String buildSql(DmCompareTableInfo compareInfo) {
        List<String> sqlList = new ArrayList<>();
        String qualifiedTableName = DmStrategy.withSchemaName(compareInfo.getSchema(), compareInfo.getName());

        // 删除旧索引
        compareInfo.getDropIndexList().forEach(index ->
                sqlList.add("DROP INDEX " + DmStrategy.withSchemaName(compareInfo.getSchema(), index)));

        // 表结构变更
        List<String> alterClauses = new ArrayList<>();

        // 删除主键
        if (StringUtils.hasText(compareInfo.getDropPrimaryKeyName())) {
            alterClauses.add("DROP CONSTRAINT " + compareInfo.getDropPrimaryKeyName());
        }

        // 删除列
        compareInfo.getDropColumnList().forEach(column ->
                alterClauses.add("DROP COLUMN " + column));

        // 添加新列
        compareInfo.getNewColumnMetadataList().forEach(column ->
                alterClauses.add("ADD " + ColumnSqlBuilder.buildSql(column)));

        // 修改列
        compareInfo.getModifyColumnMetadataList().forEach(column -> {
            String columnName = column.getName();
            // 修改类型
            alterClauses.add(String.format("MODIFY %s %s",
                    columnName,
                    column.getType().getDefaultFullType()));
            // 修改非空约束
            alterClauses.add(String.format("MODIFY %s %s",
                    columnName,
                    column.isNotNull() ? "NOT NULL" : "NULL"));
            // 修改默认值
            if (StringUtils.hasText(column.getDefaultValue())) {
                alterClauses.add(String.format("MODIFY %s DEFAULT %s",
                        columnName,
                        column.getDefaultValue()));
            } else {
                alterClauses.add("MODIFY " + columnName + " DROP DEFAULT");
            }
        });

        // 添加主键
        if (!compareInfo.getNewPrimaries().isEmpty()) {
            String pkColumns = compareInfo.getNewPrimaries().stream()
                    .map(ColumnMetadata::getName)
                    .collect(Collectors.joining(", "));
            alterClauses.add("ADD PRIMARY KEY (" + pkColumns + ")");
        }

        // 生成ALTER TABLE语句
        if (!alterClauses.isEmpty()) {
            sqlList.add(String.format("ALTER TABLE %s\n  %s;",
                    qualifiedTableName,
                    String.join(",\n  ", alterClauses)));
        }

        // 新建索引
        sqlList.add(DmCreateTableSqlBuilder.buildIndexStatements(
                compareInfo.getSchema(),
                compareInfo.getName(),
                compareInfo.getIndexMetadataList()));

        // 添加注释
        sqlList.add(buildCommentStatements(compareInfo));

        return String.join("\n", sqlList);
    }

    private static String buildCommentStatements(DmCompareTableInfo compareInfo) {
        List<String> comments = new ArrayList<>();
        String qualifiedTableName = DmStrategy.withSchemaName(compareInfo.getSchema(), compareInfo.getName());

        // 表注释
        if (StringUtils.hasText(compareInfo.getComment())) {
            comments.add(String.format("COMMENT ON TABLE %s IS '%s';",
                    qualifiedTableName,
                    compareInfo.getComment()));
        }

        // 列注释
        compareInfo.getColumnComment().forEach((col, comment) -> {
            comments.add(String.format("COMMENT ON COLUMN %s.%s IS '%s';",
                    qualifiedTableName,
                    col,
                    comment));
        });

        return String.join("\n", comments);
    }
}
