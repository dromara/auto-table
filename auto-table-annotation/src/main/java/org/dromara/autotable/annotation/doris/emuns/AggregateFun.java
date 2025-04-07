package org.dromara.autotable.annotation.doris.emuns;

/**
 * value列的聚合类型
 *
 * @author lizhian
 */
public enum AggregateFun {

    /**
     * 无
     */
    none,
    /**
     * 求和。适用数值类型。
     */
    sum,
    /**
     * 求最小值。适合数值类型。
     */
    min,
    /**
     * 求最大值。适合数值类型。
     */
    max,
    /**
     * 替换。对于维度列相同的行，指标列会按照导入的先后顺序，后导入的替换先导入的。
     */
    replace,
    /**
     * 非空值替换。和 REPLACE 的区别在于对于 NULL 值，不做替换。这里要注意的是字段默认值要给 NULL，而不能是空字符串，如果是空字符串，会给你替换成空字符串。
     */
    replace_if_not_null,
    /**
     * HLL 类型的列的聚合方式，通过 HyperLogLog 算法聚合。
     */
    hll_union,
    /**
     * BIMTAP 类型的列的聚合方式，进行位图的并集聚合。
     */
    bitmap_union
}
