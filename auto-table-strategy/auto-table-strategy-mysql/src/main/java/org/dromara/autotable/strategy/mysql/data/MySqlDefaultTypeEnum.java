package org.dromara.autotable.strategy.mysql.data;

import lombok.Getter;
import org.dromara.autotable.annotation.mysql.MysqlTypeConstant;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;

/**
 * 用于配置Mysql数据库中类型，并且该类型需要设置几个长度
 * 这里配置多少个类型决定了，创建表能使用多少类型
 * 例如：varchar(1)
 * decimal(5,2)
 * datetime
 *
 * @author don
 */
@Getter
public enum MySqlDefaultTypeEnum implements DefaultTypeEnumInterface {

    /**
     * 整数，从8.0.17版本开始,TINYINT,SMALLINT,MEDIUMINT,INT,andBIGINT类型的显示宽度将失效
     */
    INT(MysqlTypeConstant.INT, null, null),
    TINYINT(MysqlTypeConstant.TINYINT, null, null),
    SMALLINT(MysqlTypeConstant.SMALLINT, null, null),
    MEDIUMINT(MysqlTypeConstant.MEDIUMINT, null, null),
    BIGINT(MysqlTypeConstant.BIGINT, null, null),
    /**
     * 小数
     */
    FLOAT(MysqlTypeConstant.FLOAT, 4, 2),
    DOUBLE(MysqlTypeConstant.DOUBLE, 6, 2),
    DECIMAL(MysqlTypeConstant.DECIMAL, 10, 4),
    /**
     * 字符串
     */
    CHAR(MysqlTypeConstant.CHAR, 255, null),
    VARCHAR(MysqlTypeConstant.VARCHAR, 255, null),
    TEXT(MysqlTypeConstant.TEXT, null, null),
    TINYTEXT(MysqlTypeConstant.TINYTEXT, null, null),
    MEDIUMTEXT(MysqlTypeConstant.MEDIUMTEXT, null, null),
    LONGTEXT(MysqlTypeConstant.LONGTEXT, null, null),
    /**
     * 枚举
     */
    ENUM(MysqlTypeConstant.ENUM, null, null),
    SET(MysqlTypeConstant.SET, null, null),
    /**
     * 日期
     */
    YEAR(MysqlTypeConstant.YEAR, null, null),
    TIME(MysqlTypeConstant.TIME, null, null),
    DATE(MysqlTypeConstant.DATE, null, null),
    DATETIME(MysqlTypeConstant.DATETIME, null, null),
    TIMESTAMP(MysqlTypeConstant.TIMESTAMP, null, null),
    /**
     * 二进制
     */
    BIT(MysqlTypeConstant.BIT, 1, null),
    BINARY(MysqlTypeConstant.BINARY, 1, null),
    VARBINARY(MysqlTypeConstant.VARBINARY, 1, null),
    BLOB(MysqlTypeConstant.BLOB, null, null),
    TINYBLOB(MysqlTypeConstant.TINYBLOB, null, null),
    MEDIUMBLOB(MysqlTypeConstant.MEDIUMBLOB, null, null),
    LONGBLOB(MysqlTypeConstant.LONGBLOB, null, null),
    /**
     * json
     */
    JSON(MysqlTypeConstant.JSON, null, null);

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

    MySqlDefaultTypeEnum(String typeName, Integer defaultLength, Integer defaultDecimalLength) {
        this.typeName = typeName;
        this.defaultLength = defaultLength;
        this.defaultDecimalLength = defaultDecimalLength;
    }
}
