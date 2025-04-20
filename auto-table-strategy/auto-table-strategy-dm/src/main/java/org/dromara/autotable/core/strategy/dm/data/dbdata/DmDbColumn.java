package org.dromara.autotable.core.strategy.dm.data.dbdata;

import lombok.Data;

/**
 * 达梦字段元信息（精简版）
 */
@Data
public class DmDbColumn {
    /**
     * 是否主键（根据系统表查询结果计算）
     */
    private boolean primary;
    /**
     * 字段名称
     */
    private String name;
    /**
     * 字段类型（达梦原生类型）
     */
    private String type;
    /**
     * 字段长度（字符类型）
     */
    private Integer length;
    /**
     * 数字类型总精度
     */
    private Integer precision;
    /**
     * 数字类型小数位
     */
    private Integer scale;
    /**
     * 是否允许NULL（Y/N）
     */
    private String nullable;
    /**
     * 默认值表达式
     */
    private String defaultValue;
    /**
     * 字段注释
     */
    private String comment;


}
