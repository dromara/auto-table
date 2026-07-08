package org.dromara.autotable.strategy.doris.data;

import lombok.*;
import org.dromara.autotable.core.strategy.CompareTableInfo;
import org.dromara.autotable.strategy.doris.data.dbdata.InformationSchemaColumn;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author don
 */
@Getter
@Setter
public class DorisCompareTableInfo extends CompareTableInfo {
    /**
     * 表数据大小
     */
    private Long tableDataLength;
    /**
     * 创建语句
     */
    private String createTableSql;
    /**
     * 列信息
     */
    private List<InformationSchemaColumn> columns;
    /**
     * 临时表信息
     */
    private TempTableInfo tempTableInfo;
    /**
     * 添加、修改、删除的列信息
     */
    private List<String> added;
    private List<String> modified;
    private List<String> removed;
    /**
     * 需要重命名的列（逻辑删除）：Key=原列名, Value=新列名
     */
    private Map<String, String> renameColumnMap = new LinkedHashMap<>();


    public DorisCompareTableInfo(@NonNull String name, String schema) {
        super(name, schema);
    }

    @Override
    public boolean needModify() {
        return !createTableSql.equals(tempTableInfo.getCreateTableSql()) || !renameColumnMap.isEmpty();
    }

    @Override
    public String validateFailedMessage() {
        StringBuilder errorMsg = new StringBuilder();
        for (String line : added) {
            errorMsg.append("表配置新增：").append(line).append("\n");
        }
        for (String line : modified) {
            errorMsg.append("表配置修改：").append(line).append("\n");
        }
        for (String line : removed) {
            errorMsg.append("表配置删除：").append(line).append("\n");
        }
        if (!renameColumnMap.isEmpty()) {
            String renameColumns = renameColumnMap.entrySet().stream()
                    .map(entry -> entry.getKey() + " -> " + entry.getValue())
                    .collect(Collectors.joining(","));
            errorMsg.append("重命名列（逻辑删除）：").append(renameColumns).append("\n");
        }
        return errorMsg.toString();
    }

    public void addRenameColumns(Set<String> columnNames, String prefix) {
        for (String columnName : columnNames) {
            this.renameColumnMap.put(columnName, prefix + columnName);
        }
    }

    @Data
    @AllArgsConstructor
    public static class TempTableInfo {
        private String createTableSql;
        private List<InformationSchemaColumn> columns;
    }
}
