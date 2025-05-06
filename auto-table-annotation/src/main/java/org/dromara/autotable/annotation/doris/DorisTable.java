package org.dromara.autotable.annotation.doris;


import org.dromara.autotable.annotation.doris.emuns.DorisTimeUnit;

import java.lang.annotation.*;


/**
 * doris表配置
 *
 * @author lizhian
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DorisTable {

    /**
     * 索引配置
     */
    DorisIndex[] indexes() default {};

    /**
     * 表引擎,目前仅支持olap
     */
    String engine() default "olap";

    /**
     * 明细模型key
     * <a href="https://doris.apache.org/zh-CN/docs/table-design/data-model/duplicate">文档</a>
     */
    String[] duplicate_key() default {};

    /**
     * 主键模型key
     * <a href="https://doris.apache.org/zh-CN/docs/table-design/data-model/unique">文档</a>
     */
    String[] unique_key() default {};

    /**
     * 聚合模型key
     * <a href="https://doris.apache.org/zh-CN/docs/table-design/data-model/aggregate">文档</a>
     */
    String[] aggregate_key() default {};

    /**
     * 是否开启自动分区
     */
    boolean auto_partition() default false;

    /**
     * 自动分区时间单位
     * auto partition by range (date_trunc(col, '{auto_partition_time_unit}'))
     */
    DorisTimeUnit auto_partition_time_unit() default DorisTimeUnit.none;

    /**
     * 手动分区,range分区
     */
    String[] partition_by_range() default {};

    /**
     * 手动分区,list分区
     */
    String[] partition_by_list() default {};

    /**
     * 手动分区配置
     */
    DorisPartition[] partitions() default {};

    /**
     * 动态分区配置
     * <a href="https://doris.apache.org/zh-CN/docs/3.0/table-design/data-partitioning/dynamic-partitioning">文档</a>
     */
    DorisDynamicPartition dynamic_partition() default @DorisDynamicPartition(enable = false);

    /**
     * hash分桶算法使用的列
     * 非空时指定分桶算法为hash,否则指定分桶算法为random
     */
    String[] distributed_by_hash() default {};

    /**
     * 分桶数量,-1=auto
     */
    int distributed_buckets() default -1;

    /**
     * 物化视频配置
     */
    DorisRollup[] rollup() default {};

    /**
     * 建表properties
     */
    String[] properties() default {};


}
