package org.dromara.autotable.annotation.pgsql;

public interface PgsqlTypeConstant {

    String INT2 = "int2";           // 小整数 (16位)
    String INT4 = "int4";           // 整数 (32位)
    String INT8 = "int8";           // 大整数 (64位)
    String BOOL = "bool";           // 布尔类型
    String FLOAT4 = "float4";       // 单精度浮点数
    String FLOAT8 = "float8";       // 双精度浮点数
    String MONEY = "money";         // 货币类型
    String NUMERIC = "numeric";     // 精确小数类型
    String BYTEA = "bytea";         // 二进制数据类型
    String CHAR = "char";           // 定长字符类型
    String VARCHAR = "varchar";     // 可变长度字符类型
    String TEXT = "text";           // 大文本类型
    String TIME = "time";           // 时间类型 (无时区)
    String TIMETZ = "timetz";       // 时间类型 (带时区)
    String DATE = "date";           // 日期类型
    String TIMESTAMP = "timestamp"; // 时间戳 (无时区)
    String TIMESTAMPTZ = "timestamptz"; // 时间戳 (带时区)
    String BIT = "bit";             // 定长位串类型
    String VARBIT = "varbit";       // 可变长度位串类型
    String JSON = "json";           // JSON 数据类型
    String JSONB = "jsonb";         // 二进制 JSON 数据类型
    String XML = "xml";             // XML 数据类型
    String INTERVAL = "interval";   // 时间间隔类型
    String CIDR = "cidr";           // IPv4 或 IPv6 网络类型
    String INET = "inet";           // IPv4 或 IPv6 地址类型
    String MACADDR = "macaddr";     // MAC 地址类型
    String TSQUERY = "tsquery";     // 全文检索查询类型
    String TSVECTOR = "tsvector";   // 全文检索向量类型
}
