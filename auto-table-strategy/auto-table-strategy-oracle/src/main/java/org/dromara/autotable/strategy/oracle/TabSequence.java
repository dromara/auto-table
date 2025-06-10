package org.dromara.autotable.strategy.oracle;

import lombok.Data;

import java.util.Collections;
import java.util.Map;

/**
 * 序列信息
 */
@Data
public class TabSequence {
    private String sequence_name;

    public static TabSequence search(String tableName) {
        Map<String, Object> params = Collections.singletonMap("tableName", tableName);
        String sql = "SELECT * FROM user_sequences WHERE upper(sequence_name) = upper('auto_seq_:tableName')";
        return OracleHelper.DB.queryOne(sql, params, TabSequence.class);
    }
}
