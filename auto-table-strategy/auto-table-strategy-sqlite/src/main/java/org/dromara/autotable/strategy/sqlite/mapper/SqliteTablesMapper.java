package org.dromara.autotable.strategy.sqlite.mapper;

import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.strategy.sqlite.data.dbdata.SqliteColumns;
import org.dromara.autotable.strategy.sqlite.data.dbdata.SqliteMaster;
import org.dromara.autotable.core.utils.DBHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 创建更新表结构的Mapper
 * @author don
 */
public class SqliteTablesMapper {

    private static Map<String, Object> params(String tableName) {
        Map<String, Object> map = new HashMap<>(4);
        map.put("tableName", tableName);
        return map;
    }

    /**
     * 查询建表语句
     *
     * @param tableName 表名
     * @return 建表语句
     */
    // @Select("SELECT `sql` FROM sqlite_master WHERE type='table' AND name=#{tableName};")
    public String queryBuildTableSql(String tableName) {
        return DataSourceManager.useConnection(connection -> {
            String sql = "SELECT `sql` FROM sqlite_master WHERE type='table' AND name=':tableName';";
            return DBHelper.queryValue(connection, sql, params(tableName));
        });
    }

    /**
     * 查询建表语句
     *
     * @param tableName 表名
     * @return 建表语句
     */
    // @Results({
    //         @Result(column = "type", property = "type"),
    //         @Result(column = "name", property = "name"),
    //         @Result(column = "tbl_name", property = "tblName"),
    //         @Result(column = "rootpage", property = "rootpage"),
    //         @Result(column = "sql", property = "sql"),
    // })
    // @Select("SELECT * FROM sqlite_master WHERE type='index' AND tbl_name=#{tableName};")
    public List<SqliteMaster> queryBuildIndexSql(String tableName) {
        return DataSourceManager.useConnection(connection -> {
            String sql = "SELECT * FROM sqlite_master WHERE type='index' AND tbl_name=':tableName';";
            return DBHelper.queryObjectList(connection, sql, params(tableName), SqliteMaster.class);
        });
    }

    /**
     * 查询建表语句
     *
     * @param tableName 表名
     * @return 建表语句
     */
    // @Results({
    //         @Result(column = "cid", property = "cid"),
    //         @Result(column = "name", property = "name"),
    //         @Result(column = "type", property = "type"),
    //         @Result(column = "notnull", property = "notnull"),
    //         @Result(column = "dflt_value", property = "dfltValue"),
    //         @Result(column = "pk", property = "pk"),
    // })
    // @Select("pragma table_info(${tableName});")
    public List<SqliteColumns> queryTableColumns(String tableName) {
        return DataSourceManager.useConnection(connection -> {
            String sql = "PRAGMA table_info(':tableName');";
            return DBHelper.queryObjectList(connection, sql, params(tableName), SqliteColumns.class);
        });
    }
}
