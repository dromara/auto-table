package org.dromara.autotable.strategy.dm.data.dbdata;

/**
 * @author Min, Freddy
 * @date: 2025/2/25 22:13
 */

import lombok.Data;
import org.dromara.autotable.core.utils.DBHelper;

/**
 * 达梦主键元信息
 */
@Data
public class DmDbPrimary {
    /**
     * 主键约束名称
     */
    @DBHelper.ColumnName("CONSTRAINT_NAME")
    private String primaryName;
    /**
     * 主键列名（多个用逗号分隔）
     */
    @DBHelper.ColumnName("COLUMNS")
    private String columns;
}
