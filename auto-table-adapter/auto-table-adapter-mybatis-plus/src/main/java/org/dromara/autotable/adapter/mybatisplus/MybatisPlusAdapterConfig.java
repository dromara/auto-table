package org.dromara.autotable.adapter.mybatisplus;

import lombok.Data;

/**
 * MyBatis-Plus 适配器配置 POJO。
 * 由 starter 从 MP 原生 MybatisPlusProperties 提取值并注入。
 * adapter 不依赖 Spring，通过此 POJO 承载配置值。
 *
 * @author auto-table
 */
@Data
public class MybatisPlusAdapterConfig {

    /**
     * 表名前缀
     */
    private String tablePrefix = "";

    /**
     * 是否开启驼峰转下划线（字段名 + 表名）
     */
    private boolean mapUnderscoreToCamelCase = true;

    /**
     * 表名是否大写模式
     */
    private boolean capitalMode = false;

    /**
     * 逻辑删除字段名
     */
    private String logicDeleteField = "";

    /**
     * 逻辑删除未删值
     */
    private String logicNotDeleteValue = "";
}
