package org.dromara.autotable.annotation.sqlserver;

/**
 * SQLServer 数据库类型常量。
 * <p>类型名使用大写形式，符合 SQLServer DDL 习惯；比较时统一小写化处理。</p>
 *
 * @author don
 */
public interface SqlServerTypeConstant {

    String BIT = "BIT";                         // 位类型（布尔）
    String TINYINT = "TINYINT";                  // 微整数
    String SMALLINT = "SMALLINT";                // 小整数
    String INT = "INT";                          // 整数
    String BIGINT = "BIGINT";                    // 大整数

    String REAL = "REAL";                        // 单精度浮点数
    String FLOAT = "FLOAT";                      // 双精度浮点数
    String DECIMAL = "DECIMAL";                  // 精确小数类型
    String NUMERIC = "NUMERIC";                  // 数值类型

    String CHAR = "CHAR";                         // 定长字符类型
    String VARCHAR = "VARCHAR";                  // 可变长度字符类型（非 Unicode）
    String NCHAR = "NCHAR";                       // 定长 Unicode 字符类型
    String NVARCHAR = "NVARCHAR";                 // 可变长度 Unicode 字符类型
    String TEXT = "TEXT";                         // 大文本类型（非 Unicode）
    String NTEXT = "NTEXT";                       // 大文本类型（Unicode）

    String DATE = "DATE";                         // 日期类型
    String TIME = "TIME";                         // 时间类型
    String DATETIME = "DATETIME";                 // 日期时间类型
    String DATETIME2 = "DATETIME2";               // 高精度日期时间类型
    String SMALLDATETIME = "SMALLDATETIME";       // 小范围日期时间类型

    String BINARY = "BINARY";                     // 定长二进制类型
    String VARBINARY = "VARBINARY";               // 可变长度二进制类型
    String IMAGE = "IMAGE";                        // 大二进制类型
}
