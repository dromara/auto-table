package org.dromara.autotable.strategy.sqlite.builder;

import org.dromara.autotable.annotation.ColumnDefault;
import org.dromara.autotable.core.builder.ColumnMetadataBuilder;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.strategy.sqlite.data.SqliteTypeHelper;
import org.dromara.autotable.core.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.HashSet;

/**
 * 用于存放创建表的字段信息
 *
 * @author don
 */
@Slf4j
public class SqliteColumnMetadataBuilder extends ColumnMetadataBuilder {

    private final HashSet<String> defaultValueFuncs = new HashSet<>();

    {
        defaultValueFuncs.add("CURRENT_DATE");
        defaultValueFuncs.add("CURRENT_TIME");
        defaultValueFuncs.add("CURRENT_TIMESTAMP");
    }

    public SqliteColumnMetadataBuilder() {
        super(DatabaseDialect.SQLite);
    }

    @Override
    protected DatabaseTypeAndLength getTypeAndLength(String databaseDialect, Class<?> clazz, Field field) {
        DatabaseTypeAndLength typeAndLength = super.getTypeAndLength(databaseDialect, clazz, field);
        fixTypeAndLength(typeAndLength);
        return typeAndLength;
    }

    @Override
    protected String getDefaultValue(DatabaseTypeAndLength typeAndLength, ColumnDefault columnDefault) {

        String defaultValue = super.getDefaultValue(typeAndLength, columnDefault);

        if (StringUtils.hasText(defaultValue)) {
            // 补偿逻辑：类型为Boolean的时候(实际数据库为bit数字类型)，兼容 true、false
            boolean isBooleanType = SqliteTypeHelper.isInteger(typeAndLength) &&
                    ("true".equalsIgnoreCase(defaultValue) || "false".equalsIgnoreCase(defaultValue));
            if (isBooleanType) {
                if (Boolean.parseBoolean(defaultValue)) {
                    defaultValue = "1";
                } else {
                    defaultValue = "0";
                }
            }

            // 特殊函数，直接跳过
            if (defaultValueFuncs.contains(defaultValue)) {
                return defaultValue;
            }

            // 补偿逻辑：字符串类型，前后自动添加'
            if (SqliteTypeHelper.isText(typeAndLength) && !defaultValue.isEmpty() && !defaultValue.startsWith("'") && !defaultValue.endsWith("'")) {
                defaultValue = "'" + defaultValue + "'";
            }
        }
        return defaultValue;
    }

    private static void fixTypeAndLength(DatabaseTypeAndLength typeAndLength) {
        // 纠正类型的写法为正规方式
        String type = typeAndLength.getType().toLowerCase();
        if (type.contains("int")) {
            type = "integer";
        }
        if (type.contains("char") || type.contains("clob") || type.contains("text")) {
            type = "text";
        }
        if (type.contains("blob")) {
            type = "blob";
        }
        if (type.contains("real") || type.contains("floa") || type.contains("doub")) {
            type = "real";
        }
        typeAndLength.setType(type);
    }
}
