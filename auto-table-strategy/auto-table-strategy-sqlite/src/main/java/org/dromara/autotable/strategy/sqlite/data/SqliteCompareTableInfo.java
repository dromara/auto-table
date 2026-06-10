package org.dromara.autotable.strategy.sqlite.data;

import org.dromara.autotable.core.strategy.CompareTableInfo;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * 重命名的列（逻辑删除）：Key=原列名, Value=新列名
     */
    private Map<String, String> renameColumnMap = new LinkedHashMap<>();

    public SqliteCompareTableInfo(@NonNull String name, String schema) {
        super(name, schema);
    }

    @Override
    public boolean needModify() {
        return rebuildTableSql != null ||
                !buildIndexSqlList.isEmpty() ||
                !deleteIndexList.isEmpty() ||
                !renameColumnMap.isEmpty();
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
        if (!renameColumnMap.isEmpty()) {
            String renameColumns = renameColumnMap.entrySet().stream()
                    .map(entry -> entry.getKey() + " -> " + entry.getValue())
                    .collect(Collectors.joining(","));
            errorMsg.append("重命名列（逻辑删除）：").append(renameColumns).append("\n");
        }
        if (!buildIndexSqlList.isEmpty()) {
            errorMsg.append("新增的索引: ").append(String.join(",", buildIndexSqlList)).append("\n");
        }
        return errorMsg.toString();
    }
}
