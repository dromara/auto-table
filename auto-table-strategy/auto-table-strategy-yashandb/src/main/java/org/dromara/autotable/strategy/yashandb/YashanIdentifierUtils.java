package org.dromara.autotable.strategy.yashandb;

/**
 * Identifier rules used when YashanDB identifiers are emitted without quotes.
 * YashanDB accepts letters, digits, _, $, and # in an unquoted identifier;
 * AutoTable can generate index names containing '-' in its hash suffix.
 */
public final class YashanIdentifierUtils {

    private YashanIdentifierUtils() {
    }

    /**
     * Replaces characters that cannot appear in an unquoted YashanDB index name.
     */
    public static String normalizeIndexName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder normalized = new StringBuilder(name.length());
        // AutoTable's generated hash suffix can contain '-', which YashanDB rejects here.
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            boolean valid = (ch >= 'A' && ch <= 'Z')
                    || (ch >= 'a' && ch <= 'z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '_' || ch == '$' || ch == '#';
            normalized.append(valid ? ch : '_');
        }
        return normalized.toString();
    }
}
