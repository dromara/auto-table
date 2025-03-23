package org.dromara.autotable.annotation.dm;

/**
 * 达梦数据库类型常量
 *
 * @author freddy
 */
public interface DmTypeConstant {
    // 数值类型
    String SERIAL = "SERIAL";
    String INTEGER = "INTEGER";
    String BIGINT = "BIGINT";
    String TINYINT = "TINYINT";
    String SMALLINT = "SMALLINT";
    String DECIMAL = "DECIMAL";
    String FLOAT = "FLOAT";
    String DOUBLE = "DOUBLE";
    String NUMBER = "NUMBER";

    // 字符类型
    String CHAR = "CHAR";
    String VARCHAR = "VARCHAR";
    String VARCHAR2 = "VARCHAR2";
    String CLOB = "CLOB";
    String TEXT = "TEXT";

    // 日期时间
    String DATE = "DATE";
    String TIME = "TIME";
    String DATETIME = "DATETIME";
    String TIMESTAMP = "TIMESTAMP";

    // 二进制类型
    String BLOB = "BLOB";
    String BINARY = "BINARY";
    String VARBINARY = "VARBINARY";
    String IMAGE = "IMAGE";

    // 特殊类型
    String BIT = "BIT";
    String XML = "XML";
    String FILE = "FILE";
    String ARRAY = "ARRAY";
    String OBJECT = "OBJECT";
}
