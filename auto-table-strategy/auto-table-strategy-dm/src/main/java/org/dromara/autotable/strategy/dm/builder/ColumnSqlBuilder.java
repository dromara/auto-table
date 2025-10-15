package org.dromara.autotable.strategy.dm.builder;

import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.utils.StringConnectHelper;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.strategy.dm.data.DmDefaultTypeEnum;

import java.util.Arrays;

/**
 * 达梦列SQL构建器
 */
public class ColumnSqlBuilder {

    /**
     * 生成达梦字段定义SQL
     */
    public static String buildSql(ColumnMetadata columnMetadata) {
        StringConnectHelper sql = StringConnectHelper.newInstance("{columnName} {type} {null} {default} " +
                        "{autoIncrement}")
                .replace("{columnName}", IStrategy.wrapIdentifiers(columnMetadata.getName()))
                .replace("{type}", buildTypeDefinition(columnMetadata.getType()))
                .replace("{null}", columnMetadata.isNotNull() ? "NOT NULL" : "")
                .replace("{default}", buildDefaultValue(columnMetadata))
                .replace("{autoIncrement}", buildAutoIncrement(columnMetadata));

        return sql.toString().replaceAll("\\s+", " ").trim();
    }

    /**
     * 构建类型定义
     */
    private static String buildTypeDefinition(DatabaseTypeAndLength type) {
        String typeName = type.getType().toUpperCase();
        Integer length = type.getLength();
        Integer decimal = type.getDecimalLength();
        // 优先使用枚举中定义的默认值
        DmDefaultTypeEnum typeEnum = Arrays.stream(DmDefaultTypeEnum.values())
                .filter(e -> e.getTypeName().equalsIgnoreCase(typeName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid type: " + typeName));

        if ("NUMBER".equals(typeName)) {
            return handleNumberType(typeEnum, length, decimal);
        } else if ("VARCHAR2".equals(typeName)) {
            int actualLength = (length != null) ? Math.min(length, 8188) : 50;
            return String.format("VARCHAR2(%d)", actualLength);
        } else if ("CHAR".equals(typeName)) {
            return (length != null && length > 1) ? "CHAR(" + length + ")" : "CHAR";
        } else if ("INTEGER".equals(typeName)) {
            return "INTEGER";
        } else {
            return buildDefaultType(type, typeEnum, length, decimal);
        }
    }

    private static String handleNumberType(DmDefaultTypeEnum typeEnum,
                                           Integer length, Integer decimal) {
        int precision = length != null ? length : typeEnum.getDefaultLength();
        int scale = decimal != null ? decimal : typeEnum.getDefaultDecimalLength();
        return DmDefaultTypeEnum.convertNumberType(precision, scale);
    }

    private static String buildDefaultType(DatabaseTypeAndLength type, DmDefaultTypeEnum typeEnum,
                                           Integer length, Integer decimal) {
        if (typeEnum == DmDefaultTypeEnum.FLOAT || typeEnum == DmDefaultTypeEnum.DOUBLE) {
            int actualLength = (length != null) ? length : typeEnum.getDefaultLength();
            return String.format("%s(%d)", typeEnum.getTypeName(), actualLength);
        } else if (typeEnum == DmDefaultTypeEnum.DECIMAL) {
            int actualLength = (length != null) ? length : typeEnum.getDefaultLength();
            int actualDecimal = (decimal != null) ? decimal : typeEnum.getDefaultDecimalLength();
            return String.format("%s(%d,%d)", typeEnum.getTypeName(), actualLength, actualDecimal);
        } else {
            return type.getDefaultFullType();
        }
    }


    /**
     * 构建默认值子句
     */
    private static String buildDefaultValue(ColumnMetadata columnMetadata) {
        if (columnMetadata.isAutoIncrement()) {
            return "";
        }

        DefaultValueEnum defaultValueType = columnMetadata.getDefaultValueType();
        String defaultValue = columnMetadata.getDefaultValue();

        // 根据字段类型判断是否需要引号
        String typeName = columnMetadata.getType().getType().toUpperCase();
        boolean isNumberType = isNumberType(typeName);
        boolean isBooleanType = "TINYINT".equals(typeName);

        if (defaultValueType == DefaultValueEnum.NULL) {
            return "DEFAULT NULL";
        }

        if (defaultValueType == DefaultValueEnum.EMPTY_STRING) {
            return isNumberType ? "" : "DEFAULT ''";
        }

        if (DefaultValueEnum.isCustom(defaultValueType) && StringUtils.hasText(defaultValue)) {
            if (isFunctionDefault(defaultValue)) {
                return "DEFAULT " + defaultValue;
            }

            // 处理布尔类型
            if (isBooleanType) {
                return "DEFAULT " + convertBooleanValue(defaultValue);
            }

            // 处理数值类型
            if (isNumberType) {
                return "DEFAULT " + defaultValue;
            }

            // 字符串类型需要转义单引号
            // return "DEFAULT '" + defaultValue.replace("'", "''") + "'";
            return "DEFAULT " + defaultValue;
        }

        return "";
    }

    // 判断是否为数值类型
    private static boolean isNumberType(String typeName) {
        return typeName.matches("BIGINT|INT|INTEGER|SMALLINT|TINYINT|NUMBER|DECIMAL|FLOAT|DOUBLE");
    }

    // 转换布尔值
    private static String convertBooleanValue(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return "1";
        }
        if ("false".equalsIgnoreCase(value)) {
            return "0";
        }
        // 非标准布尔值保持原样
        return value;
    }


    /**
     * 构建自增子句
     */
    private static String buildAutoIncrement(ColumnMetadata columnMetadata) {
        if (!columnMetadata.isAutoIncrement()) {
            return "";
        }

        // 达梦的SERIAL类型已包含自增特性
        return "SERIAL".equalsIgnoreCase(columnMetadata.getType().getType()) ?
                "" : "IDENTITY(1,1)";
    }


    /**
     * 处理保留字列名
     */
    public static String wrapColumnName(String columnName) {
        return IStrategy.wrapIdentifiers(columnName);
    }

    /**
     * 判断是否为函数型默认值
     */
    private static boolean isFunctionDefault(String value) {
        return value.toUpperCase().matches("^(SYSDATE|CURRENT_TIMESTAMP|NEXTVAL\\()");
    }
}
