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

    /**
     * 根据表名查询表的索引信息
     *
     * @param tableName 表名，用于查询索引信息
     * @return 返回一个包含索引信息的TabIndex对象列表
     */
    public static List<TabIndex> search(String tableName) {
        // 创建一个参数映射，用于将表名参数传递给SQL查询
        Map<String, Object> params = Collections.singletonMap("tableName", tableName);

        // 定义SQL查询语句，用于从数据库中获取表的索引信息
        // 这个查询加入了多种连接条件和过滤条件，以获取准确的索引信息
        String sql = "SELECT idx.table_name " +
                "     , idx.table_type " +
                "     , idx.index_name " +
                "     , idx.index_type " +
                "     , idx.uniqueness " +
                "     , col.column_name " +
                "     , col.column_position " +
                "     , col.descend " +
                "     , exp.column_expression " +
                "FROM user_indexes idx " +
                "         LEFT JOIN user_ind_columns col ON idx.table_name = col.table_name AND idx.index_name = col.index_name " +
                "         LEFT JOIN user_ind_expressions exp ON col.table_name = exp.table_name AND col.index_name = exp.index_name AND col.column_position = exp.column_position " +
                "         LEFT JOIN user_constraints uc ON idx.index_name = uc.index_name " +
                "WHERE idx.table_type = 'TABLE' " +
                "  AND idx.generated = 'N' " +
                "  AND uc.INDEX_NAME IS NULL " +
                "  AND upper(idx.table_name) = upper(':tableName') " +
                "ORDER BY idx.index_name, col.column_position";

        // 使用OracleHelper.DB.queryList方法执行SQL查询，并将结果映射为TabIndex对象列表
        // 这个方法将SQL查询语句、参数映射和结果需要映射的类类型作为参数
        return OracleHelper.DB.queryList(sql.replaceAll("\\s+", " "), params, TabIndex.class);
    }
}
