package org.dromara.autotable.adapter.mybatisplus.spring.annotation;

import com.baomidou.mybatisplus.annotation.TableField;
import org.dromara.autotable.annotation.ColumnComment;
import org.dromara.autotable.annotation.ColumnDefault;
import org.dromara.autotable.annotation.ColumnNotNull;
import org.dromara.autotable.annotation.ColumnType;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 建表的必备注解（组合注解）。
 * <p>
 * 组合了 MP 的 {@link TableField} 以及 AutoTable 的多个注解，
 * 通过 {@link AliasFor} 一个注解完成字段名、类型、长度、默认值、注释等配置。
 *
 * @author don
 */
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TableField
@ColumnType
@ColumnNotNull
@ColumnDefault
@ColumnComment("")
public @interface Column {

    /**
     * 字段名：不填默认使用属性名作为表字段名
     */
    @AliasFor(annotation = TableField.class, attribute = "value")
    String value() default "";

    /**
     * 字段类型：不填默认使用属性的数据类型进行转换，转换失败的字段不会添加
     */
    @AliasFor(annotation = ColumnType.class, attribute = "value")
    String type() default "";

    /**
     * 字段长度，默认是 -1，小于 0 相当于 null
     */
    @AliasFor(annotation = ColumnType.class, attribute = "length")
    int length() default -1;

    /**
     * 小数点长度，默认是 -1，小于 0 相当于 null
     */
    @AliasFor(annotation = ColumnType.class, attribute = "decimalLength")
    int decimalLength() default -1;

    /**
     * 是否为可以为 null，true 是不可以，false 是可以，默认为 false
     */
    @AliasFor(annotation = ColumnNotNull.class, attribute = "value")
    boolean notNull() default false;

    /**
     * 默认值类型
     */
    @AliasFor(annotation = ColumnDefault.class, attribute = "type")
    DefaultValueEnum defaultValueType() default DefaultValueEnum.UNDEFINED;

    /**
     * 默认值，默认为 null
     */
    @AliasFor(annotation = ColumnDefault.class, attribute = "value")
    String defaultValue() default "";

    /**
     * 数据表字段备注
     */
    @AliasFor(annotation = ColumnComment.class, attribute = "value")
    String comment() default "";
}
