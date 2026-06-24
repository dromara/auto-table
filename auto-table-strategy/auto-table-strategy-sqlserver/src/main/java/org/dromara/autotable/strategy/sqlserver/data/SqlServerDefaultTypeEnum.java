package org.dromara.autotable.strategy.sqlserver.data;

import lombok.Getter;
import org.dromara.autotable.annotation.sqlserver.SqlServerTypeConstant;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;

/**
 * SQLServer 默认类型枚举。
 *
 * @author don
 */
@Getter
public enum SqlServerDefaultTypeEnum implements DefaultTypeEnumInterface {

    /**
     * 整数
     */
    BIT(SqlServerTypeConstant.BIT, null, null), // Boolean

    TINYINT(SqlServerTypeConstant.TINYINT, null, null), //

    SMALLINT(SqlServerTypeConstant.SMALLINT, null, null), // Short、Byte

    INT(SqlServerTypeConstant.INT, null, null), // Integer

    BIGINT(SqlServerTypeConstant.BIGINT, null, null), // Long、BigInteger

    /**
     * 浮点
     */
    REAL(SqlServerTypeConstant.REAL, null, null), // Float

    FLOAT(SqlServerTypeConstant.FLOAT, null, null), // Double

    DECIMAL(SqlServerTypeConstant.DECIMAL, 19, 4), // BigDecimal

    NUMERIC(SqlServerTypeConstant.NUMERIC, 19, 4), //

    /**
     * 字符串（默认 Unicode，避免中文乱码）
     */
    CHAR(SqlServerTypeConstant.NCHAR, 255, null), //

    VARCHAR(SqlServerTypeConstant.NVARCHAR, 255, null), // String

    TEXT(SqlServerTypeConstant.NTEXT, null, null), //

    /**
     * 日期时间
     */
    DATE(SqlServerTypeConstant.DATE, null, null), // LocalDate

    TIME(SqlServerTypeConstant.TIME, null, null), // LocalTime、java.sql.Time、OffsetTime

    DATETIME2(SqlServerTypeConstant.DATETIME2, null, null), // java.sql.Timestamp、Date、LocalDateTime

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

    SqlServerDefaultTypeEnum(String typeName, Integer defaultLength, Integer defaultDecimalLength) {
        this.typeName = typeName;
        this.defaultLength = defaultLength;
        this.defaultDecimalLength = defaultDecimalLength;
    }
}
