package org.dromara.autotable.core.strategy;

import lombok.Getter;
import lombok.Setter;

/**
 * Bean的基础信息元数据
 *
 * @author don
 */
@Getter
public abstract class TableMetadata {

    /**
     * 实体类
     */
    protected Class<?> entityClass;

    /**
     * 表名
     */
    protected String tableName;
    public static String tableNameFieldName = "tableName";

    /**
     * schema
     */
    protected String schema;

    /**
     * 注释
     */
    @Setter
    protected String comment;

    public TableMetadata(Class<?> entityClass, String tableName, String schema, String comment) {
        this.entityClass = entityClass;
        this.tableName = tableName;
        this.schema = schema;
        this.comment = comment;
    }
}
