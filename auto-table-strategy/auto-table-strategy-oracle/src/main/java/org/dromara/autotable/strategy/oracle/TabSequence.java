package org.dromara.autotable.strategy.oracle;

import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 序列信息
 */
@Data
public class TabSequence {
    private String sequence_name;

    public static TabSequence search(String tableName) {
        String expectedName = OracleIdentifierUtils.sequenceName(tableName);
        Map<String, Object> params = Collections.singletonMap("sequenceName", expectedName);
        String sql = "SELECT * FROM user_sequences " +
                "WHERE sequence_name = ':sequenceName' OR sequence_name = upper(':sequenceName')";
        List<TabSequence> sequences = OracleHelper.DB.queryList(sql, params, TabSequence.class);
        return OracleIdentifierUtils.resolveExisting(sequences, expectedName, TabSequence::getSequence_name);
    }
}
