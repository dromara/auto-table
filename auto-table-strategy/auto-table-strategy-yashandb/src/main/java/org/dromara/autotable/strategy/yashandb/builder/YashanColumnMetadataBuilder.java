package org.dromara.autotable.strategy.yashandb.builder;

import org.dromara.autotable.annotation.ColumnDefault;
import org.dromara.autotable.core.builder.ColumnMetadataBuilder;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.strategy.yashandb.data.YashanTypeHelper;

/**
 * Builds AutoTable column metadata using the YashanDB MySQL-compatible dialect.
 */
public class YashanColumnMetadataBuilder extends ColumnMetadataBuilder {
    public YashanColumnMetadataBuilder() {
        super(DatabaseDialect.YashanDB);
    }

    /**
     * Normalizes literal defaults the same way the MySQL strategy does, so MySQL models keep working:
     * character and date-time literals are quoted, boolean words and bit literals become 1/0.
     */
    @Override
    protected String getDefaultValue(DatabaseTypeAndLength typeAndLength, ColumnDefault columnDefault) {
        String defaultValue = super.getDefaultValue(typeAndLength, columnDefault);
        if (!StringUtils.hasText(defaultValue) || typeAndLength == null) {
            return defaultValue;
        }
        String type = typeAndLength.getType();
        defaultValue = YashanTypeHelper.normalizeBitLiteral(defaultValue);
        if (YashanTypeHelper.isBooleanType(type)
                && ("true".equalsIgnoreCase(defaultValue) || "false".equalsIgnoreCase(defaultValue))) {
            return Boolean.parseBoolean(defaultValue) ? "1" : "0";
        }
        if (isQuoted(defaultValue)) {
            return defaultValue;
        }
        if (YashanTypeHelper.isCharType(type)) {
            return "'" + defaultValue.replace("'", "''") + "'";
        }
        // Date-time literals are quoted; functions such as CURRENT_TIMESTAMP are kept as expressions.
        if (YashanTypeHelper.isDateTimeType(type) && defaultValue.matches(StringUtils.DATETIME_REGEX)) {
            return "'" + defaultValue + "'";
        }
        return defaultValue;
    }

    private static boolean isQuoted(String value) {
        return value.length() >= 2 && value.startsWith("'") && value.endsWith("'");
    }
}
