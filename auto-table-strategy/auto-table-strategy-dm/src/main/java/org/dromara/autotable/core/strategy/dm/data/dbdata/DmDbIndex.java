package org.dromara.autotable.core.strategy.dm.data.dbdata;

/**
 * @author Min, Freddy
 * @date: 2025/2/25 22:13
 */

import lombok.Data;
import org.dromara.autotable.core.utils.DBHelper;

/**
 * 达梦索引元信息
 */
@Data
public class DmDbIndex {
    /**
     * 索引名称
     */
    @DBHelper.ColumnName("INDEX_NAME")
    private String indexName;
    /**
     * 是否唯一索引（UNIQUE/NONUNIQUE）
     */
    @DBHelper.ColumnName("UNIQUENESS")
    private String uniqueness;
    /**
     * 索引列（多个用逗号分隔）
     */
    @DBHelper.ColumnName("COLUMNS")
    private String columns;
    /**
     * 索引类型（NORMAL/BITMAP）
     */
    @DBHelper.ColumnName("INDEX_TYPE")
    private String indexType;
}
