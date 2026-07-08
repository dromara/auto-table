package org.dromara.autotable.adapter.mybatisplus.spring.annotation;

import org.dromara.autotable.annotation.Index;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 设置字段唯一索引（组合注解）。
 * <p>
 * {@link Index} 的快捷方式，预设为 {@link IndexTypeEnum#UNIQUE}。
 *
 * @author don
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Index(type = IndexTypeEnum.UNIQUE)
public @interface UniqueIndex {

    /**
     * 索引的名字，不设置默认为 {mpe_idx_当前标记字段名}。
     * 如果设置了名字例如 union_name，系统会默认在名字前加 mpe_idx_ 前缀，
     * 也就是 mpe_idx_union_name。
     */
    @AliasFor(annotation = Index.class, attribute = "name")
    String name() default "";

    /**
     * 索引注释
     */
    @AliasFor(annotation = Index.class, attribute = "comment")
    String comment() default "";
}
