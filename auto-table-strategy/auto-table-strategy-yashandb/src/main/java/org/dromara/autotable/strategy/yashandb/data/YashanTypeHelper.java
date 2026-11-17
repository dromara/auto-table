package org.dromara.autotable.strategy.yashandb.data;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps MySQL-compatible column types to YashanDB DDL types.
 */
public final class YashanTypeHelper {
    private static final Map<String, String> MYSQL_TYPE_ALIASES;
    private static final Pattern SIGN_MODIFIERS = Pattern.compile("\\s+(UNSIGNED|SIGNED|ZEROFILL)\\b");
    private static final Pattern BIT_LITERAL = Pattern.compile("^[bB]'([01]+)'$");
    private static final Set<String> CHAR_TYPES = new HashSet<>(Arrays.asList(
            "CHAR", "VARCHAR", "VARCHAR2", "NCHAR", "NVARCHAR", "NVARCHAR2", "CLOB", "NCLOB"));

    static {
        Map<String, String> aliases = new HashMap<>();
        // These aliases are accepted by the OA/MySQL model but need YashanDB DDL names.
        aliases.put("INT", "INTEGER");
        aliases.put("MEDIUMINT", "INTEGER");
        aliases.put("YEAR", "INTEGER");
        aliases.put("TEXT", "CLOB");
        aliases.put("TINYTEXT", "CLOB");
        aliases.put("MEDIUMTEXT", "CLOB");
        aliases.put("LONGTEXT", "CLOB");
        aliases.put("JSON", "CLOB");
        aliases.put("TINYBLOB", "BLOB");
        aliases.put("MEDIUMBLOB", "BLOB");
        aliases.put("LONGBLOB", "BLOB");
        // MySQL BIT(1)/BOOL models are booleans; store them like Java Boolean (TINYINT).
        aliases.put("BIT", "TINYINT");
        aliases.put("BOOL", "TINYINT");
        aliases.put("BOOLEAN", "TINYINT");
        MYSQL_TYPE_ALIASES = Collections.unmodifiableMap(aliases);
    }

    private YashanTypeHelper() {
    }

    /**
     * Converts a MySQL-compatible type name to a YashanDB DDL type name.
     */
    public static String toDdlType(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        return MYSQL_TYPE_ALIASES.getOrDefault(normalized, normalized);
    }

    /**
     * Returns a comparison form with aliases and precision suffixes normalized.
     */
    public static String canonicalType(String type) {
        // MySQL-mode catalogs report e.g. "BIGINT UNSIGNED"; the sign modifier does not change the base type.
        String base = type == null ? "" : SIGN_MODIFIERS.matcher(type.trim().toUpperCase(Locale.ROOT)).replaceAll("");
        String normalized = toDdlType(base).replaceAll("\\s+", "");
        int parenthesis = normalized.indexOf('(');
        if (parenthesis >= 0) {
            normalized = normalized.substring(0, parenthesis);
        }
        if ("DECIMAL".equals(normalized) || "NUMERIC".equals(normalized)) {
            return "NUMBER";
        }
        // The catalog reports DATETIME columns as TIMESTAMP(n).
        if ("DATETIME".equals(normalized)) {
            return "TIMESTAMP";
        }
        return toDdlType(normalized);
    }

    /**
     * Returns whether a model type stores character data, so that literal defaults need quoting.
     */
    public static boolean isCharType(String type) {
        String canonical = canonicalType(type);
        return CHAR_TYPES.contains(canonical);
    }

    /**
     * Returns whether a model type stores date/time values.
     */
    public static boolean isDateTimeType(String type) {
        String canonical = canonicalType(type);
        return "TIMESTAMP".equals(canonical) || "DATE".equals(canonical) || "TIME".equals(canonical);
    }

    /**
     * Returns whether a model type is stored as a boolean-style TINYINT.
     */
    public static boolean isBooleanType(String type) {
        return "TINYINT".equals(canonicalType(type));
    }

    /**
     * Converts a MySQL bit literal default such as {@code b'1'} to the numeric literal YashanDB accepts.
     */
    public static String normalizeBitLiteral(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = BIT_LITERAL.matcher(value.trim());
        return matcher.matches() ? new BigInteger(matcher.group(1), 2).toString() : value;
    }
}
