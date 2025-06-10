package org.dromara.autotable.strategy.doris.data;

import lombok.Getter;
import org.dromara.autotable.annotation.doris.DorisTypeConstant;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;

/**
 * 默认类型映射
 *
 * @author lizhian
 */
@Getter
public enum DorisDefaultTypeEnum implements DefaultTypeEnumInterface {
    /**
     * 布尔值
     */
    BOOLEAN(DorisTypeConstant.BOOLEAN, null, null),
    /**
     * 整数
     */
    TINYINT(DorisTypeConstant.TINYINT, null, null),
    SMALLINT(DorisTypeConstant.SMALLINT, null, null),
    INT(DorisTypeConstant.INT, null, null),
    BIGINT(DorisTypeConstant.BIGINT, null, null),
    LARGEINT(DorisTypeConstant.LARGEINT, null, null),
    /**
     * 小数
     */
    FLOAT(DorisTypeConstant.FLOAT, 6, 2),
    DOUBLE(DorisTypeConstant.DOUBLE, 10, 2),
    DECIMAL(DorisTypeConstant.DECIMAL, 12, 2),
    /**
     * 日期
     */
    DATE(DorisTypeConstant.DATE, null, null),
    DATETIME(DorisTypeConstant.DATETIME, null, null),
    /**
     * 字符串
     */
    CHAR(DorisTypeConstant.CHAR, 255, null),
    VARCHAR(DorisTypeConstant.VARCHAR, 255, null),
    STRING(DorisTypeConstant.STRING, null, null),
    /**
     * 半结构类型
     */
    ARRAY(DorisTypeConstant.ARRAY, null, null),
    MAP(DorisTypeConstant.MAP, null, null),
    STRUCT(DorisTypeConstant.STRUCT, null, null),
    JSON(DorisTypeConstant.JSON, null, null),
    VARIANT(DorisTypeConstant.VARIANT, null, null),
    /**
     * 聚合类型
     */
    HLL(DorisTypeConstant.HLL, null, null),
    BITMAP(DorisTypeConstant.BITMAP, null, null),
    QUANTILE_STATE(DorisTypeConstant.QUANTILE_STATE, null, null),
    AGG_STATE(DorisTypeConstant.AGG_STATE, null, null),
    /**
     * IP 类型
     */
    IPv4(DorisTypeConstant.IPv4, null, null),
    IPv6(DorisTypeConstant.IPv6, null, null),
    ;
    /**
     * 默认类型长度
     */
    private final Integer defaultLength;
    /**
     * 默认小数点后长度
     */
    private final Integer defaultDecimalLength;
    /**
     * 类型名称
     */
    private final String typeName;

    DorisDefaultTypeEnum(String typeName, Integer defaultLength, Integer defaultDecimalLength) {
        this.typeName = typeName;
        this.defaultLength = defaultLength;
        this.defaultDecimalLength = defaultDecimalLength;
    }
}
