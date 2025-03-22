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
            "USER", "DATE", "LEVEL", "SERIAL", "FILE", "GROUP",
            "ORDER", "CHECK", "SESSION", "PARTITION", "CLUSTER", "LINK", "MERGE"
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

        switch (typeName) {
            case "NUMBER":
                // 当未指定精度/小数位时使用默认值
                int precision = length != null ? length : 38;
                int scale = decimal != null ? decimal : 4;

                // 调用智能转换方法
                return DmDefaultTypeEnum.convertNumberType(precision, scale);

            case "VARCHAR2":
                return length != null && length > 0 ?
                        "VARCHAR2(" + Math.min(length, 8188) + ")" : "VARCHAR2(8188)";

            case "CHAR":
                return length != null && length > 0 ?
                        "CHAR(" + length + ")" : "CHAR";

            case "FLOAT":
            case "DOUBLE":
                return length != null ?
                        String.format("%s(%d)", typeName, length) : typeName;

            default:
                return typeName;
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

        // 判断是否为SERIAL类型
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
