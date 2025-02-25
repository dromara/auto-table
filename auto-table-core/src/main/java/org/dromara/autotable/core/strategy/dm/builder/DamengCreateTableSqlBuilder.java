package org.dromara.autotable.core.strategy.dm.builder;

import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.strategy.dm.DmStrategy;
import org.dromara.autotable.core.utils.StringConnectHelper;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 达梦建表SQL生成器
 */
public class DamengCreateTableSqlBuilder {

    public static String buildSql(DefaultTableMetadata tableMetadata) {
        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();

        // 构建CREATE TABLE语句
        String createTableSql = buildCreateTableStatement(tableMetadata);

        // 构建索引语句
        String indexSql = buildIndexStatements(schema, tableName, tableMetadata.getIndexMetadataList());

        // 构建注释语句
        String commentSql = buildCommentStatements(tableMetadata);

        return String.join("\n", createTableSql, indexSql, commentSql);
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
            columns.add("PRIMARY KEY (" + String.join(", ", primaries) + ")");
        }

        return String.format("CREATE TABLE %s (\n  %s\n)",
                DmStrategy.withSchemaName(metadata.getSchema(), metadata.getTableName()),
                String.join(",\n  ", columns));
    }

    static String buildIndexStatements(String schema, String tableName, List<IndexMetadata> indexes) {
        return indexes.stream()
                .map(index -> buildIndexStatement(schema, tableName, index))
                .collect(Collectors.joining("\n"));
    }

    private static String buildIndexStatement(String schema, String tableName, IndexMetadata index) {
        return StringConnectHelper.newInstance("CREATE {unique}INDEX {indexName} ON {tableName} ({columns})")
                .replace("{unique}", index.getType() == IndexTypeEnum.UNIQUE ? "UNIQUE " : "")
                .replace("{indexName}", index.getName())
                .replace("{tableName}", DmStrategy.withSchemaName(schema, tableName))
                .replace("{columns}", index.getColumns().stream()
                        .map(col -> col.getColumn() + (col.getSort() != null ? " " + col.getSort() : ""))
                        .collect(Collectors.joining(", ")))
                .toString() + ";";
    }

    private static String buildCommentStatements(DefaultTableMetadata metadata) {
        List<String> comments = new ArrayList<>();
        String qualifiedTableName = DmStrategy.withSchemaName(metadata.getSchema(), metadata.getTableName());

        // 表注释
        if (StringUtils.hasText(metadata.getComment())) {
            comments.add(String.format("COMMENT ON TABLE %s IS '%s';",
                    qualifiedTableName,
                    metadata.getComment()));
        }

        // 列注释
        metadata.getColumnMetadataList().forEach(column -> {
            if (StringUtils.hasText(column.getComment())) {
                comments.add(String.format("COMMENT ON COLUMN %s.%s IS '%s';",
                        qualifiedTableName,
                        column.getName(),
                        column.getComment()));
            }
        });

        // 索引注释（达梦不支持直接注释索引）
        return String.join("\n", comments);
    }
}