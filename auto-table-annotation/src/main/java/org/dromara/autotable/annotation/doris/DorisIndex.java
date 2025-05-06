package org.dromara.autotable.annotation.doris;

import org.dromara.autotable.annotation.doris.emuns.DorisIndexType;

import java.lang.annotation.*;

/**
 * doris索引配置
 *
 * @author lizhian
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DorisIndex {

    /**
     * 索引名
     */
    String name() default "";

    /**
     * 索引列,可使用fieldName
     */
    String column();

    /**
     * 索引类型
     */
    DorisIndexType using() default DorisIndexType.inverted;

    /**
     * 索引properties
     */
    String[] properties() default {};

    /**
     * @return 索引注释: 默认空字符串
     */
    String comment() default "";

}

