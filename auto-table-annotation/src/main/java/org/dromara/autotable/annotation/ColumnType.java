package org.dromara.autotable.annotation;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段的类型
 * @author don
 */
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ColumnType {

    /**
     * @return 字段的类型
     */
    String value() default "";

    /**
     * @return 字段长度，默认是-1，小于0相当于null
     */
    int length() default -1;

    /**
     * @return 小数点长度，默认是-1，小于0相当于null
     */
    int decimalLength() default -1;

    /**
     * 目前仅支持MySQL的enum和set类型
     * 如果字段是java的Enum类型，那么values可以不指定，默认取枚举的所有值
     * @return 枚举值
     */
    String[] values() default {};
}
