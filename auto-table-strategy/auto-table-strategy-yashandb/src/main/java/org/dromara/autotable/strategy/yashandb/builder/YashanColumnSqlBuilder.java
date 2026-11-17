package org.dromara.autotable.strategy.yashandb.builder;

import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.strategy.yashandb.data.YashanTypeHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Builds YashanDB column definitions from MySQL-compatible AutoTable metadata.
 */
public final class YashanColumnSqlBuilder {
    private static final Set<String> LENGTH_TYPES = new HashSet<>(Arrays.asList(
            "CHAR", "VARCHAR", "VARCHAR2", "BINARY", "VARBINARY"));
    private static final Set<String> PRECISION_TYPES = new HashSet<>(Arrays.asList(
            "NUMBER", "NUMERIC", "DECIMAL"));
    private static final Set<String> FRACTIONAL_SECOND_TYPES = new HashSet<>(Arrays.asList(
            "TIMESTAMP", "DATETIME", "TIME"));

    private YashanColumnSqlBuilder() {
    }

    /**
     * Builds one YashanDB column definition, including identity, default and inline comment clauses.
     *
     * <p>The comment is written inline (MySQL syntax) instead of {@code COMMENT ON}: in MySQL compatibility
     * mode an application user usually holds privileges on the database ({@code GRANT ... ON db.*}) but does
     * not own its schema, and {@code COMMENT ON} on another owner's table fails with YAS-02213.</p>
     */
    public static String buildSql(ColumnMetadata column) {
        if (column.isAutoIncrement() && !column.isPrimary()) {
            throw new IllegalArgumentException("YashanDB AUTO_INCREMENT column must be a primary key: " + column.getName());
        }
        StringBuilder sql = new StringBuilder()
                .append(IStrategy.wrapIdentifiers(column.getName()))
                .append(' ')
                .append(buildType(column.getType()));
        // YashanDB identity columns are emitted only for model primary keys.
        if (column.isAutoIncrement()) {
            sql.append(" AUTO_INCREMENT");
        }
        if (column.isNotNull() || column.isPrimary() || column.isAutoIncrement()) {
            sql.append(" NOT NULL");
        } else {
            sql.append(" NULL");
        }
        String defaultClause = buildDefault(column);
        if (StringUtils.hasText(defaultClause)) {
            sql.append(' ').append(defaultClause);
        }
        if (StringUtils.hasText(column.getComment())) {
            sql.append(" COMMENT '").append(YashanCreateTableSqlBuilder.escape(column.getComment())).append('\'');
        }
        return sql.toString();
    }

    /**
     * Converts a model type and optional length/precision into a YashanDB type expression.
     */
    public static String buildType(DatabaseTypeAndLength type) {
        if (type == null || !StringUtils.hasText(type.getType())) {
            throw new IllegalArgumentException("YashanDB column type must not be empty");
        }
        String typeName = YashanTypeHelper.toDdlType(type.getType());
        Integer length = type.getLength();
        Integer scale = type.getDecimalLength();
        // Only BIT(1) is a boolean; wider MySQL bit fields keep YashanDB's native BIT(n).
        if ("BIT".equalsIgnoreCase(type.getType().trim()) && length != null && length > 1) {
            return "BIT(" + length + ")";
        }
        if (FRACTIONAL_SECOND_TYPES.contains(typeName) && length != null && length > 0) {
            return typeName + "(" + length + ")";
        }
        // Precision types use (precision, scale); character types use a single length.
        if (PRECISION_TYPES.contains(typeName) && length != null) {
            return scale == null ? typeName + "(" + length + ")" : typeName + "(" + length + "," + scale + ")";
        }
        if (LENGTH_TYPES.contains(typeName) && length != null) {
            return typeName + "(" + length + ")";
        }
        return typeName;
    }

    /**
     * Builds a default-value clause while preserving AutoTable's default-value semantics.
     */
    private static String buildDefault(ColumnMetadata column) {
        if (column.isAutoIncrement()) {
            return "";
        }
        if (column.getDefaultValueType() == DefaultValueEnum.NULL) {
            return "DEFAULT NULL";
        }
        if (column.getDefaultValueType() == DefaultValueEnum.EMPTY_STRING) {
            return "DEFAULT ''";
        }
        if (DefaultValueEnum.isCustom(column.getDefaultValueType())
                && StringUtils.hasText(column.getDefaultValue())) {
            return "DEFAULT " + YashanTypeHelper.normalizeBitLiteral(column.getDefaultValue());
        }
        return "";
    }
}
