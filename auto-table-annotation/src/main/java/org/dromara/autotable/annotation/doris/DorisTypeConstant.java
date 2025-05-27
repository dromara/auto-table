package org.dromara.autotable.annotation.doris;
/**
 * doris类型常量
 */
public interface DorisTypeConstant {
    /**
     * 布尔值
     */
    String BOOLEAN = "boolean";
    /**
     * 整数
     */
    String TINYINT = "tinyint";
    String SMALLINT = "smallint";
    String INT = "int";
    String BIGINT = "bigint";
    String LARGEINT = "largeint";
    /**
     * 小数
     */
    String FLOAT = "float";
    String DOUBLE = "double";
    String DECIMAL = "decimal";
    /**
     * 日期
     */
    String DATE = "date";
    String DATETIME = "datetime";
    /**
     * 字符串
     */
    String CHAR = "char";
    String VARCHAR = "varchar";
    String STRING = "string";
    /**
     * 半结构类型
     */
    String ARRAY = "array";
    String MAP = "map";
    String STRUCT = "struct";
    String JSON = "json";
    String VARIANT = "variant";
    /**
     * 聚合类型
     */
    String HLL = "hll";
    String BITMAP = "bitmap";
    String QUANTILE_STATE = "quantile_state";
    String AGG_STATE = "agg_state";
    /**
     * IP 类型
     */
    String IPv4 = "ipv4";
    String IPv6 = "ipv6";
}
