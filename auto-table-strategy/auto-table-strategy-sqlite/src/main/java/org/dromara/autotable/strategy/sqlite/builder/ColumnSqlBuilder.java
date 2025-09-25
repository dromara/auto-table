package org.dromara.autotable.strategy.sqlite.builder;

import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.strategy.sqlite.data.SqliteTypeHelper;
import org.dromara.autotable.core.utils.StringConnectHelper;
import org.dromara.autotable.core.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author don
 */
@Slf4j
public class ColumnSqlBuilder {

    /**
     * 生成字段相关的SQL片段
     * "id" INTEGER NOT NULL AUTOINCREMENT, -- 主键
     * "name" TEXT(200) NOT NULL DEFAULT '', -- 姓名
     * "age" INTEGER(2), -- 年龄
     * "address" TEXT(500) DEFAULT 济南市, -- 地址
     * "card_id" INTEGER(11) NOT NULL, -- 身份证id
     * "card_number" text(30) NOT NULL -- 身份证号码
     */
    public static String buildSql(ColumnMetadata columnMetadata, boolean isSinglePrimaryKey, boolean addComma) {
        // sqlite 只能主键自动递增
        boolean isAutoIncrement = isSinglePrimaryKey && columnMetadata.isPrimary() && columnMetadata.isAutoIncrement();
        return StringConnectHelper.newInstance("{columnName} {typeAndLength} {null} {default} {primaryKey}{comma}{columnComment}")
                .replace("{columnName}", columnMetadata.getName())
                .replace("{typeAndLength}", SqliteTypeHelper.getFullType(columnMetadata.getType(), isAutoIncrement))
                .replace("{null}", columnMetadata.isNotNull() ? "NOT NULL" : "NULL")
                .replace("{default}", () -> {
                    // 指定NULL
                    DefaultValueEnum defaultValueType = columnMetadata.getDefaultValueType();
                    if (defaultValueType == DefaultValueEnum.NULL) {
                        return "DEFAULT NULL";
                    }
                    // 指定空字符串
                    if (defaultValueType == DefaultValueEnum.EMPTY_STRING) {
                        return "DEFAULT ''";
                    }
                    // 自定义
                    String defaultValue = columnMetadata.getDefaultValue();
                    if (DefaultValueEnum.isCustom(defaultValueType) && StringUtils.hasText(defaultValue)) {
                        return "DEFAULT " + defaultValue;
                    }
                    return "";
                })
                .replace("{primaryKey}", () -> {
                    // sqlite特殊：只能是一个主键的情况下，才能设置自增，且只有主键才能自增
                    if (isAutoIncrement) {
                        return "PRIMARY KEY AUTOINCREMENT";
                    }
                    return "";
                })
                .replace("{comma}", addComma ? "," : "")
                .replace("{columnComment}", StringUtils.hasText(columnMetadata.getComment()) ? " -- " + columnMetadata.getComment() : "")
                .toString();
    }
}
