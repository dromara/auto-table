package org.dromara.autotable.core.strategy.oracle.data;

import lombok.Getter;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;

/**
 * 用于配置Oracle数据库中类型，并且该类型需要设置几个长度
 * 这里配置多少个类型决定了，创建表能使用多少类型
 * 例如：VARCHAR2(1)
 * NUMBER(5,2)
 * DATE
 *
 * @author don
 */
@Getter
public enum OracleDefaultTypeEnum implements DefaultTypeEnumInterface {

    /**
     * 数值类型
     */
    NUMBER("NUMBER", 10, 2),
    FLOAT("FLOAT", 38, null),
    BINARY_FLOAT("BINARY_FLOAT", null, null),
    BINARY_DOUBLE("BINARY_DOUBLE", null, null),

    /**
     * 字符串类型
     */
    CHAR("CHAR", 255, null),
    VARCHAR2("VARCHAR2", 255, null),
    NCHAR("NCHAR", 255, null),
    NVARCHAR2("NVARCHAR2", 255, null),
    CLOB("CLOB", null, null),
    NCLOB("NCLOB", null, null),
    LONG("LONG", null, null),
    BOOLEAN("CHAR", 1, null),

    /**
     * 日期时间类型
     */
    DATE("DATE", null, null),
    TIMESTAMP("TIMESTAMP", null, null),
    TIMESTAMP_WITH_TIME_ZONE("TIMESTAMP WITH TIME ZONE", null, null),
    TIMESTAMP_WITH_LOCAL_TIME_ZONE("TIMESTAMP WITH LOCAL TIME ZONE", null, null),
    INTERVAL_YEAR_TO_MONTH("INTERVAL YEAR TO MONTH", null, null),
    INTERVAL_DAY_TO_SECOND("INTERVAL DAY TO SECOND", null, null),

    /**
     * 二进制类型
     */
    BLOB("BLOB", null, null),
    BFILE("BFILE", null, null),
    RAW("RAW", 2000, null),
    LONG_RAW("LONG RAW", null, null),

    /**
     * 其他类型
     */
    ROWID("ROWID", null, null),
    UROWID("UROWID", null, null);

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

    OracleDefaultTypeEnum(String typeName, Integer defaultLength, Integer defaultDecimalLength) {
        this.typeName = typeName;
        this.defaultLength = defaultLength;
        this.defaultDecimalLength = defaultDecimalLength;
    }
}
