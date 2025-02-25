package org.dromara.autotable.core.strategy.dm.builder;

import org.dromara.autotable.annotation.ColumnDefault;
import org.dromara.autotable.core.builder.ColumnMetadataBuilder;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.dm.data.DamengTypeHelper;
import org.dromara.autotable.core.utils.StringUtils;

/**
 * @author Min, Freddy
 * @date: 2025/2/25 23:03
 */
public class DamengColumnMetadataBuilder extends ColumnMetadataBuilder {
    public DamengColumnMetadataBuilder() {
        super(DatabaseDialect.DM);
    }

    @Override
    protected String getDefaultValue(DatabaseTypeAndLength typeAndLength, ColumnDefault columnDefault) {
        String defaultValue = super.getDefaultValue(typeAndLength, columnDefault);

        if (StringUtils.hasText(defaultValue)) {
            // 布尔值处理
            if (DamengTypeHelper.isBoolean(typeAndLength)) {
                if ("1".equals(defaultValue)) {
                    return "1";
                } else if ("0".equals(defaultValue)) {
                    return "0";
                }
            }

            // 处理函数型默认值
            if (isFunctionDefault(defaultValue)) {
                return defaultValue;
            }

            // 字符串类型处理
            if (DamengTypeHelper.isCharString(typeAndLength) && !defaultValue.startsWith("'")) {
                return "'" + defaultValue + "'";
            }

            // 时间类型处理
            if (DamengTypeHelper.isTime(typeAndLength) && !defaultValue.startsWith("'")) {
                return "'" + defaultValue + "'";
            }
        }
        return defaultValue;
    }

    private boolean isFunctionDefault(String value) {
        return value.toUpperCase().matches("^(SYSDATE|CURRENT_TIMESTAMP|NEXTVAL\\()");
    }
}
