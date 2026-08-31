package org.dromara.autotable.strategy.yashandb.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Maps MySQL-compatible column types to YashanDB DDL types.
 */
public final class YashanTypeHelper {
    private static final Map<String, String> MYSQL_TYPE_ALIASES;

    static {
        Map<String, String> aliases = new HashMap<>();
        // These aliases are accepted by the OA/MySQL model but need YashanDB DDL names.
        aliases.put("INT", "INTEGER");
        aliases.put("MEDIUMINT", "INTEGER");
        aliases.put("YEAR", "INTEGER");
        aliases.put("DATETIME", "TIMESTAMP");
        aliases.put("TEXT", "CLOB");
        aliases.put("TINYTEXT", "CLOB");
        aliases.put("MEDIUMTEXT", "CLOB");
        aliases.put("LONGTEXT", "CLOB");
        aliases.put("JSON", "CLOB");
        aliases.put("TINYBLOB", "BLOB");
        aliases.put("MEDIUMBLOB", "BLOB");
        aliases.put("LONGBLOB", "BLOB");
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
        String normalized = toDdlType(type).replaceAll("\\s+", "");
        int parenthesis = normalized.indexOf('(');
        if (parenthesis >= 0) {
            normalized = normalized.substring(0, parenthesis);
        }
        if ("DECIMAL".equals(normalized) || "NUMERIC".equals(normalized)) {
            return "NUMBER";
        }
        return normalized;
    }
}
