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
     * Builds CREATE TABLE followed by indexes and comments as separate statements.
     */
    public static List<String> buildSql(DefaultTableMetadata table) {
        List<String> sql = new ArrayList<>();
        sql.add(buildCreateTable(table));
        List<IndexMetadata> indexes = table.getIndexMetadataList() == null
                ? Collections.emptyList() : table.getIndexMetadataList();
        // Indexes and comments are separate because YashanDB applies them after the table exists.
        for (IndexMetadata index : indexes) {
            sql.add(buildCreateIndex(table.getSchema(), table.getTableName(), index));
        }
        sql.addAll(buildComments(table));
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
        return "CREATE TABLE " + IStrategy.concatWrapIdentifiers(table.getSchema(), table.getTableName())
                + " (\n  " + String.join(",\n  ", definitions) + "\n)";
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

    private static List<String> buildComments(DefaultTableMetadata table) {
        List<String> comments = new ArrayList<>();
        String qualifiedTable = IStrategy.concatWrapIdentifiers(table.getSchema(), table.getTableName());
        // YashanDB stores table and column comments through COMMENT ON statements.
        if (StringUtils.hasText(table.getComment())) {
            comments.add("COMMENT ON TABLE " + qualifiedTable + " IS '" + escape(table.getComment()) + "'");
        }
        for (ColumnMetadata column : table.getColumnMetadataList()) {
            if (StringUtils.hasText(column.getComment())) {
                comments.add("COMMENT ON COLUMN " + qualifiedTable + "."
                        + IStrategy.wrapIdentifiers(column.getName()) + " IS '" + escape(column.getComment()) + "'");
            }
        }
        return comments;
    }

    /**
     * Escapes a comment for a single-quoted YashanDB string literal.
     */
    static String escape(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}
