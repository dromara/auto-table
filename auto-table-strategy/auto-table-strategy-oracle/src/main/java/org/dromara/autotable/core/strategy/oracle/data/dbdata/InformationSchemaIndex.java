package org.dromara.autotable.core.strategy.oracle.data.dbdata;

import lombok.Data;
import org.dromara.autotable.core.utils.DBHelper;
@Data
public class InformationSchemaIndex {

    /**
     * 索引名称
     */
    @DBHelper.ColumnName("INDEX_NAME")
    private String indexName;

    /**
     * 表名
     */
    @DBHelper.ColumnName("TABLE_NAME")
    private String tableName;

    /**
     * 索引是否唯一（UNIQUE 或 NONUNIQUE）
     */
    @DBHelper.ColumnName("UNIQUENESS")
    private String uniqueness;

    /**
     * 索引的列名
     */
    @DBHelper.ColumnName("COLUMN_NAME")
    private String columnName;

    /**
     * 索引列的位置（顺序）
     */
    @DBHelper.ColumnName("COLUMN_POSITION")
    private Integer columnPosition;

    /**
     * 排序方式（ASC 或 DESC）
     */
    @DBHelper.ColumnName("DESCEND")
    private String descend;

    public boolean isUnique() {
        return "UNIQUE".equalsIgnoreCase(uniqueness);
    }
}
