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

    private String data_default_vc;

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
        String sql = "SELECT tc.table_name\n" +
                "     , tc.column_name\n" +
                "     , tc.data_type\n" +
                "     , tc.data_length\n" +
                "     , tc.data_precision\n" +
                "     , tc.data_scale\n" +
                "     , tc.nullable\n" +
                "     , tc.column_id\n" +
                "     , tc.data_default\n" +
                "     , tc.data_default_vc\n" +
                "     , cc.comments\n" +
                "     , pk.constraint_name\n" +
                "     , pk.constraint_type\n" +
                "      FROM user_tab_columns tc\n" +
                "               LEFT JOIN user_col_comments cc ON tc.table_name = cc.table_name AND tc.column_name = cc.column_name\n" +
                "               LEFT JOIN (SELECT cons_col.table_name\n" +
                "                               , cons_col.column_name\n" +
                "                               , cons_col.constraint_name\n" +
                "                               , cons.constraint_type\n" +
                "                          FROM user_cons_columns cons_col\n" +
                "                                   LEFT JOIN user_constraints cons ON cons_col.constraint_name = cons.constraint_name\n" +
                "                          WHERE cons.constraint_type = 'P') pk\n" +
                "                         ON tc.table_name = pk.table_name AND tc.column_name = pk.column_name\n" +
                "      WHERE UPPER(tc.table_name) = UPPER(':tableName')\n" +
                "      ORDER BY tc.column_id";

        // 执行SQL查询，并将结果映射到TabColumn对象列表中
        return OracleHelper.DB.queryList(sql, params, TabColumn.class);
    }


}
