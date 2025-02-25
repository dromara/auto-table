package org.dromara.autotable.core.strategy.dm.data.dbdata;

/**
 * @author Min, Freddy
 * @date: 2025/2/25 22:13
 */

import lombok.Data;

/**
 * 达梦主键元信息
 */
@Data
public class DmDbPrimary {
    /**
     * 主键约束名称
     */
    private String primaryName;
    /**
     * 主键列名（多个用逗号分隔）
     */
    private String columns;
}