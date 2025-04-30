package org.dromara.autotable.core.strategy.doris.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.doris.DorisTypeHelper;
import org.dromara.autotable.core.utils.StringConnectHelper;
import org.dromara.autotable.core.utils.StringUtils;

/**
 * mysql有部分特殊注解，继承ColumnMetadata，拓展额外信息
 *
 * @author don
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class DorisColumnMetadata extends ColumnMetadata {
    private String fieldName;
    /**
     * doris没有主键概念,但是区分key列和value列
     * 字段是否是key
     */
    private boolean key;

    /**
     * 自增开始值
     * 默认为1
     */
    private Long autoIncrementStartValue;

    /**
     * 聚合类型
     * SUM：求和。适用数值类型。
     * MIN：求最小值。适合数值类型。
     * MAX：求最大值。适合数值类型。
     * REPLACE：替换。对于维度列相同的行，指标列会按照导入的先后顺序，后导入的替换先导入的。
     * REPLACE_IF_NOT_NULL：非空值替换。和 REPLACE 的区别在于对于 NULL 值，不做替换。这里要注意的是字段默认值要给 NULL，而不能是空字符串，如果是空字符串，会给你替换成空字符串。
     * HLL_UNION：HLL 类型的列的聚合方式，通过 HyperLogLog 算法聚合。
     * BITMAP_UNION：BIMTAP 类型的列的聚合方式，进行位图的并集聚合。
     * <a href="https://doris.apache.org/zh-CN/docs/3.0/table-design/best-practice?_highlight=replace_if_not_null#02-aggregate-key-%E8%A1%A8%E6%A8%A1%E5%9E%8B">...</a>
     */
    private String aggregateFun = "";

    /**
     * 是否在该行有列更新时将该列的值更新为当前时间 (`current_timestamp`)。该特性只能在开启了 Merge-on-Write 的 Unique 表上使用，开启了这个特性的列必须声明默认值，且默认值必须为`current_timestamp`。如果此处声明了时间戳的精度，则该列默认值中的时间戳精度必须与该处的时间戳精度相同。
     */
    private boolean onUpdateCurrentTimestamp;

    /**
     * 当前字段的顺序位置，按照实体字段自上而下排列的，父类的字段整体排在子类之后
     */
    private int position;

    /**
     * <p>表示前一列的列名，该值的使用规则如下:
     * <p>if 非空，生成“AFTER [newPreColumn]”，表示位于某列之后；
     * <p>else if 空字符，生成“FIRST”，表示第一列；
     * <p>else 生成空字符串，表示没有变动；
     */
    private String newPreColumn;

    public DorisColumnMetadata(ColumnMetadata delegation) {
        this.setName(delegation.getName())
                .setComment(delegation.getComment())
                .setType(delegation.getType())
                .setNotNull(delegation.isNotNull())
                .setPrimary(delegation.isPrimary())
                .setAutoIncrement(delegation.isAutoIncrement())
                .setDefaultValueType(delegation.getDefaultValueType())
                .setDefaultValue(delegation.getDefaultValue())

        ;
    }

    public String toSql() {
        return StringConnectHelper.newInstance("`{column_name}` {column_type} {aggregate_fun} {null} {auto_increment(auto_auto_increment_start_value)} {default_value} {on update current_timestamp} {column_comment}")
                .replace("{column_name}", name)
                .replace("{column_type}", DorisTypeHelper.getFullType(type))
                .replace("{aggregate_fun}", aggregateFun)
                .replace("{null}", notNull ? "not null" : "null")
                .replace("{auto_increment(auto_auto_increment_start_value)}", () -> {
                    if (!this.autoIncrement) {
                        return "";
                    }
                    if (this.autoIncrementStartValue == null || this.autoIncrementStartValue <= 0) {
                        return "auto_increment";
                    }
                    return "auto_increment(" + autoIncrementStartValue + ")";
                })
                .replace("{default_value}", () -> {
                    // 指定NULL
                    if (DefaultValueEnum.NULL.equals(defaultValueType)) {
                        return "default null";
                    }
                    // 指定空字符串
                    if (DefaultValueEnum.EMPTY_STRING.equals(defaultValueType)) {
                        return "default \"\"";
                    }

                    // 自定义
                    if (DefaultValueEnum.isCustom(defaultValueType) && StringUtils.hasText(defaultValue)) {
                        if ("current_timestamp".equalsIgnoreCase(defaultValue)) {
                            return "default current_timestamp";
                        }
                        return "default \"" + defaultValue + "\"";
                    }
                    return "";
                })
                .replace("{on update current_timestamp}", () -> {
                    if (!onUpdateCurrentTimestamp) {
                        return "";
                    }
                    Integer length = type.getLength();
                    if (length == null) {
                        return "on update current_timestamp";
                    } else {
                        return "on update current_timestamp(" + length + ")";
                    }
                })
                .replace("{column_comment}", StringUtils.hasText(comment) ? "comment \"" + comment + "\"" : "")
                .replace("{position}", () -> {
                    if (StringUtils.hasText(newPreColumn)) {
                        return "after `" + newPreColumn + "`";
                    }
                    if ("".equals(newPreColumn)) {
                        return "first";
                    }
                    return "";
                })
                .toString()
                .trim();
    }
}
