package org.dromara.autotable.core.strategy.oracle.data.dbdata;

import lombok.Data;
import org.dromara.autotable.core.utils.DBHelper;

@Data
public class InformationSchemaColumn {

    /**
     * 表名（大写）
     */
    @DBHelper.ColumnName("TABLE_NAME")
    private String tableName;

    /**
     * 列名（大写）
     */
    @DBHelper.ColumnName("COLUMN_NAME")
    private String columnName;

    /**
     * 字段的数据类型，如 VARCHAR2、NUMBER、DATE 等
     */
    @DBHelper.ColumnName("DATA_TYPE")
    private String dataType;

    /**
     * 字段的长度（对于 CHAR、VARCHAR2 等类型）
     */
    @DBHelper.ColumnName("DATA_LENGTH")
    private Integer dataLength;

    /**
     * 数值字段的有效位数（precision）
     */
    @DBHelper.ColumnName("DATA_PRECISION")
    private Integer dataPrecision;

    /**
     * 数值字段的小数位数（scale）
     */
    @DBHelper.ColumnName("DATA_SCALE")
    private Integer dataScale;

    /**
     * 是否允许为空（Y/N）
     */
    @DBHelper.ColumnName("NULLABLE")
    private String nullable;

    /**
     * 列在表中的位置（从1开始）
     */
    @DBHelper.ColumnName("COLUMN_ID")
    private Integer columnId;

    /**
     * 字段的默认值（可能为 null）
     */
    @DBHelper.ColumnName("DATA_DEFAULT")
    private String dataDefault;

    /**
     * 字段的注释（需要关联 user_col_comments 或 all_col_comments 获取）
     */
    @DBHelper.ColumnName("COMMENTS")
    private String comments;


    public boolean isNotNull(){
        return "N".equals(nullable);
    }
}
