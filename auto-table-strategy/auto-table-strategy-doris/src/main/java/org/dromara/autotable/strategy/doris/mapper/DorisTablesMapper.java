package org.dromara.autotable.strategy.doris.mapper;


import lombok.Data;
import org.apache.commons.dbutils.QueryRunner;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.strategy.doris.data.dbdata.InformationSchemaColumn;
import org.dromara.autotable.core.utils.DBHelper;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 创建更新表结构的Mapper
 *
 * @author lizhian
 */
public class DorisTablesMapper {


    public Long findTableDataLength(String tableName) {
        String sql = "select data_length from information_schema.tables where table_name = ':tableName' and table_schema = (select database())";
        Map<String, Object> params = Collections.singletonMap("tableName", tableName);
        FindTableDataLengthDTO dto = DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObject(connection, sql, params, FindTableDataLengthDTO.class);
        });
        return dto.getDataLength();
    }

    @Data
    public static class FindTableDataLengthDTO {
        @DBHelper.ColumnName("DATA_LENGTH")
        private Long dataLength;
    }


    public String findTableCreateSql(String tableName) {
        String sql = "show create table `:tableName`";
        Map<String, Object> params = Collections.singletonMap("tableName", tableName);
        FindTableCreateSqlDTO dto = DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObject(connection, sql, params, FindTableCreateSqlDTO.class);
        });
        String createTable = dto.getCreateTable();
        // doris bug 处理
        if(createTable.contains("AUTO PARTITION BY RANGE date_trunc(")){
            createTable = createTable.replace("AUTO PARTITION BY RANGE date_trunc(", "AUTO PARTITION BY RANGE (date_trunc(");
            createTable = createTable.replace("')\n()", "'))\n()");
        }
        return createTable;
    }

    @Data
    public static class FindTableCreateSqlDTO {
        @DBHelper.ColumnName("Create Table")
        private String createTable;
    }
    private final QueryRunner queryRunner = new QueryRunner();

    public void executeRawSql(String sql) {
        DataSourceManager.useConnection(connection -> {
            try {
                DBHelper.setParameters(sql, Collections.emptyMap());
                return queryRunner.execute(connection, sql);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

    }

    /**
     * 根据表名查询库中该表的字段结构等信息
     *
     * @param tableName 表结构的map
     * @return 表的字段结构等信息
     */
    public List<InformationSchemaColumn> findTableEnsembleByTableName(String tableName) {
        String sql = "select * from information_schema.columns where table_name = ':tableName' and table_schema = (select database()) order by ordinal_position asc";
        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObjectList(connection, sql,
                    Collections.singletonMap("tableName", tableName), InformationSchemaColumn.class);
        });
    }
}
