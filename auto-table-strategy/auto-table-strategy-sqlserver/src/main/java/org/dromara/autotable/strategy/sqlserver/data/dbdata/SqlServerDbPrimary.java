package org.dromara.autotable.strategy.sqlserver.data.dbdata;

import lombok.Data;
import org.dromara.autotable.core.utils.DBHelper;

/**
 * SQLServer 数据库，主键信息。
 *
 * @author don
 */
@Data
public class SqlServerDbPrimary {

    /**
     * 主键约束名
     */
    @DBHelper.ColumnName("primaryName")
    private String primaryName;
    /**
     * 主键列的拼接,例子：name,phone
     */
    @DBHelper.ColumnName("columns")
    private String columns;
}
