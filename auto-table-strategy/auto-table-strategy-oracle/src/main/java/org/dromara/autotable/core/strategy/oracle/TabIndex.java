package org.dromara.autotable.core.strategy.oracle;

import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 索引信息
 */
@Data
public class TabIndex {
    private String table_name;
    private String table_type;
    private String index_name;
    private String index_type;
    private String uniqueness;
    private String column_name;
    private Integer column_position;
    private String descend;
    private String column_expression;

    public static List<TabIndex> search(String tableName) {
        Map<String, Object> params = Collections.singletonMap("tableName", tableName);
        String sql = "SELECT idx.table_name\n" +
                "     , idx.table_type\n" +
                "     , idx.index_name\n" +
                "     , idx.index_type\n" +
                "     , idx.uniqueness\n" +
                "     , col.column_name\n" +
                "     , col.column_position\n" +
                "     , col.descend\n" +
                "     , exp.column_expression\n" +
                "FROM user_indexes idx\n" +
                "         LEFT JOIN user_ind_columns col ON idx.table_name = col.table_name AND idx.index_name = col.index_name\n" +
                "         LEFT JOIN user_ind_expressions exp\n" +
                "                   ON col.table_name = exp.table_name\n" +
                "                       AND col.index_name = exp.index_name\n" +
                "                       AND col.column_position = exp.column_position\n" +
                "WHERE idx.table_type = 'TABLE'\n" +
                "  AND idx.generated = 'N'\n" +
                "  AND idx.constraint_index = 'NO'\n" +
                "  AND upper(idx.table_name) = upper(':tableName')\n" +
                "ORDER BY idx.index_name, col.column_position";
        return OracleHelper.DB.queryList(sql, params, TabIndex.class);
    }
}
