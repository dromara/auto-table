package org.dromara.autotable.core.strategy.mysql.builder;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.strategy.mysql.data.MysqlColumnMetadata;
import org.dromara.autotable.core.strategy.mysql.data.MysqlTypeHelper;
import org.dromara.autotable.core.utils.StringConnectHelper;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author don
 */
@Slf4j
public class ColumnSqlBuilder {

    /**
     * 生成字段相关的SQL片段
     */
    public static String buildSql(MysqlColumnMetadata columnMetadata) {
        // 例子：`name` varchar(100) NULL DEFAULT '张三' COMMENT '名称'
        // 例子：`id` int(32) NOT NULL AUTO_INCREMENT COMMENT '主键'
        return StringConnectHelper.newInstance("`{columnName}` {typeAndLength} {qualifier} {character} {collate} {null} {default} {autoIncrement} {columnComment} {position}")
                .replace("{columnName}", columnMetadata.getName())
                .replace("{typeAndLength}", MysqlTypeHelper.getFullType(columnMetadata.getType()))
                // 添加二进制、无符号、补零 等修饰符
                .replace("{qualifier}", () -> {
                    Set<String> qualifiers = new HashSet<>();
                    if (columnMetadata.isUnsigned()) {
                        qualifiers.add("UNSIGNED");
                    }
                    if (columnMetadata.isZerofill()) {
                        qualifiers.add("UNSIGNED");
                        qualifiers.add("ZEROFILL");
                    }
                    return String.join(" ", qualifiers);
                })
                .replace("{character}", () -> {
                    String characterSet = columnMetadata.getCharacterSet();
                    if (StringUtils.hasText(characterSet)) {
                        return "CHARACTER SET " + characterSet;
                    }
                    return "";
                })
                .replace("{collate}", () -> {
                    String collate = columnMetadata.getCollate();
                    if (StringUtils.hasText(collate)) {
                        return "COLLATE " + collate;
                    }
                    return "";
                })
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
                .replace("{autoIncrement}", columnMetadata.isAutoIncrement() ? "AUTO_INCREMENT" : "")
                .replace("{columnComment}", StringUtils.hasText(columnMetadata.getComment()) ? "COMMENT '" + columnMetadata.getComment() + "'" : "")
                .replace("{position}", () -> {
                    if (StringUtils.hasText(columnMetadata.getNewPreColumn())) {
                        return "AFTER `" + columnMetadata.getNewPreColumn() + "`";
                    }
                    if ("".equals(columnMetadata.getNewPreColumn())) {
                        return "FIRST";
                    }
                    return "";
                })
                .toString();
    }
}
