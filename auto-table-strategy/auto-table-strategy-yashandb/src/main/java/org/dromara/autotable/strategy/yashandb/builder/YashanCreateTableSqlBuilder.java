package org.dromara.autotable.strategy.yashandb.builder;

import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.strategy.yashandb.YashanIdentifierUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds YashanDB DDL for schemas originating from MySQL-compatible metadata.
 */
public final class YashanCreateTableSqlBuilder {
    private YashanCreateTableSqlBuilder() {
    }

    /**
     * Builds CREATE TABLE (with inline table and column comments) followed by index statements.
     */
    public static List<String> buildSql(DefaultTableMetadata table) {
        List<String> sql = new ArrayList<>();
        sql.add(buildCreateTable(table));
        List<IndexMetadata> indexes = table.getIndexMetadataList() == null
                ? Collections.emptyList() : table.getIndexMetadataList();
        // Indexes are separate statements because YashanDB applies them after the table exists.
        for (IndexMetadata index : indexes) {
            sql.add(buildCreateIndex(table.getSchema(), table.getTableName(), index));
        }
        return sql;
    }

    /**
     * Builds the table definition and an optional composite primary key clause.
     */
    static String buildCreateTable(DefaultTableMetadata table) {
        List<String> definitions = table.getColumnMetadataList().stream()
                .map(YashanColumnSqlBuilder::buildSql)
                .collect(Collectors.toCollection(ArrayList::new));
        List<String> primaryColumns = table.getColumnMetadataList().stream()
                .filter(ColumnMetadata::isPrimary)
                .map(ColumnMetadata::getName)
                .collect(Collectors.toList());
        if (!primaryColumns.isEmpty()) {
            definitions.add("PRIMARY KEY (" + IStrategy.customConcatWrapIdentifiers(", ", primaryColumns) + ")");
        }
        String sql = "CREATE TABLE " + IStrategy.concatWrapIdentifiers(table.getSchema(), table.getTableName())
                + " (\n  " + String.join(",\n  ", definitions) + "\n)";
        if (StringUtils.hasText(table.getComment())) {
            sql += " COMMENT '" + escape(table.getComment()) + "'";
        }
        return sql;
    }

    /**
     * Builds a statement that drops one index; MySQL compatibility mode requires the owning table
     * ({@code DROP INDEX schema.index} fails with YAS-04256).
     */
    public static String buildDropIndex(String schema, String tableName, String indexName) {
        return "DROP INDEX " + IStrategy.wrapIdentifiers(indexName)
                + " ON " + IStrategy.concatWrapIdentifiers(schema, tableName);
    }

    /**
     * Builds a YashanDB index statement with a normalized unquoted index name.
     */
    public static String buildCreateIndex(String schema, String tableName, IndexMetadata index) {
        String columns = index.getColumns().stream()
                .map(column -> IStrategy.wrapIdentifiers(column.getColumn())
                        + (column.getSort() == null ? "" : " " + column.getSort().name()))
                .collect(Collectors.joining(", "));
        return "CREATE " + (index.getType() == IndexTypeEnum.UNIQUE ? "UNIQUE " : "")
                + "INDEX " + IStrategy.wrapIdentifiers(YashanIdentifierUtils.normalizeIndexName(index.getName()))
                + " ON " + IStrategy.concatWrapIdentifiers(schema, tableName)
                + " (" + columns + ")";
    }

    /**
     * Escapes a comment for a single-quoted YashanDB string literal.
     */
    static String escape(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}
