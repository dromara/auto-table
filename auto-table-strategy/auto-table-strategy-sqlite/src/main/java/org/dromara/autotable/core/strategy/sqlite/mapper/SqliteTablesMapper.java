package org.dromara.autotable.core.strategy.sqlite.mapper;

import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.dromara.autotable.core.strategy.sqlite.data.dbdata.SqliteColumns;
import org.dromara.autotable.core.strategy.sqlite.data.dbdata.SqliteMaster;

import java.util.List;

/**
 * 创建更新表结构的Mapper
 * @author don
 */
public interface SqliteTablesMapper {

    /**
     * 查询建表语句
     *
     * @param tableName 表名
     * @return 建表语句
     */
    @Select("select `sql` from sqlite_master where type='table' and name=#{tableName};")
    String queryBuildTableSql(String tableName);

    /**
     * 查询建表语句
     *
     * @param tableName 表名
     * @return 建表语句
     */
    @Results({
            @Result(column = "type", property = "type"),
            @Result(column = "name", property = "name"),
            @Result(column = "tbl_name", property = "tblName"),
            @Result(column = "rootpage", property = "rootpage"),
            @Result(column = "sql", property = "sql"),
    })
    @Select("select * from sqlite_master where type='index' and tbl_name=#{tableName};")
    List<SqliteMaster> queryBuildIndexSql(String tableName);

    /**
     * 查询建表语句
     *
     * @param tableName 表名
     * @return 建表语句
     */
    @Results({
            @Result(column = "cid", property = "cid"),
            @Result(column = "name", property = "name"),
            @Result(column = "type", property = "type"),
            @Result(column = "notnull", property = "notnull"),
            @Result(column = "dflt_value", property = "dfltValue"),
            @Result(column = "pk", property = "pk"),
    })
    @Select("pragma table_info(${tableName});")
    List<SqliteColumns> queryTableColumns(String tableName);
}
