package org.dromara.autotable.strategy.sqlserver.builder;

import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.utils.StringConnectHelper;
import org.dromara.autotable.core.utils.StringUtils;

/**
 * 列相关的 SQL 生成器（SQLServer）。
 *
 * <p>列定义模板：{@code [columnName] typeAndLength [IDENTITY(1,1)] [NOT NULL] [DEFAULT ...]}</p>
 * <p>注意：SQLServer 自增列使用 {@code IDENTITY(1,1)}，且 IDENTITY 列自动非空，会忽略 NOT NULL/DEFAULT 配置。</p>
 *
 * @author don
 */
public class ColumnSqlBuilder {

    /**
     * 生成字段相关的 SQL 片段。
     *
     * @param columnMetadata 列元数据
     * @return 列相关的 sql
     */
    public static String buildSql(ColumnMetadata columnMetadata) {
        // 例子：[id] BIGINT IDENTITY(1,1) NOT NULL
        // 例子：[name] NVARCHAR(255) NULL DEFAULT '张三'
        StringConnectHelper sql = StringConnectHelper.newInstance("{columnName} {typeAndLength} {null} {default}")
                .replace("{columnName}", IStrategy.wrapIdentifiers(columnMetadata.getName()))
                .replace("{typeAndLength}", () -> columnMetadata.getType().getDefaultFullType());

        // 自增列：使用 IDENTITY(1,1)，自动非空，忽略 NOT NULL/DEFAULT 配置
        if (columnMetadata.isAutoIncrement()) {
            return sql.replace("{null} {default}", "IDENTITY(1,1) NOT NULL").toString();
        }

        return sql.replace("{null}", columnMetadata.isNotNull() ? "NOT NULL" : "NULL")
                .replace("{default}", () -> {
                    DefaultValueEnum defaultValueType = columnMetadata.getDefaultValueType();
                    // 指定 NULL
                    if (defaultValueType == DefaultValueEnum.NULL) {
                        return " DEFAULT NULL";
                    }
                    // 指定空字符串
                    if (defaultValueType == DefaultValueEnum.EMPTY_STRING) {
                        return " DEFAULT ''";
                    }
                    // 自定义
                    String defaultValue = columnMetadata.getDefaultValue();
                    if (DefaultValueEnum.isCustom(defaultValueType) && StringUtils.hasText(defaultValue)) {
                        return " DEFAULT " + defaultValue;
                    }
                    return "";
                })
                .toString()
                .trim();
    }
}
