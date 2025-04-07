package org.dromara.autotable.annotation;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 创建表时指定表名、schema、注释
 *
 * @author don
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoTable {

    /**
     * @return 表名，为空默认取类名
     */
    String value() default "";

    /**
     * @return 表schema
     */
    String schema() default "";

    /**
     * @return 表注释，为空默认取类名
     */
    String comment() default "";

    /**
     * @return 自定义方言策略
     */
    String strategy() default "";
}
