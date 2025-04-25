package org.dromara.autotable.core.strategy.mysql.mapper;

import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.strategy.mysql.data.dbdata.InformationSchemaColumn;
import org.dromara.autotable.core.strategy.mysql.data.dbdata.InformationSchemaStatistics;
import org.dromara.autotable.core.strategy.mysql.data.dbdata.InformationSchemaTable;
import org.dromara.autotable.core.utils.DBHelper;

import java.util.Collections;
import java.util.List;


/**
 * 创建更新表结构的Mapper
 * @author don
 */
public class MysqlTablesMapper {

    /**
     * 根据表名查询表在库中是否存在
     *
     * @param tableName 表结构的map
     * @return InformationSchemaTable
     */
    // @Select("select * from information_schema.tables where table_name = #{tableName} and table_schema = (select database())")
    public InformationSchemaTable findTableByTableName(String tableName) {
        return DataSourceManager.useConnection(connection -> {

            String sql = "select * from information_schema.tables where table_name = ':tableName' and table_schema = (select database())";

            return DBHelper.queryObject(connection, sql,
                    Collections.singletonMap("tableName", tableName), InformationSchemaTable.class);
        });
    }

    /**
     * 根据表名查询库中该表的字段结构等信息
     *
     * @param tableName 表结构的map
     * @return 表的字段结构等信息
     */
    // @Select("select * from information_schema.columns where table_name = #{tableName} and table_schema = (select database()) order by ordinal_position asc")
    public List<InformationSchemaColumn> findTableEnsembleByTableName(String tableName) {

        String sql = "select * from information_schema.columns where table_name = ':tableName' and table_schema = (select database()) order by ordinal_position asc";

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObjectList(connection, sql,
                    Collections.singletonMap("tableName", tableName), InformationSchemaColumn.class);
        });
    }

    /**
     * 查询指定表的所有主键和索引信息
     *
     * @param tableName 表名
     * @return 所有主键和索引信息
     */
    // @Select("SELECT * FROM information_schema.statistics WHERE table_name = #{tableName} and table_schema = (select database())")
    public List<InformationSchemaStatistics> queryTablePrimaryAndIndex(String tableName) {

        String sql = "SELECT * FROM information_schema.statistics WHERE table_name = ':tableName' and table_schema = (select database())";

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObjectList(connection, sql,
                    Collections.singletonMap("tableName", tableName), InformationSchemaStatistics.class);
        });
    }
}
