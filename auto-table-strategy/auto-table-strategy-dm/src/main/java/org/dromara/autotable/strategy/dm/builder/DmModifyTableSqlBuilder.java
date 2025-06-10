package org.dromara.autotable.strategy.dm.builder;

/**
 * @author Min, Freddy
 * @date: 2025/2/25 23:06
 */

import org.dromara.autotable.strategy.dm.DmStrategy;
import org.dromara.autotable.strategy.dm.data.DmCompareTableInfo;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 达梦表修改SQL生成器
 */
public class DmModifyTableSqlBuilder {

    public static List<String> buildSql(DmCompareTableInfo compareInfo) {
        List<String> sqlList = new ArrayList<>();
        String qualifiedTableName = DmStrategy.withSchemaName(compareInfo.getSchema(), compareInfo.getName());

        // 1. 删除旧索引（独立语句）
        compareInfo.getDropIndexList().forEach(index ->
                sqlList.add("DROP INDEX " + DmStrategy.withSchemaName(compareInfo.getSchema(), index) + ";")
        );
        // 2. 字段修改
        compareInfo.getModifyColumnMetadataList().forEach(column -> {
            String columnDef = ColumnSqlBuilder.buildSql(column)
                    .replaceFirst("\"\\w+\"", "\"" + column.getName() + "\"");
            if (StringUtils.hasText(columnDef)) { // 增加有效性校验
                sqlList.add("ALTER TABLE " + qualifiedTableName + " MODIFY " + columnDef + ";");
            }
        });

        // 3. 其他ALTER操作拆分为独立语句
        // 删除主键
        if (StringUtils.hasText(compareInfo.getDropPrimaryKeyName())) {
            sqlList.add("ALTER TABLE " + qualifiedTableName
                    + " DROP CONSTRAINT \"" + compareInfo.getDropPrimaryKeyName() + "\";");
        }

        // 删除列（每个列独立）
        compareInfo.getDropColumnList().forEach(column ->
                sqlList.add("ALTER TABLE " + qualifiedTableName
                        + " DROP COLUMN \"" + column + "\";")
        );

        // 新增列（每个列独立）
        compareInfo.getNewColumnMetadataList().forEach(column ->
                sqlList.add("ALTER TABLE " + qualifiedTableName
                        + " ADD " + ColumnSqlBuilder.buildSql(column) + ";")
        );

        // 添加主键（独立语句）
        if (!compareInfo.getNewPrimaries().isEmpty()) {
            String pkColumns = compareInfo.getNewPrimaries().stream()
                    .map(col -> "\"" + col.getName() + "\"")
                    .collect(Collectors.joining(", "));
            String pkStatement = "ADD PRIMARY KEY (" + pkColumns + ")";
            sqlList.add("ALTER TABLE " + qualifiedTableName + " " + pkStatement + ";");
        }

        // 4. 索引生成（增加空校验）
        List<String> indexStatements = DmCreateTableSqlBuilder.buildIndexStatements(
                compareInfo.getSchema(),
                compareInfo.getName(),
                compareInfo.getIndexMetadataList());
        if (!indexStatements.isEmpty()) {
            sqlList.addAll(indexStatements);
        }

        // 5. 注释（增加空校验）
        List<String> commentStatements = buildCommentStatements(compareInfo);
        if (!commentStatements.isEmpty()) {
            sqlList.addAll(commentStatements);
        }

        // 过滤空元素
        return sqlList.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private static List<String> buildCommentStatements(DmCompareTableInfo compareInfo) {
        List<String> comments = new ArrayList<>();
        String qualifiedTableName = DmStrategy.withSchemaName(compareInfo.getSchema(), compareInfo.getName());

        // 表注释
        if (StringUtils.hasText(compareInfo.getComment())) {
            comments.add("COMMENT ON TABLE " + qualifiedTableName
                    + " IS '" + compareInfo.getComment().replace("'", "''") + "';");
        }

        // 列注释（每个列独立）
        compareInfo.getColumnComment().forEach((col, comment) -> {
            comments.add("COMMENT ON COLUMN " + qualifiedTableName + ".\"" + col
                    + "\" IS '" + comment.replace("'", "''") + "';");
        });

        return comments;
    }
}
