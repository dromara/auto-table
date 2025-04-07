package org.dromara.autotable.annotation.doris;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * rollup物化视图配置
 *
 * @author lizhian
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DorisRollup {
    /**
     * 名称
     */
    String name() default "";

    /**
     * 列,可使用fieldName
     */
    String[] columns();

    /**
     * properties
     */
    String[] properties() default {};

}
