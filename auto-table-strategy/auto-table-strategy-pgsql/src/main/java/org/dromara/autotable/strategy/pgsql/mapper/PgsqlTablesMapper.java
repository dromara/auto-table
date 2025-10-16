package org.dromara.autotable.strategy.pgsql.mapper;

import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.strategy.pgsql.data.dbdata.PgsqlDbColumn;
import org.dromara.autotable.strategy.pgsql.data.dbdata.PgsqlDbIndex;
import org.dromara.autotable.strategy.pgsql.data.dbdata.PgsqlDbPrimary;
import org.dromara.autotable.core.utils.DBHelper;

import java.util.HashMap;
import java.util.List;


/**
 * 创建更新表结构的Mapper
 *
 * @author don
 */
public class PgsqlTablesMapper {

    /**
     * 查询表名注释
     *
     * @param schema    schema
     * @param tableName 表名
     * @return 表注释
     */
    // @Select("SELECT des.description FROM pg_catalog.pg_description des " +
    //         "LEFT JOIN pg_catalog.pg_class clas ON des.objoid = clas.oid " +
    //         "LEFT JOIN pg_catalog.pg_namespace nams ON clas.relnamespace = nams.oid " +
    //         "WHERE nams.nspname = #{schema} AND clas.relname = #{tableName} AND des.objsubid = 0;")
    public String selectTableDescription(String schema, String tableName) {

        String sql = "SELECT des.description FROM pg_catalog.pg_description des " +
                "LEFT JOIN pg_catalog.pg_class clas ON des.objoid = clas.oid " +
                "LEFT JOIN pg_catalog.pg_namespace nams ON clas.relnamespace = nams.oid " +
                "WHERE nams.nspname = ':schema' AND clas.relname = ':tableName' AND des.objsubid = 0;";

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryValue(connection, sql, new HashMap<String, Object>() {{
                put("tableName", tableName);
                put("schema", schema);
            }});
        });
    }

    /**
     * 查询所有字段信息
     *
     * @param tableName 表名
     * @return 字段信息
     */
    // @Select("SELECT DISTINCT key_col.column_name IS NOT NULL AS primary, des.description, cols.* " +
    //         "FROM information_schema.columns cols " +
    //         "LEFT JOIN information_schema.key_column_usage key_col ON key_col.column_name = cols.column_name " +
    //         "LEFT JOIN pg_catalog.pg_class clas ON clas.relname = cols.table_name AND clas.relnamespace = ( SELECT oid FROM pg_namespace WHERE nspname = cols.table_schema ) " +
    //         "LEFT JOIN pg_catalog.pg_description des ON des.objoid = clas.oid AND cols.ordinal_position = des.objsubid " +
    //         "WHERE cols.table_schema = #{schema} AND cols.table_name = #{tableName};")
    public List<PgsqlDbColumn> selectTableFieldDetail(String schema, String tableName) {

        String sql = "SELECT DISTINCT key_col.column_name IS NOT NULL AS primary, des.description, cols.* " +
                "FROM information_schema.columns cols " +
                "LEFT JOIN information_schema.key_column_usage key_col ON key_col.column_name = cols.column_name " +
                "LEFT JOIN pg_catalog.pg_class clas ON clas.relname = cols.table_name AND clas.relnamespace = ( SELECT oid FROM pg_namespace WHERE nspname = cols.table_schema ) " +
                "LEFT JOIN pg_catalog.pg_description des ON des.objoid = clas.oid AND cols.ordinal_position = des.objsubid " +
                "WHERE cols.table_schema = ':schema' AND cols.table_name = ':tableName';";

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObjectList(connection, sql, new HashMap<String, Object>() {{
                put("tableName", tableName);
                put("schema", schema);
            }}, PgsqlDbColumn.class);
        });
    }

    /**
     * <p>查询所有索引信息
     * <p>关于pg_constraint表的contype值有以下几种：
     * <p>主键约束（PRIMARY KEY）：contype 字段的值为 'p'
     * <p>唯一约束（UNIQUE）：contype 字段的值为 'u'
     * <p>检查约束（CHECK）：contype 字段的值为 'c'
     * <p>外键约束（FOREIGN KEY）：contype 字段的值为 'f'
     * <p>排他约束（EXCLUDE）：contype 字段的值为 'x'
     *
     * @param tableName 表名
     * @return 索引信息
     */
    // @Select("SELECT DISTINCT des.description, idxs.* " +
    //         "FROM pg_catalog.pg_indexes idxs " +
    //         "LEFT JOIN pg_catalog.pg_class clas ON idxs.indexname = clas.relname " +
    //         "LEFT JOIN pg_catalog.pg_description des ON clas.oid = des.objoid " +
    //         "LEFT JOIN pg_catalog.pg_constraint cst ON idxs.indexname = cst.conname " +
    //         "WHERE idxs.schemaname = #{schema} AND idxs.tablename = #{tableName} AND cst.contype is null;")
    public List<PgsqlDbIndex> selectTableIndexesDetail(String schema, String tableName) {

        String sql = "SELECT DISTINCT des.description, idxs.* " +
                "FROM pg_catalog.pg_indexes idxs " +
                "LEFT JOIN pg_catalog.pg_class clas ON idxs.indexname = clas.relname " +
                "LEFT JOIN pg_catalog.pg_description des ON clas.oid = des.objoid " +
                "LEFT JOIN pg_catalog.pg_constraint cst ON idxs.indexname = cst.conname " +
                "WHERE idxs.schemaname = ':schema' AND idxs.tablename = ':tableName' AND cst.contype is null;";

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObjectList(connection, sql, new HashMap<String, Object>() {{
                put("tableName", tableName);
                put("schema", schema);
            }}, PgsqlDbIndex.class);
        });
    }

    /**
     * 查询表下的主键信息
     *
     * @param tableName 表明
     * @return 主键名
     */
    public PgsqlDbPrimary selectPrimaryKeyName(String schema, String tableName) {

        String sql = "SELECT con.conname AS primary_name, string_agg(col.attname, ',' ORDER BY att.ord) AS columns " +
                "FROM pg_catalog.pg_constraint con " +
                "JOIN pg_catalog.pg_class cls ON con.conrelid = cls.oid " +
                "JOIN pg_catalog.pg_namespace nsp ON cls.relnamespace = nsp.oid " +
                "JOIN pg_catalog.unnest(con.conkey) WITH ORDINALITY AS att(attnum, ord) ON TRUE " +
                "JOIN pg_catalog.pg_attribute col ON col.attrelid = cls.oid AND col.attnum = att.attnum " +
                "WHERE nsp.nspname = ':schema' AND cls.relname = ':tableName' AND con.contype = 'p' " +
                "GROUP BY con.conname;";

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObject(connection, sql, new HashMap<String, Object>() {{
                put("tableName", tableName);
                put("schema", schema);
            }}, PgsqlDbPrimary.class);
        });
    }
}
