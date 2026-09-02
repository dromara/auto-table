package org.dromara.autotable.strategy.oracle;

import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Oracle 标识符处理工具。
 */
final class OracleIdentifierUtils {

    private static final String SEQUENCE_PREFIX = "auto_seq_";

    private OracleIdentifierUtils() {
    }

    static String wrap(String name) {
        return IStrategy.wrapIdentifiers(name);
    }

    static String sequenceName(String tableName) {
        return SEQUENCE_PREFIX + tableName;
    }

    static String sequenceNextVal(String sequenceName) {
        return wrap(sequenceName) + ".NEXTVAL";
    }

    static String resolveExistingName(Collection<String> actualNames, String expectedName) {
        if (actualNames == null || expectedName == null) {
            return null;
        }
        for (String actualName : actualNames) {
            if (expectedName.equals(actualName)) {
                return actualName;
            }
        }
        String legacyName = expectedName.toUpperCase(Locale.ROOT);
        for (String actualName : actualNames) {
            if (legacyName.equals(actualName)) {
                return actualName;
            }
        }
        return null;
    }

    static <T> T resolveExisting(Collection<T> values, String expectedName, Function<T, String> nameGetter) {
        if (values == null || expectedName == null) {
            return null;
        }
        for (T value : values) {
            if (expectedName.equals(nameGetter.apply(value))) {
                return value;
            }
        }
        String legacyName = expectedName.toUpperCase(Locale.ROOT);
        for (T value : values) {
            if (legacyName.equals(nameGetter.apply(value))) {
                return value;
            }
        }
        return null;
    }

    static boolean isSequenceNextVal(String defaultValue, String actualSequenceName) {
        if (!StringUtils.hasText(defaultValue) || !StringUtils.hasText(actualSequenceName)) {
            return false;
        }
        List<IdentifierPart> parts = splitQualifiedIdentifier(defaultValue.trim());
        if (parts.size() < 2) {
            return false;
        }
        IdentifierPart nextVal = parts.get(parts.size() - 1);
        if (!"NEXTVAL".equalsIgnoreCase(nextVal.value)) {
            return false;
        }
        IdentifierPart sequence = parts.get(parts.size() - 2);
        if (sequence.quoted) {
            return actualSequenceName.equals(sequence.value);
        }
        return actualSequenceName.equals(sequence.value.toUpperCase(Locale.ROOT));
    }

    private static List<IdentifierPart> splitQualifiedIdentifier(String expression) {
        List<IdentifierPart> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean currentQuoted = false;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < expression.length() && expression.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                    continue;
                }
                quoted = !quoted;
                currentQuoted = true;
                continue;
            }
            if (ch == '.' && !quoted) {
                addIdentifierPart(parts, current, currentQuoted);
                currentQuoted = false;
                continue;
            }
            if (!Character.isWhitespace(ch) || quoted) {
                current.append(ch);
            }
        }
        addIdentifierPart(parts, current, currentQuoted);
        return parts;
    }

    private static void addIdentifierPart(List<IdentifierPart> parts, StringBuilder current, boolean quoted) {
        if (current.length() > 0) {
            parts.add(new IdentifierPart(current.toString(), quoted));
            current.setLength(0);
        }
    }

    private static final class IdentifierPart {
        private final String value;
        private final boolean quoted;

        private IdentifierPart(String value, boolean quoted) {
            this.value = value;
            this.quoted = quoted;
        }
    }
}
