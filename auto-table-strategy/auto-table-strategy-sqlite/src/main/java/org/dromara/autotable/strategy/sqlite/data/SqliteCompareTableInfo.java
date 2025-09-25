package org.dromara.autotable.strategy.sqlite.data;

import org.dromara.autotable.core.strategy.CompareTableInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author don
 */
@Getter
@Setter
public class SqliteCompareTableInfo extends CompareTableInfo {

    /**
     * 构建表的sql，如果不为空，则重新构建表
     */
    private String rebuildTableSql;

    /**
     * merge后，可迁移的有效字段
     */
    private List<String> dataMigrationColumnList = new ArrayList<>();

    /**
     * 新构建索引的sql
     */
    private List<String> buildIndexSqlList = new ArrayList<>();

    /**
     * 待删除的索引
     */
    private List<String> deleteIndexList = new ArrayList<>();

    public SqliteCompareTableInfo(@NonNull String name, String schema) {
        super(name, schema);
    }

    @Override
    public boolean needModify() {
        return rebuildTableSql != null ||
                !buildIndexSqlList.isEmpty() ||
                !deleteIndexList.isEmpty();
    }

    @Override
    public String validateFailedMessage() {
        StringBuilder errorMsg = new StringBuilder();
        if (rebuildTableSql != null) {
            errorMsg.append("新的建表语句: ").append(rebuildTableSql).append("\n");
        }
        if (!deleteIndexList.isEmpty()) {
            errorMsg.append("待删除的索引: ").append(String.join(",", deleteIndexList)).append("\n");
        }
        if (!buildIndexSqlList.isEmpty()) {
            errorMsg.append("新增的索引: ").append(String.join(",", buildIndexSqlList)).append("\n");
        }
        return errorMsg.toString();
    }

    @Data
    @AllArgsConstructor
    public static class RebuildIndex {

        private String name;
        private String sql;
    }
}
