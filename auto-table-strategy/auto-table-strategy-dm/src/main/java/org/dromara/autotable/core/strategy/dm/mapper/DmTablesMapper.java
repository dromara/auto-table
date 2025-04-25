package org.dromara.autotable.core.strategy.dm.mapper;

/**
 * @author Min, Freddy
 * @date: 2025/2/25 22:09
 */


import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.strategy.dm.data.dbdata.DmDbColumn;
import org.dromara.autotable.core.strategy.dm.data.dbdata.DmDbIndex;
import org.dromara.autotable.core.strategy.dm.data.dbdata.DmDbPrimary;
import org.dromara.autotable.core.utils.DBHelper;

import java.util.HashMap;
import java.util.List;

/**
 * 达梦数据库系统表查询Mapper
 */
public class DmTablesMapper {

    /**
     * 查询表注释
     */
    public String selectTableDescription(String schema, String tableName) {

        String sql = "SELECT COMMENTS FROM USER_TAB_COMMENTS WHERE TABLE_NAME = ':tableName';";

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryValue(connection, sql, new HashMap<String, Object>() {{
                put("tableName", tableName);
                put("schema", schema);
            }});
        });
    }

    /**
     * 查询表字段详细信息
     */
    public List<DmDbColumn> selectTableColumns(String schema, String tableName) {
        String sql = "SELECT DISTINCT c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.DATA_PRECISION, c.DATA_SCALE,  \n" +
                "       c.NULLABLE, c.DATA_DEFAULT, com.COMMENTS  \n" +
                "FROM ALL_TAB_COLUMNS c  \n" +
                "LEFT JOIN USER_COL_COMMENTS com ON c.TABLE_NAME = com.TABLE_NAME AND c.COLUMN_NAME = com.COLUMN_NAME" +
                "  \n" +
                "WHERE c.TABLE_NAME = ':tableName' \n" +
                "AND c.OWNER = ':schema';";

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObjectList(connection, sql, new HashMap<String, Object>() {{
                put("tableName", tableName);
                put("schema", schema);
            }}, DmDbColumn.class);
        });
    }

    /**
     * 查询主键信息
     */
    public DmDbPrimary selectPrimaryKey(String schema, String tableName) {

        String sql = "SELECT cons.CONSTRAINT_NAME,  \n" +
                "       LISTAGG(cols.COLUMN_NAME, ',') WITHIN GROUP (ORDER BY cols.POSITION) AS COLUMNS  \n" +
                "FROM USER_CONSTRAINTS cons  \n" +
                "JOIN USER_CONS_COLUMNS cols ON cons.CONSTRAINT_NAME = cols.CONSTRAINT_NAME  \n" +
                "WHERE cons.TABLE_NAME =':tableName'  \n" +
                "  AND cons.CONSTRAINT_TYPE = 'P'  \n" +
                "GROUP BY cons.CONSTRAINT_NAME;";

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObject(connection, sql, new HashMap<String, Object>() {{
                put("tableName", tableName);
                put("schema", schema);
            }}, DmDbPrimary.class);
        });
    }

    /**
     * 查询索引信息
     */
    public List<DmDbIndex> selectTableIndexes(String schema, String tableName) {

        String sql = "SELECT ind.INDEX_NAME,  \n" +
                "       ind.UNIQUENESS,  \n" +
                "       LISTAGG(cols.COLUMN_NAME, ',') WITHIN GROUP (ORDER BY cols.COLUMN_POSITION) AS COLUMNS,  \n" +
                "       ind.INDEX_TYPE  \n" +
                "FROM USER_INDEXES ind  \n" +
                "JOIN USER_IND_COLUMNS cols ON ind.INDEX_NAME = cols.INDEX_NAME  \n" +
                "WHERE ind.TABLE_NAME =':tableName'  \n" +
                "  AND ind.INDEX_TYPE != 'LOB'  \n" +// 排除LOB索引
                "  AND ind.INDEX_NAME NOT LIKE 'INDEX%'  \n" +// 排除系统索引
                "GROUP BY ind.INDEX_NAME, ind.UNIQUENESS, ind.INDEX_TYPE;";

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObjectList(connection, sql, new HashMap<String, Object>() {{
                put("tableName", tableName);
                put("schema", schema);
            }}, DmDbIndex.class);
        });
    }

}
