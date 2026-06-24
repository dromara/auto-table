package org.dromara.autotable.strategy.sqlserver.builder;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.annotation.ColumnDefault;
import org.dromara.autotable.core.builder.ColumnMetadataBuilder;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.strategy.sqlserver.data.SqlServerTypeHelper;

/**
 * SQLServer 列元数据构造器。
 *
 * <p>方言适配：</p>
 * <ul>
 *   <li>布尔默认值：SQLServer BIT 无 true/false，使用 1/0</li>
 *   <li>字符串默认值：自动补单引号</li>
 *   <li>日期默认值（非函数字面量）：自动补单引号</li>
 * </ul>
 *
 * @author don
 */
@Slf4j
public class SqlServerColumnMetadataBuilder extends ColumnMetadataBuilder {

    public SqlServerColumnMetadataBuilder() {
        super(DatabaseDialect.SQLServer);
    }

    @Override
    protected String getDefaultValue(DatabaseTypeAndLength typeAndLength, ColumnDefault columnDefault) {

        String defaultValue = super.getDefaultValue(typeAndLength, columnDefault);

        if (StringUtils.hasText(defaultValue)) {
            // 布尔值，自动转化：SQLServer BIT 用 1/0
            if (SqlServerTypeHelper.isBoolean(typeAndLength)) {
                if ("true".equalsIgnoreCase(defaultValue) || "1".equals(defaultValue)) {
                    defaultValue = "1";
                } else if ("false".equalsIgnoreCase(defaultValue) || "0".equals(defaultValue)) {
                    defaultValue = "0";
                }
            }
            // 兼容逻辑：如果是字符串的类型，自动包一层''（如果没有的话）
            if (SqlServerTypeHelper.isCharString(typeAndLength) && !defaultValue.startsWith("'") && !defaultValue.endsWith("'")) {
                defaultValue = "'" + defaultValue + "'";
            }
            // 兼容逻辑：如果是日期，且非函数，自动包一层''（如果没有的话）
            if (SqlServerTypeHelper.isTime(typeAndLength) && defaultValue.matches(StringUtils.DATETIME_REGEX) && !defaultValue.startsWith("'") && !defaultValue.endsWith("'")) {
                defaultValue = "'" + defaultValue + "'";
            }
        }
        return defaultValue;
    }
}
