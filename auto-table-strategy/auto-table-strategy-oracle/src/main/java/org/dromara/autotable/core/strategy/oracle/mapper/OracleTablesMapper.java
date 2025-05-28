package org.dromara.autotable.core.strategy.oracle.mapper;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.strategy.oracle.data.dbdata.InformationSchemaColumn;
import org.dromara.autotable.core.strategy.oracle.data.dbdata.InformationSchemaConstraint;
import org.dromara.autotable.core.strategy.oracle.data.dbdata.InformationSchemaIndex;
import org.dromara.autotable.core.strategy.oracle.data.dbdata.InformationSchemaTable;
import org.dromara.autotable.core.utils.DBHelper;

import java.sql.SQLException;
import java.util.*;

public class OracleTablesMapper {
    public InformationSchemaTable findTableByTableName(String tableName) {
        String finalTableName = tableName.toUpperCase(Locale.ROOT);
        return DataSourceManager.useConnection(connection -> {
            String sql = "select * from USER_OBJECTS where OBJECT_NAME = ':tableName'";
            return DBHelper.queryObject(connection, sql,
                    Collections.singletonMap("tableName", finalTableName), InformationSchemaTable.class);
        });
    }


    public List<InformationSchemaColumn> findTableColumnByTableName(String tableName) {
        String finalTableName = tableName.toUpperCase(Locale.ROOT);
        String sql = "SELECT  " +
                "    utc.table_name, " +
                "    utc.column_name, " +
                "    utc.data_type, " +
                "    utc.data_length, " +
                "    utc.data_precision, " +
                "    utc.data_scale, " +
                "    utc.nullable, " +
                "    utc.column_id, " +
                "    ucc.comments " +
                "FROM user_tab_columns utc " +
                "LEFT JOIN user_col_comments ucc " +
                "  ON utc.table_name = ucc.table_name AND utc.column_name = ucc.column_name " +
                "WHERE utc.table_name = ':tableName' " ;
        return DataSourceManager.useConnection(connection -> {
            List<InformationSchemaColumn> columnList = DBHelper.queryObjectList(connection, sql, Collections.singletonMap("tableName", finalTableName), InformationSchemaColumn.class);
            String defaultQuerySql = String.format("select COLUMN_NAME,DATA_DEFAULT from user_tab_columns where TABLE_NAME = '%s'", finalTableName);
            try {
                List<Map<String, Object>> result = new QueryRunner().query(connection, defaultQuerySql , new MapListHandler());
                Map<String,String> columnDefaultMap = new HashMap<>();
                result.forEach(it -> columnDefaultMap.put(String.valueOf(it.get("COLUMN_NAME")),String.valueOf(it.get("DATA_DEFAULT"))));
                for (InformationSchemaColumn informationSchemaColumn : columnList) {
                    informationSchemaColumn.setDataDefault(columnDefaultMap.get(String.valueOf(informationSchemaColumn.getColumnName())));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return columnList;
        });
    }

    public List<InformationSchemaConstraint> findTableConstraintByTableName(String tableName,String constraintType) {
        String finalTableName = tableName.toUpperCase(Locale.ROOT);
        String sql = "SELECT \n" +
                "    uc.constraint_name,\n" +
                "    uc.table_name,\n" +
                "    ucc.column_name,\n" +
                "    uc.r_constraint_name,\n" +
                "    uc2.table_name AS referenced_table_name,\n" +
                "    ucc2.column_name AS referenced_column_name\n" +
                "FROM user_constraints uc\n" +
                "JOIN user_cons_columns ucc\n" +
                "  ON uc.constraint_name = ucc.constraint_name\n" +
                "LEFT JOIN user_constraints uc2\n" +
                "  ON uc.r_constraint_name = uc2.constraint_name\n" +
                "LEFT JOIN user_cons_columns ucc2\n" +
                "  ON uc2.constraint_name = ucc2.constraint_name AND ucc.position = ucc2.position\n" +
                "WHERE uc.constraint_type = ':constraintType'\n" +
                "  AND uc.table_name = ':tableName'\n" +
                "ORDER BY uc.constraint_name, ucc.position\n";
        Map<String,Object> param = new HashMap<>();
        param.put("constraintType",constraintType);
        param.put("tableName",finalTableName);

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObjectList(connection, sql,param, InformationSchemaConstraint.class);
        });
    }
    public List<InformationSchemaIndex> findTableIndexByTableName(String tableName) {
        String finalTableName = tableName.toUpperCase(Locale.ROOT);
        String sql = "SELECT \n" +
                "    ui.index_name,\n" +
                "    ui.table_name,\n" +
                "    ui.uniqueness,\n" +
                "    uic.column_name,\n" +
                "    uic.column_position,\n" +
                "    uic.descend\n" +
                "FROM user_indexes ui\n" +
                "JOIN user_ind_columns uic\n" +
                "  ON ui.index_name = uic.index_name\n" +
                "WHERE ui.table_name = ':tableName'\n" +
                "  AND ui.index_name NOT IN (\n" +
                "    SELECT index_name\n" +
                "    FROM user_constraints\n" +
                "    WHERE table_name = ':tableName'\n" +
                "      AND constraint_type = 'P'\n" +
                "      AND index_name IS NOT NULL\n" +
                ") " +
                "ORDER BY ui.index_name, uic.column_position\n";

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObjectList(connection, sql,
                    Collections.singletonMap("tableName", finalTableName), InformationSchemaIndex.class);
        });
    }
}
