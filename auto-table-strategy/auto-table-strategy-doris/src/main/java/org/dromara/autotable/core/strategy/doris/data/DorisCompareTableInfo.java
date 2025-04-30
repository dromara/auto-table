package org.dromara.autotable.core.strategy.doris.data;

import lombok.*;
import org.dromara.autotable.core.strategy.CompareTableInfo;
import org.dromara.autotable.core.strategy.doris.data.dbdata.InformationSchemaColumn;

import java.util.List;

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


    public DorisCompareTableInfo(@NonNull String name, @NonNull String schema) {
        super(name, schema);
    }

    @Override
    public boolean needModify() {
        return !createTableSql.equals(tempTableInfo.getCreateTableSql());
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
        return errorMsg.toString();
    }

    @Data
    @AllArgsConstructor
    public static class TempTableInfo {
        private String createTableSql;
        private List<InformationSchemaColumn> columns;
    }
}
