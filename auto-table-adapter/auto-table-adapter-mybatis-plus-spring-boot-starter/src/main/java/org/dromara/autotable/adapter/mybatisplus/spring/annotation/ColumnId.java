package org.dromara.autotable.adapter.mybatisplus.spring.annotation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import org.dromara.autotable.annotation.ColumnComment;
import org.dromara.autotable.annotation.ColumnType;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标志该字段为主键（组合注解）。
 * <p>
 * 组合了 MP 的 {@link TableId} 以及 AutoTable 的 {@link ColumnType}、{@link ColumnComment}，
 * 通过 {@link AliasFor} 一个注解完成主键名、类型、字段类型等配置。
 *
 * @author don
 */
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TableId
@ColumnType
@ColumnComment("")
public @interface ColumnId {

    /**
     * 字段名：不填默认使用属性名作为表字段名
     */
    @AliasFor(annotation = TableId.class, attribute = "value")
    String value() default "";

    /**
     * 主键类型
     */
    @AliasFor(annotation = TableId.class, attribute = "type")
    IdType mode() default IdType.NONE;

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
     * 数据表字段备注
     */
    @AliasFor(annotation = ColumnComment.class, attribute = "value")
    String comment() default "";
}
