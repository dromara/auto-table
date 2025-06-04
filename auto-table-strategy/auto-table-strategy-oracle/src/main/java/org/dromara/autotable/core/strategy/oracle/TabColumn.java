package org.dromara.autotable.core.strategy.oracle;

import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 字段信息
 */
@Data
public class TabColumn {


    private String table_name;

    private String column_name;

    private String data_type;

    private Integer data_length;

    private Integer data_precision;

    private Integer data_scale;

    private String nullable;

    private Integer column_id;

    private String data_default;

    // private String data_default_vc;

    private String comments;

    private String constraint_name;

    private String constraint_type;

    public String getFullType() {
        String fullType = data_type;
        if (fullType.toLowerCase().contains("clob")) {
            return fullType;
        }
        if (fullType.contains("(") && fullType.contains(")")) {
            return fullType;
        }
        if (data_length == null && data_precision == null && data_scale == null) {
            return fullType;
        }
        if (data_precision == null && data_scale == null) {
            return fullType + "(" + data_length + ")";
        }
        if (data_precision != null) {
            fullType += "(" + data_precision;
            if (data_scale != null) {
                fullType += "," + data_scale;
            }
            fullType += ")";
        }
        return fullType;
    }


    /**
     * 根据表名查询表的列信息
     *
     * @param tableName 表名，用于查询列信息的唯一标识
     * @return 返回一个TabColumn对象的列表，每个对象包含表中每一列的详细信息
     */
    public static List<TabColumn> search(String tableName) {
        // 初始化参数，用于在SQL查询中传递表名
        Map<String, Object> params = Collections.singletonMap("tableName", tableName);

        // SQL查询语句，用于从数据库中获取指定表的列信息
        // 包含了表名、列名、数据类型、长度、精度、小数位数、是否可为空、列ID、默认值、注释以及主键信息
        String sql = "SELECT tc.table_name " +
                "     , tc.column_name " +
                "     , tc.data_type " +
                "     , tc.data_length " +
                "     , tc.data_precision " +
                "     , tc.data_scale " +
                "     , tc.nullable " +
                "     , tc.column_id " +
                "     , tc.data_default " +
                //"     , tc.data_default_vc " +
                "     , cc.comments " +
                "     , pk.constraint_name " +
                "     , pk.constraint_type " +
                "      FROM user_tab_columns tc " +
                "               LEFT JOIN user_col_comments cc ON tc.table_name = cc.table_name AND tc.column_name = cc.column_name " +
                "               LEFT JOIN (SELECT cons_col.table_name " +
                "                               , cons_col.column_name " +
                "                               , cons_col.constraint_name " +
                "                               , cons.constraint_type " +
                "                          FROM user_cons_columns cons_col " +
                "                                   LEFT JOIN user_constraints cons ON cons_col.constraint_name = cons.constraint_name " +
                "                          WHERE cons.constraint_type = 'P') pk " +
                "                         ON tc.table_name = pk.table_name AND tc.column_name = pk.column_name " +
                "      WHERE UPPER(tc.table_name) = UPPER(':tableName') " +
                "      ORDER BY tc.column_id";

        // 执行SQL查询，并将结果映射到TabColumn对象列表中
        return OracleHelper.DB.queryList(sql.replaceAll("\\s+", " "), params, TabColumn.class);
    }


}
