package org.dromara.autotable.annotation.oracle;

public class OracleTypeConstant {

    // 字符类型
    /**
     * 固定长度字符串，最大2000字节
     */
    public static final String CHAR = "CHAR";
    /**
     * 可变长度字符串，最大4000字节
     */
    public static final String VARCHAR2 = "VARCHAR2";
    /**
     * Unicode字符，最大2000字节
     */
    public static final String NCHAR = "NCHAR";
    /**
     * 可变Unicode字符，最大4000字节
     */
    public static final String NVARCHAR2 = "NVARCHAR2";
    // 数值类型
    /**
     * 通用数值类型，p表示精度(1-38)，s表示小数位数(-84到127)
     */
    public static final String NUMBER = "NUMBER";

    // 日期时间类型
    /**
     * 含世纪、年、月、日、时、分、秒
     */
    public static final String DATE = "DATE";
    /**
     * 更精确的时间戳，p表示小数秒精度(0-9)
     */
    public static final String TIMESTAMP = "TIMESTAMP";
    /**
     * 带时区信息的时间戳
     */
    public static final String TIMESTAMP_WITH_TIME_ZONE = "TIMESTAMP WITH TIME ZONE";
    /**
     * 本地时区时间戳
     */
    public static final String TIMESTAMP_WITH_LOCAL_TIME_ZONE = "TIMESTAMP WITH LOCAL TIME ZONE";

    // 大对象类型
    /**
     * 二进制大对象，最大支持4GB
     */
    public static final String BLOB = "BLOB";
    /**
     * 字符大对象，支持单字节和多字节字符
     */
    public static final String CLOB = "CLOB";
    /**
     * Unicode字符大对象
     */
    public static final String NCLOB = "NCLOB";
    /**
     * 存储在外部文件中的二进制数据
     */
    public static final String BFILE = "BFILE";

    // 行标识类型
    /**
     * 物理行标识符，表示行的物理存储地址
     */
    public static final String ROWID = "ROWID";
    /**
     * 通用行标识符，用于索引组织表
     */
    public static final String UROWID = "UROWID";


    // 特殊数据类型
    /**
     * 可变长度二进制数据，最大2000字节
     */
    public static final String RAW = "RAW";

    // 复杂数据类型
    /**
     * 32位浮点数
     */
    public static final String BINARY_FLOAT = "BINARY_FLOAT";
    /**
     * 64位浮点数
     */
    public static final String BINARY_DOUBLE = "BINARY_DOUBLE";
}
