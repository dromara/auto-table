package org.dromara.autotable.core.strategy.pgsql.data.dbdata;

import lombok.Data;
import org.dromara.autotable.core.utils.DBHelper;

/**
 * pgsql数据库，索引信息
 */
@Data
public class PgsqlDbPrimary {

    /**
     * 主键名称
     */
    @DBHelper.ColumnName("primary_name")
    private String primaryName;

    /**
     * 主键列的拼接,例子：name,phone
     */
    @DBHelper.ColumnName("columns")
    private String columns;
}