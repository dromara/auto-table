package org.dromara.autotable.core.strategy.oracle;

import lombok.Data;

import java.util.Collections;
import java.util.Map;

/**
 * 表注释信息
 */
@Data
public class TabComment {
    private String table_name;
    private String table_type;
    private String comments;

    /**
     * 根据表名查询表注释信息
     * 此方法旨在从数据库中检索特定表的注释信息，使用提供的表名作为查询参数
     * 如果找到匹配的表注释信息，则返回该信息；否则返回一个新的空表注释对象
     *
     * @param tableName 要查询注释的表名，是查询的主要参数
     * @return 返回一个TabComment对象，包含查询到的表注释信息如果没有找到匹配的表注释，返回一个新的空TabComment对象
     */
    public static TabComment search(String tableName) {
        // 初始化查询参数，将表名存储在参数映射中
        Map<String, Object> params = Collections.singletonMap("tableName", tableName);
        // 定义SQL查询语句，用于从user_tab_comments表中根据表名查询表注释信息
        String sql = "SELECT * FROM user_tab_comments WHERE table_type = 'TABLE' AND upper(table_name) = upper(':tableName')";
        // 执行查询，返回一个TabComment对象，如果查询不到则返回null
        TabComment tabComment = OracleHelper.DB.queryOne(sql, params, TabComment.class);
        // 检查查询结果是否为空，如果不为空则返回查询到的TabComment对象
        if (tabComment != null) {
            return tabComment;
        }
        // 如果查询结果为空，返回一个新的空TabComment对象
        return new TabComment();
    }
}
