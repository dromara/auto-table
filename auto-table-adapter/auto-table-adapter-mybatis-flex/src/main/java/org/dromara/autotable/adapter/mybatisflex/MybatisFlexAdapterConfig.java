package org.dromara.autotable.adapter.mybatisflex;

import lombok.Data;

/**
 * MyBatis-Flex 适配器配置 POJO。
 * 由 starter 从 MybatisFlexProperties 提取值并注入。
 * adapter 不依赖 Spring，通过此 POJO 承载配置值。
 *
 * @author auto-table
 */
@Data
public class MybatisFlexAdapterConfig {

    /**
     * 是否开启驼峰转下划线（字段名 + 表名）
     */
    private boolean mapUnderscoreToCamelCase = true;
}
