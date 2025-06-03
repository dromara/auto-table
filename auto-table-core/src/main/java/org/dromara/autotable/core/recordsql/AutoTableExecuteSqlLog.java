package org.dromara.autotable.core.recordsql;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoColumns;
import org.dromara.autotable.annotation.ColumnType;
import org.dromara.autotable.annotation.Ignore;
import org.dromara.autotable.annotation.doris.DorisTable;
import org.dromara.autotable.annotation.doris.DorisTypeConstant;
import org.dromara.autotable.core.constants.DatabaseDialect;

/**
 * 记录自动建表执行的SQL
 *
 * @author don
 */
@Getter
@DorisTable(
        duplicate_key = {AutoTableExecuteSqlLog.Fields.tableSchema, AutoTableExecuteSqlLog.Fields.tableName},
        properties = {
                "replication_num=1"
        }
)
@FieldNameConstants
public class AutoTableExecuteSqlLog {

    @Ignore
    private Class<?> entityClass;

    private String tableSchema;

    private String tableName;

    @ColumnType(length = 5000)
    @AutoColumns({
            @AutoColumn(dialect = DatabaseDialect.Doris, type = DorisTypeConstant.STRING, length = -1)
    })
    private String sqlStatement;

    @Setter
    private String version;

    private Long executionTime;

    private Long executionEndTime;

    private AutoTableExecuteSqlLog() {
    }

    public static AutoTableExecuteSqlLog of(Class<?> entityClass, String tableSchema, String tableName, String sql, long executionTime, long executionEndTime) {
        AutoTableExecuteSqlLog autoTableExecuteSqlLog = new AutoTableExecuteSqlLog();
        autoTableExecuteSqlLog.entityClass = entityClass;
        autoTableExecuteSqlLog.tableSchema = tableSchema;
        autoTableExecuteSqlLog.tableName = tableName;
        autoTableExecuteSqlLog.sqlStatement = sql;
        autoTableExecuteSqlLog.executionTime = executionTime;
        autoTableExecuteSqlLog.executionEndTime = executionEndTime;
        return autoTableExecuteSqlLog;
    }
}
