package org.dromara.autotable.annotation.doris;

import org.dromara.autotable.annotation.doris.emuns.DorisTimeUnit;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 动态分区配置
 * <a href="https://doris.apache.org/zh-CN/docs/3.0/table-design/data-partitioning/dynamic-partitioning">文档</a>
 *
 * @author lizhian
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DorisDynamicPartition {
    boolean enable();

    DorisTimeUnit time_unit() default DorisTimeUnit.none;

    String start() default "";

    String end() default "";

    String prefix() default "";

    String buckets() default "";

    String replication_num() default "";

    String create_history_partition() default "";

    String start_day_of_week() default "";

    String start_day_of_month() default "";

    String reserved_history_periods() default "";

    String time_zone() default "";

}
