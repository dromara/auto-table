package org.dromara.autotable.strategy.dm.data.dbdata;

import lombok.Data;
import org.dromara.autotable.core.utils.DBHelper;

/**
 * 达梦字段元信息（精简版）
 */
@Data
public class DmDbColumn {
    /**
     * 字段名称
     */
    @DBHelper.ColumnName("COLUMN_NAME")
    private String name;
    /**
     * 字段类型（达梦原生类型）
     */
    @DBHelper.ColumnName("DATA_TYPE")
    private String type;
    /**
     * 字段长度（字符类型）
     */
    @DBHelper.ColumnName("DATA_LENGTH")
    private Integer length;
    /**
     * 数字类型总精度
     */
    @DBHelper.ColumnName("DATA_PRECISION")
    private Integer precision;
    /**
     * 数字类型小数位
     */
    @DBHelper.ColumnName("DATA_SCALE")
    private Integer scale;
    /**
     * 是否允许NULL（Y/N）
     */
    @DBHelper.ColumnName("NULLABLE")
    private String nullable;
    /**
     * 默认值表达式
     */
    @DBHelper.ColumnName("DATA_DEFAULT")
    private String defaultValue;
    /**
     * 字段注释
     */
    @DBHelper.ColumnName("COMMENTS")
    private String comment;


}
