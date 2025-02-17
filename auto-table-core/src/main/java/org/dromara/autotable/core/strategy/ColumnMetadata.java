package org.dromara.autotable.core.strategy;

import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * 用于存放创建表的字段信息
 *
 * @author don
 */
@Slf4j
@Data
@Accessors(chain = true)
public class ColumnMetadata {

    /**
     * 列名: 不可变，变了意味着新列
     */
    protected String name;

    /**
     * 字段的备注
     */
    protected String comment;

    /**
     * 字段类型
     */
    protected DatabaseTypeAndLength type;

    /**
     * 字段是否非空
     */
    protected boolean notNull;

    /**
     * 字段是否是主键
     */
    protected boolean primary;

    /**
     * 是否自增
     */
    protected boolean autoIncrement;

    /**
     * <p>字段默认值类型</p>
     * <p>如果该值有值的情况下，将忽略 {@link #defaultValue} 的值</p>
     */
    protected DefaultValueEnum defaultValueType;

    /**
     * <p>字段默认值</p>
     * <p>如果 {@link #defaultValueType} 有值的情况下，将忽略本字段的指定</p>
     */
    protected String defaultValue;
}
