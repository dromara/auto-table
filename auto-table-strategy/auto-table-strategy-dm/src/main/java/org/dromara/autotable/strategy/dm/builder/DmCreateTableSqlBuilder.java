package org.dromara.autotable.strategy.dm.builder;

import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.utils.StringConnectHelper;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 达梦建表SQL生成器
 *
 * @author freddy
 */
public class DmCreateTableSqlBuilder {

    public static List<String> buildSql(DefaultTableMetadata tableMetadata) {
        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();

        // 构建CREATE TABLE语句
        String createTableSql = buildCreateTableStatement(tableMetadata);

        // 构建索引语句
        List<String> indexSql = buildIndexStatements(schema, tableName, tableMetadata.getIndexMetadataList());
        indexSql.add(0, createTableSql);

        // 构建注释语句
        List<String> commentSql = buildCommentStatements(tableMetadata);
        indexSql.addAll(commentSql);

        return indexSql;
    }

    private static String buildCreateTableStatement(DefaultTableMetadata metadata) {
        List<String> columns = new ArrayList<>();
        List<String> primaries = new ArrayList<>();

        // 处理列定义
        for (ColumnMetadata column : metadata.getColumnMetadataList()) {
            String columnSql = ColumnSqlBuilder.buildSql(column);
            columns.add(columnSql);

            if (column.isPrimary()) {
                primaries.add(column.getName());
            }
        }

        // 添加主键约束
        if (!primaries.isEmpty()) {
            columns.add("PRIMARY KEY (" + IStrategy.customConcatWrapIdentifiers(", ", primaries) + ")");
        }

        return String.format("CREATE TABLE %s (\n  %s\n)",
                IStrategy.concatWrapIdentifiers(metadata.getSchema(), metadata.getTableName()),
                String.join(",\n  ", columns));
    }

    static List<String> buildIndexStatements(String schema, String tableName, List<IndexMetadata> indexes) {
        return indexes.stream()
                .map(index -> buildIndexStatement(schema, tableName, index))
                .collect(Collectors.toList());
    }

    private static String buildIndexStatement(String schema, String tableName, IndexMetadata index) {
        return StringConnectHelper.newInstance("CREATE {unique}INDEX {indexName} ON {schemaTable} ({columns})")
                .replace("{unique}", index.getType() == IndexTypeEnum.UNIQUE ? "UNIQUE " : "")
                .replace("{indexName}", index.getName())
                // 关键修改点：统一处理模式名和表名
                .replace("{schemaTable}", IStrategy.concatWrapIdentifiers(schema, tableName))
                .replace("{columns}", index.getColumns().stream()
                        .map(col -> IStrategy.wrapIdentifiers(col.getColumn())
                                + (col.getSort() != null ? " " + col.getSort() : ""))
                        .collect(Collectors.joining(", ")))
                .toString() + ";";
    }

    /**
     * 构建注释语句
     *
     * @param metadata 表元数据
     * @return 注释语句列表
     */
    private static List<String> buildCommentStatements(DefaultTableMetadata metadata) {
        List<String> comments = new ArrayList<>();
        String qualifiedTableName = IStrategy.concatWrapIdentifiers(metadata.getSchema(), metadata.getTableName());

        // 表注释
        if (StringUtils.hasText(metadata.getComment())) {
            comments.add("COMMENT ON TABLE " + qualifiedTableName
                    + " IS '" + metadata.getComment().replace("'", "''") + "';");
        }

        // 列注释
        for (ColumnMetadata column : metadata.getColumnMetadataList()) {
            if (StringUtils.hasText(column.getComment())) {
                comments.add("COMMENT ON COLUMN " + qualifiedTableName +
                        "." + IStrategy.wrapIdentifiers(column.getName()) + " IS '"
                        + column.getComment().replace("'", "''") + "';");
            }
        }

        return comments;
    }
}
