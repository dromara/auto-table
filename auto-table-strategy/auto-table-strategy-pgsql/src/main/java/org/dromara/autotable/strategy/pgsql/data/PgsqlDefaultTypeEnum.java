package org.dromara.autotable.strategy.pgsql.data;

import org.dromara.autotable.annotation.pgsql.PgsqlTypeConstant;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
import lombok.Getter;

/**
 * @author don
 */
@Getter
public enum PgsqlDefaultTypeEnum implements DefaultTypeEnumInterface {
    /**
     * 整数
     */
    INT2(PgsqlTypeConstant.INT2, null, null), //

    INT4(PgsqlTypeConstant.INT4, null, null), // int

    INT8(PgsqlTypeConstant.INT8, null, null), // long

    /**
     * 布尔
     */
    BOOL(PgsqlTypeConstant.BOOL, null, null), // Boolean

    /**
     * 小数
     */
    FLOAT4(PgsqlTypeConstant.FLOAT4, null, null), // float

    FLOAT8(PgsqlTypeConstant.FLOAT8, null, null), // double

    MONEY(PgsqlTypeConstant.MONEY, null, null), //

    NUMERIC(PgsqlTypeConstant.NUMERIC, 10, 6), // BigDecimal

    /**
     * 字节
     */
    BYTEA(PgsqlTypeConstant.BYTEA, null, null), // byte

    /**
     * 字符串
     */
    CHAR(PgsqlTypeConstant.CHAR, 255, null), //

    VARCHAR(PgsqlTypeConstant.VARCHAR, 255, null), // String

    TEXT(PgsqlTypeConstant.TEXT, null, null), //

    /**
     * 日期
     */
    TIME(PgsqlTypeConstant.TIME, null, null), // java.sql.Time、LocalTime
    TIMETZ(PgsqlTypeConstant.TIMETZ, null, null), // java.sql.Time、LocalTime

    DATE(PgsqlTypeConstant.DATE, null, null), // LocalDate

    TIMESTAMP(PgsqlTypeConstant.TIMESTAMP, null, null), // java.sql.Timestamp、Date、LocalDateTime

    /**
     * 二进制
     */
    BIT(PgsqlTypeConstant.BIT, 1, null), //

    ;

    /**
     * 类型名称
     */
    private final String typeName;
    /**
     * 默认类型长度
     */
    private final Integer defaultLength;
    /**
     * 默认小数点后长度
     */
    private final Integer defaultDecimalLength;

    PgsqlDefaultTypeEnum(String typeName, Integer defaultLength, Integer defaultDecimalLength) {
        this.typeName = typeName;
        this.defaultLength = defaultLength;
        this.defaultDecimalLength = defaultDecimalLength;
    }
}
