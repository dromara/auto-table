package org.dromara.autotable.core.strategy.dm.data;

import lombok.Getter;
import org.dromara.autotable.annotation.dm.DmTypeConstant;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;

/**
 * 达梦数据库字段类型枚举
 * @author freddy
 */
@Getter
public enum DmDefaultTypeEnum implements DefaultTypeEnumInterface {
    // 数值类型
    SERIAL(DmTypeConstant.SERIAL, null, null),
    INTEGER(DmTypeConstant.INTEGER, null, null),
    BIGINT(DmTypeConstant.BIGINT, null, null),
    TINYINT(DmTypeConstant.TINYINT, null, null),
    SMALLINT(DmTypeConstant.SMALLINT, null, null),
    DECIMAL(DmTypeConstant.DECIMAL, 10, 2),
    FLOAT(DmTypeConstant.FLOAT, 20, null),
    DOUBLE(DmTypeConstant.DOUBLE, 20, null),
    NUMBER(DmTypeConstant.NUMBER, null, null),

    // 字符类型
    CHAR(DmTypeConstant.CHAR, 1, null),
    VARCHAR(DmTypeConstant.VARCHAR, 50, null),
    VARCHAR2(DmTypeConstant.VARCHAR2, 255, null),
    CLOB(DmTypeConstant.CLOB, null, null),
    TEXT(DmTypeConstant.TEXT, null, null),

    // 日期时间
    DATE(DmTypeConstant.DATE, null, null),
    TIME(DmTypeConstant.TIME, null, null),
    DATETIME(DmTypeConstant.DATETIME, null, null),
    TIMESTAMP(DmTypeConstant.TIMESTAMP, null, null),

    // 二进制类型
    BLOB(DmTypeConstant.BLOB, null, null),
    BINARY(DmTypeConstant.BINARY, 1024, null),
    VARBINARY(DmTypeConstant.VARBINARY, 1024, null),
    IMAGE(DmTypeConstant.IMAGE, null, null),

    // 特殊类型
    BIT(DmTypeConstant.BIT, null, null),
    XML(DmTypeConstant.XML, null, null),
    FILE(DmTypeConstant.FILE, null, null),
    ARRAY(DmTypeConstant.ARRAY, null, null),
    OBJECT(DmTypeConstant.OBJECT, null, null);

    private final String typeName;
    private final Integer defaultLength;
    private final Integer defaultDecimalLength;

    DmDefaultTypeEnum(String typeName, Integer defaultLength, Integer defaultDecimalLength) {
        this.typeName = typeName;
        this.defaultLength = defaultLength;
        this.defaultDecimalLength = defaultDecimalLength;
    }

    /**
     * 数值类型智能转换
     */
    public static String convertNumberType(int precision, int scale) {
        if (scale == 0) {
            if (precision <= 4) {
                return DmTypeConstant.SMALLINT;
            }
            if (precision <= 9) {
                return DmTypeConstant.INTEGER;
            }
            if (precision <= 18) {
                return DmTypeConstant.BIGINT;
            }
        }
        // 达梦最大精度38位
        return String.format("%s(%d,%d)", DmTypeConstant.NUMBER,
                Math.min(precision, 38),
                Math.min(scale, 38));
    }
}