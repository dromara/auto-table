package org.dromara.autotable.core.constants;

/**
 * @author don
 */
public interface DatabaseDialect {

    // 常见主流数据库
    String MySQL = "MySQL";
    String MariaDB = "MariaDB";
    String PostgreSQL = "PostgreSQL";
    String KingBase = "KingbaseES";
    String DM = "DM DBMS";
    String SQLite = "SQLite";
    String H2 = "H2";
    String Oracle = "Oracle";
    String SQLServer = "SQLServer";
    String DB2 = "DB2";

    // 国产数据库
    String OceanBase = "OceanBase";
    String TiDB = "TiDB";
    String Dameng = "Dameng";       // 达梦数据库
    String Kingbase = "Kingbase";   // 人大金仓
    String GBase = "GBase";         // 南大通用
    String Shentong = "Shentong";   // 神通数据库
    String HuaweiGaussDB = "GaussDB"; // 华为 GaussDB

    // 分析型数据库
    String Doris = "Doris";
    String ClickHouse = "ClickHouse";
    String Redshift = "Redshift";
    String Vertica = "Vertica";
    String Teradata = "Teradata";

    // 其他流行数据库
    String Firebird = "Firebird";
    String Informix = "Informix";
    String Sybase = "Sybase";
}
