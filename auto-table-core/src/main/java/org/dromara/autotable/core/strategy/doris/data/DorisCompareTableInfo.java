package org.dromara.autotable.core.strategy.doris.data;

import lombok.*;
import org.dromara.autotable.core.strategy.CompareTableInfo;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.strategy.mysql.data.MysqlColumnMetadata;
import org.dromara.autotable.core.strategy.mysql.data.MysqlCompareTableInfo;
import org.dromara.autotable.core.strategy.mysql.data.dbdata.InformationSchemaColumn;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author don
 */
@Getter
@Setter
public class DorisCompareTableInfo extends CompareTableInfo {
    private Long tableDataLength;
    private String createTableSql;
    private List<InformationSchemaColumn> columns;
    private TempTableInfo tempTableInfo;
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
