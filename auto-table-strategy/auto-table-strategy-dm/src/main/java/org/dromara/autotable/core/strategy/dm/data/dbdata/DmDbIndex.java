package org.dromara.autotable.core.strategy.dm.data.dbdata;

/**
 * @author Min, Freddy
 * @date: 2025/2/25 22:13
 */

import lombok.Data;

/**
 * 达梦索引元信息
 */
@Data
public class DmDbIndex {
    /**
     * 索引名称
     */
    private String indexName;
    /**
     * 是否唯一索引（UNIQUE/NONUNIQUE）
     */
    private String uniqueness;
    /**
     * 索引类型（NORMAL/BITMAP）
     */
    private String indexType;
    /**
     * 索引列（多个用逗号分隔）
     */
    private String columns;
    /**
     * 索引表空间
     */
    private String tablespace;
    /**
     * 索引创建语句
     */
    private String indexDef;
}
