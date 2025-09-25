package org.dromara.autotable.strategy.h2.data;

import org.dromara.autotable.annotation.h2.H2TypeConstant;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
import lombok.Getter;

/**
 * 用于配置h2数据库中类型，并且该类型需要设置几个长度
 * 这里配置多少个类型决定了，创建表能使用多少类型
 * 例如：varchar(1)
 * decimal(5,2)
 * datetime
 *
 * @author don
 */
@Getter
public enum H2DefaultTypeEnum implements DefaultTypeEnumInterface {

    /**
     * 整数
     */
    INTEGER(H2TypeConstant.INTEGER, null, null),
    TINYINT(H2TypeConstant.TINYINT, null, null),
    SMALLINT(H2TypeConstant.SMALLINT, null, null),
    BIGINT(H2TypeConstant.BIGINT, null, null),
    /**
     * 小数
     */
    REAL(H2TypeConstant.REAL, null, null),
    NUMERIC(H2TypeConstant.NUMERIC, 10, 4),
    /**
     * 字符串
     */
    CHARACTER(H2TypeConstant.CHARACTER, 125, null),
    VARCHAR_IGNORECASE(H2TypeConstant.VARCHAR_IGNORECASE, 255, null),
    CHARACTER_VARYING(H2TypeConstant.CHARACTER_VARYING, 255, null),
    CHARACTER_LARGE_OBJECT(H2TypeConstant.CHARACTER_LARGE_OBJECT, null, null),
    /**
     * 日期
     */
    TIME(H2TypeConstant.TIME, null, null),
    DATE(H2TypeConstant.DATE, null, null),
    TIMESTAMP(H2TypeConstant.TIMESTAMP, null, null),
    /**
     * 二进制
     */
    BINARY(H2TypeConstant.BINARY, 1, null),
    BLOB(H2TypeConstant.BLOB, null, null),
    /**
     * 布尔
     */
    BOOLEAN(H2TypeConstant.BOOLEAN, null, null);

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

    H2DefaultTypeEnum(String typeName, Integer defaultLength, Integer defaultDecimalLength) {
        this.typeName = typeName;
        this.defaultLength = defaultLength;
        this.defaultDecimalLength = defaultDecimalLength;
    }
}
