package org.dromara.autotable.strategy.sqlserver.data.dbdata;

import lombok.Data;
import org.dromara.autotable.core.utils.DBHelper;

/**
 * SQLServer 数据库，索引信息。
 *
 * @author don
 */
@Data
public class SqlServerDbIndex {

    /**
     * 索引注释
     */
    @DBHelper.ColumnName("description")
    private String description;
    /**
     * 索引名
     */
    @DBHelper.ColumnName("indexName")
    private String indexName;
    /**
     * 是否唯一索引（"YES"/"NO"，SQL 中 CASE 而成）
     */
    @DBHelper.ColumnName("isUnique")
    private String isUnique;
    /**
     * 索引列名拼接，逗号分隔，形如 col1,col2
     */
    @DBHelper.ColumnName("indexColumns")
    private String indexColumns;
}
