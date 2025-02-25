package org.dromara.autotable.core.strategy.dm.data.dbdata;

import lombok.Data;
import org.dromara.autotable.core.utils.StringUtils;

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

    /**
     * 获取完整类型定义
     */
    public String getDataTypeFormat() {
        switch (this.type.toUpperCase()) {
            case "VARCHAR2":
            case "VARCHAR":
                return type + "(" + length + ")";
            case "NUMBER":
                if (precision != null && scale != null) {
                    return scale == 0 ?
                            "NUMBER(" + precision + ")" :
                            "NUMBER(" + precision + "," + scale + ")";
                }
                return "NUMBER";
            case "CHAR":
                return length != null ? "CHAR(" + length + ")" : "CHAR";
            case "DECIMAL":
                return "DECIMAL(" + precision + "," + scale + ")";
            case "FLOAT":
            case "DOUBLE":
                return precision != null ? type + "(" + precision + ")" : type;
            case "SERIAL": // 自增列特殊处理
                return "SERIAL";
            default:
                return type;
        }
    }

    /**
     * 判断是否为自增列
     */
    public boolean isAutoIncrement() {
        return "SERIAL".equalsIgnoreCase(type) ||
                (StringUtils.hasText(defaultValue) && defaultValue.contains("NEXTVAL"));
    }
}
