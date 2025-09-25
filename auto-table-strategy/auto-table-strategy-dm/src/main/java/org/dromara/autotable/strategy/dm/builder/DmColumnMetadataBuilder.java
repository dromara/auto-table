package org.dromara.autotable.strategy.dm.builder;

import org.dromara.autotable.annotation.ColumnDefault;
import org.dromara.autotable.core.builder.ColumnMetadataBuilder;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.strategy.dm.data.DmTypeHelper;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Min, Freddy
 * @date: 2025/2/25 23:03
 */
public class DmColumnMetadataBuilder extends ColumnMetadataBuilder {
    private static final Map<String, String> BOOLEAN_MAPPING = new HashMap<String, String>() {{
        put("TRUE", "1");
        put("FALSE", "0");
        put("1", "1");
        put("0", "0");
    }};

    private static final Pattern FUNCTION_PATTERN =
            Pattern.compile("^(SYSDATE|CURRENT_(DATE|TIMESTAMP)|NEXTVAL\\s*\\(|USER|UID)",
                    Pattern.CASE_INSENSITIVE);

    public DmColumnMetadataBuilder() {
        super(DatabaseDialect.DM);
    }

    @Override
    protected String getDefaultValue(DatabaseTypeAndLength typeAndLength, ColumnDefault columnDefault) {
        if (typeAndLength == null) {
            throw new IllegalArgumentException("typeAndLength参数不能为空");
        }

        String defaultValue = super.getDefaultValue(typeAndLength, columnDefault);

        if (!StringUtils.hasText(defaultValue)) {
            return defaultValue;
        }

        // 优先处理函数型默认值
        if (isFunctionDefault(defaultValue)) {
            return defaultValue;
        }

        // 布尔类型特殊处理
        if (DmTypeHelper.isBoolean(typeAndLength)) {
            return handleBooleanDefault(defaultValue);
        }

        // 字符/时间类型引号处理
        return wrapQuotesIfNeeded(defaultValue, typeAndLength);
    }

    private String handleBooleanDefault(String value) {
        String upperValue = value.toUpperCase();
        return BOOLEAN_MAPPING.getOrDefault(upperValue, value);
    }

    private boolean isFunctionDefault(String value) {
        return FUNCTION_PATTERN.matcher(value.toUpperCase()).find();
    }

    private String wrapQuotesIfNeeded(String value, DatabaseTypeAndLength type) {
        if (DmTypeHelper.isCharString(type) || DmTypeHelper.isTime(type)) {
            if (!value.startsWith("'")) {
                return "'" + value.replace("'", "''") + "'";
            }
        }
        return value;
    }
}
