package org.dromara.autotable.core.strategy.dm.builder;

import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.ColumnMetadata;
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
            "USER", "DATE", "LEVEL", "SERIAL", "FILE", "GROUP", "ORDER", "CHECK"
    ));

    /**
     * 生成达梦字段定义SQL
     */
    public static String buildSql(ColumnMetadata columnMetadata) {
        StringConnectHelper sql = StringConnectHelper.newInstance("{columnName} {typeDefinition} {null} {default} " +
                        "{autoIncrement} {comment}")
                .replace("{columnName}", wrapColumnName(columnMetadata.getName()))
                .replace("{typeDefinition}", () -> buildTypeDefinition(columnMetadata.getType()))
                .replace("{null}", columnMetadata.isNotNull() ? "NOT NULL" : "NULL")
                .replace("{default}", buildDefaultValue(columnMetadata))
                .replace("{autoIncrement}", buildAutoIncrement(columnMetadata))
                .replace("{comment}", buildComment(columnMetadata));

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
                if (decimal != null) {
                    return String.format("NUMBER(%d,%d)", length != null ? length : 38, decimal);
                }
                return length != null ? "NUMBER(" + length + ")" : "NUMBER";

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
     * 构建注释子句
     */
    private static String buildComment(ColumnMetadata columnMetadata) {
        return StringUtils.hasText(columnMetadata.getComment()) ?
                "COMMENT '" + columnMetadata.getComment().replace("'", "''") + "'" : "";
    }

    /**
     * 处理保留字列名
     */
    private static String wrapColumnName(String columnName) {
        return "\"" + columnName + "\"";
    }

    /**
     * 判断是否为函数型默认值
     */
    private static boolean isFunctionDefault(String value) {
        return value.toUpperCase().matches("^(SYSDATE|CURRENT_TIMESTAMP|NEXTVAL\\()");
    }
}
