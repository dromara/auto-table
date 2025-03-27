package org.dromara.autotable.core.strategy.dm.builder;

import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.dm.data.DmDefaultTypeEnum;
import org.dromara.autotable.core.utils.StringConnectHelper;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 达梦列SQL构建器
 */
public class ColumnSqlBuilder {
    // 达梦保留字集合（部分示例）
    private static final Set<String> RESERVED_WORDS = new HashSet<>(Arrays.asList(
            "USER", "DATE", "LEVEL", "SERIAL", "FILE", "GROUP", "ORDER",
            "CHECK", "SESSION", "PARTITION", "CLUSTER", "LINK", "MERGE",
            "PASSWORD", "YEAR", "MONTH", "DAY", "HOUR"
    ));

    /**
     * 生成达梦字段定义SQL
     */
    public static String buildSql(ColumnMetadata columnMetadata) {
        StringConnectHelper sql = StringConnectHelper.newInstance("{columnName} {type} {null} {default} " +
                        "{autoIncrement}")
                .replace("{columnName}", wrapColumnName(columnMetadata.getName()))
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
        } else {
            return buildDefaultType(typeEnum, length, decimal);
        }
    }

    private static String handleNumberType(DmDefaultTypeEnum typeEnum,
                                           Integer length, Integer decimal) {
        int precision = length != null ? length : typeEnum.getDefaultLength();
        int scale = decimal != null ? decimal : typeEnum.getDefaultDecimalLength();
        return DmDefaultTypeEnum.convertNumberType(precision, scale);
    }

    private static String buildDefaultType(DmDefaultTypeEnum typeEnum,
                                           Integer length, Integer decimal) {
        if (typeEnum == DmDefaultTypeEnum.FLOAT || typeEnum == DmDefaultTypeEnum.DOUBLE) {
            int actualLength = (length != null) ? length : typeEnum.getDefaultLength();
            return String.format("%s(%d)", typeEnum.getTypeName(), actualLength);
        } else if (typeEnum == DmDefaultTypeEnum.DECIMAL) {
            int actualLength = (length != null) ? length : typeEnum.getDefaultLength();
            int actualDecimal = (decimal != null) ? decimal : typeEnum.getDefaultDecimalLength();
            return String.format("%s(%d,%d)", typeEnum.getTypeName(), actualLength, actualDecimal);
        } else {
            return typeEnum.getTypeName();
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

        if (defaultValueType == DefaultValueEnum.NULL) {
            return "DEFAULT NULL";
        }

        if (defaultValueType == DefaultValueEnum.EMPTY_STRING) {
            return "DEFAULT ''";
        }

        if (DefaultValueEnum.isCustom(defaultValueType) && StringUtils.hasText(defaultValue)) {
            return isFunctionDefault(defaultValue) ?
                    "DEFAULT " + defaultValue :
                    "DEFAULT '" + defaultValue + "'";
        }

        return "";
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
    private static String wrapColumnName(String columnName) {
        // 统一转为大写判断（达梦保留字不区分大小写）
        String upperName = columnName.toUpperCase();

        // 仅对保留字添加双引号
        if (RESERVED_WORDS.contains(upperName)) {
            return "\"" + columnName + "\"";
        }

        // 普通列名保持原样
        return columnName;
    }

    /**
     * 判断是否为函数型默认值
     */
    private static boolean isFunctionDefault(String value) {
        return value.toUpperCase().matches("^(SYSDATE|CURRENT_TIMESTAMP|NEXTVAL\\()");
    }
}
