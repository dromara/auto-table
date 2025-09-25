package org.dromara.autotable.strategy.h2.mapper;

import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.strategy.h2.data.dbdata.InformationSchemaColumns;
import org.dromara.autotable.strategy.h2.data.dbdata.InformationSchemaIndexes;
import org.dromara.autotable.strategy.h2.data.dbdata.InformationSchemaTables;
import org.dromara.autotable.core.utils.DBHelper;

import java.util.HashMap;
import java.util.List;


/**
 * 创建更新表结构的Mapper
 *
 * @author don
 */
public class H2TablesMapper {

    /**
     * 根据表名查询表在库中是否存在
     *
     * @param tableSchema schema名
     * @param tableName   表名
     * @return InformationSchemaTables
     */
    // @Select("SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = (#{tableSchema}) and TABLE_NAME = (#{tableName});")
    public InformationSchemaTables findTableInformation(String tableSchema, String tableName) {

        String sql = "SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = (':tableSchema') and TABLE_NAME = (':tableName');";

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObject(connection, sql, new HashMap<String, Object>() {{
                put("tableName", tableName);
                put("tableSchema", tableSchema);
            }}, InformationSchemaTables.class);
        });
    }

    /**
     * 根据表名查询库中该表的字段结构等信息
     *
     * @param tableSchema schema名
     * @param tableName   表名
     * @return 表的字段结构等信息
     */
    // @Select("SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = (#{tableSchema}) and TABLE_NAME = (#{tableName});")
    public List<InformationSchemaColumns> findColumnInformation(String tableSchema, String tableName) {

        String sql = "SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = (':tableSchema') and TABLE_NAME = (':tableName');";

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObjectList(connection, sql, new HashMap<String, Object>() {{
                put("tableName", tableName);
                put("tableSchema", tableSchema);
            }}, InformationSchemaColumns.class);
        });
    }

    /**
     * 查询指定表的所有主键和索引信息
     *
     * @param tableSchema schema名
     * @param tableName   表名
     * @return 所有主键和索引信息
     */
    // @Select("SELECT IC.*, I.REMARKS FROM INFORMATION_SCHEMA.INDEX_COLUMNS IC " +
    //         "LEFT JOIN INFORMATION_SCHEMA.INDEXES I ON I.INDEX_NAME = IC.INDEX_NAME AND IC.TABLE_SCHEMA = I.TABLE_SCHEMA AND IC.TABLE_NAME = I.TABLE_NAME " +
    //         "WHERE IC.TABLE_SCHEMA = (#{tableSchema}) AND IC.TABLE_NAME = (#{tableName}) AND I.INDEX_TYPE_NAME != 'PRIMARY KEY';")
    public List<InformationSchemaIndexes> findIndexInformation(String tableSchema, String tableName) {

        String sql = "SELECT IC.*, I.REMARKS FROM INFORMATION_SCHEMA.INDEX_COLUMNS IC " +
                "LEFT JOIN INFORMATION_SCHEMA.INDEXES I ON I.INDEX_NAME = IC.INDEX_NAME AND IC.TABLE_SCHEMA = I.TABLE_SCHEMA AND IC.TABLE_NAME = I.TABLE_NAME " +
                "WHERE IC.TABLE_SCHEMA = (':tableSchema') AND IC.TABLE_NAME = (':tableName') AND I.INDEX_TYPE_NAME != 'PRIMARY KEY';";

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObjectList(connection, sql, new HashMap<String, Object>() {{
                put("tableName", tableName);
                put("tableSchema", tableSchema);
            }}, InformationSchemaIndexes.class);
        });
    }
}
