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
 * @author freddy
 */
public class DmCreateTableSqlBuilder {

    public static String buildSql(DefaultTableMetadata tableMetadata) {
        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();

        // 构建CREATE TABLE语句
        String createTableSql = buildCreateTableStatement(tableMetadata);

        // 构建索引语句
        String indexSql = buildIndexStatements(schema, tableName, tableMetadata.getIndexMetadataList());

        return String.join(";\n", createTableSql, indexSql);
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
            columns.add("PRIMARY KEY (\"" + String.join(", ", primaries) + "\")");
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
        return StringConnectHelper.newInstance("CREATE {unique}INDEX {indexName} ON {schemaTable} ({columns})")
                .replace("{unique}", index.getType() == IndexTypeEnum.UNIQUE ? "UNIQUE " : "")
                .replace("{indexName}", index.getName())
                // 关键修改点：统一处理模式名和表名
                .replace("{schemaTable}", buildSchemaTableName(schema, tableName))
                .replace("{columns}", index.getColumns().stream()
                        .map(col -> ColumnSqlBuilder.wrapColumnName(col.getColumn())
                                + (col.getSort() != null ? " " + col.getSort() : ""))
                        .collect(Collectors.joining(", ")))
                .toString() + ";";
    }

    private static String buildSchemaTableName(String schema, String tableName) {
        String wrappedSchema = ColumnSqlBuilder.wrapColumnName(schema);
        String wrappedTable = ColumnSqlBuilder.wrapColumnName(tableName);
        return StringUtils.hasText(schema)
                ? wrappedSchema + "." + wrappedTable
                : wrappedTable;
    }


}