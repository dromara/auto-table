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

    /**
     * 查询建表语句
     *
     * @param tableName 表名
     * @return 建表语句
     */
    // @Select("select `sql` from sqlite_master where type='table' and name=#{tableName};")
    public String queryBuildTableSql(String tableName) {
        return DataSourceManager.useConnection(connection -> {
            String sql = "select `sql` from sqlite_master where type='table' and name=':tableName';";
            Map<String, Object> params = new HashMap<>();
            params.put("tableName", tableName);
            return DBHelper.queryValue(connection, sql, params);
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
    // @Select("select * from sqlite_master where type='index' and tbl_name=#{tableName};")
    public List<SqliteMaster> queryBuildIndexSql(String tableName) {
        return DataSourceManager.useConnection(connection -> {
            String sql = "select * from sqlite_master where type='index' and tbl_name=':tableName';";
            Map<String, Object> params = new HashMap<>();
            params.put("tableName", tableName);
            return DBHelper.queryObjectList(connection, sql, params, SqliteMaster.class);
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
            String sql = "pragma table_info(':tableName');";
            Map<String, Object> params = new HashMap<>();
            params.put("tableName", tableName);
            return DBHelper.queryObjectList(connection, sql, params, SqliteColumns.class);
        });
    }
}
