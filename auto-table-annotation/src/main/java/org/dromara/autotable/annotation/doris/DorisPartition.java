package org.dromara.autotable.annotation.doris;

import org.dromara.autotable.annotation.doris.emuns.DorisTimeUnit;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 分区配置
 * @author lizhian
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DorisPartition {
    /**
     * 分区名称
     */
    String partition() default "";

    /**
     * range分区values左闭区间
     */
    String[] values_left_include() default {};

    /**
     * range分区values右开区间
     */
    String[] values_right_exclude() default {};

    /**
     * range分区values分区上界
     */
    String[] values_less_than() default {};

    /**
     * range批量分区左闭区间
     */
    String from() default "";

    /**
     * range批量分区右开区间
     */
    String to() default "";

    /**
     * range批量分区步长
     */
    int interval() default -1;

    /**
     * range批量分区步长单位
     */
    DorisTimeUnit unit() default DorisTimeUnit.none;

    /**
     * list分区value,根据list key数量生成values in ((V1, V2,...), (Vn, Vm, ...), (...)...),
     */
    String[] values_in() default {};

}
