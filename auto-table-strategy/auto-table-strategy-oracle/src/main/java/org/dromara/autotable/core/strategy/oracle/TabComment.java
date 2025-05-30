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

    public static TabComment search(String tableName) {
        Map<String, Object> params = Collections.singletonMap("tableName", tableName);
        String sql = "SELECT * FROM user_tab_comments WHERE table_type = 'TABLE' AND upper(table_name) = upper(':tableName')";
        TabComment tabComment = OracleHelper.DB.queryOne(sql, params, TabComment.class);
        if (tabComment != null) {
            return tabComment;
        }
        return new TabComment();
    }
}
