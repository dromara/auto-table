package org.dromara.autotable.core.strategy.oracle.data.dbdata;

import lombok.Data;
import org.dromara.autotable.core.utils.DBHelper;

@Data
public class InformationSchemaConstraint {

    /**
     * 外键约束名称
     */
    @DBHelper.ColumnName("CONSTRAINT_NAME")
    private String constraintName;

    /**
     * 外键所在的表名
     */
    @DBHelper.ColumnName("TABLE_NAME")
    private String tableName;

    /**
     * 外键字段名
     */
    @DBHelper.ColumnName("COLUMN_NAME")
    private String columnName;

    /**
     * 引用的主键/唯一键约束名
     */
    @DBHelper.ColumnName("R_CONSTRAINT_NAME")
    private String referencedConstraintName;

    /**
     * 被引用的表名
     */
    @DBHelper.ColumnName("REFERENCED_TABLE_NAME")
    private String referencedTableName;

    /**
     * 被引用的字段名
     */
    @DBHelper.ColumnName("REFERENCED_COLUMN_NAME")
    private String referencedColumnName;
}
