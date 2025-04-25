package org.dromara.autotable.core.strategy.h2.data.dbdata;

import lombok.Data;
import org.dromara.autotable.core.utils.DBHelper;

/**
 * 数据库表查询的索引信息
 *   {
 *     "INDEX_CATALOG": "H2.DB",
 *     "INDEX_SCHEMA": "MY_TEST",
 *     "INDEX_NAME": "AUTO_IDX_SYS_USER_NAME",
 *     "TABLE_CATALOG": "H2.DB",
 *     "TABLE_SCHEMA": "MY_TEST",
 *     "TABLE_NAME": "SYS_USER",
 *     "COLUMN_NAME": "NAME",
 *     "ORDINAL_POSITION": 1,
 *     "ORDERING_SPECIFICATION": "ASC",
 *     "NULL_ORDERING": "FIRST",
 *     "IS_UNIQUE": false
 *   },
 *
 * @author don
 */
@Data
public class InformationSchemaIndexes {
    /**
     *
     */
    @DBHelper.ColumnName("INDEX_CATALOG")
    private String indexCatalog;

    /**
     * schema名
     */
    @DBHelper.ColumnName("INDEX_SCHEMA")
    private String indexSchema;

    /**
     * 索引名
     */
    @DBHelper.ColumnName("INDEX_NAME")
    private String indexName;

    /**
     *
     */
    @DBHelper.ColumnName("TABLE_CATALOG")
    private String tableCatalog;

    /**
     * schema名
     */
    @DBHelper.ColumnName("TABLE_SCHEMA")
    private String tableSchema;

    /**
     * 表名
     */
    @DBHelper.ColumnName("TABLE_NAME")
    private String tableName;

    /**
     * 列名
     */
    @DBHelper.ColumnName("COLUMN_NAME")
    private String columnName;

    /**
     * 列的序号
     */
    @DBHelper.ColumnName("ORDINAL_POSITION")
    private Integer ordinalPosition;

    /**
     * 列的排序方式: ASC, DESC
     */
    @DBHelper.ColumnName("ORDERING_SPECIFICATION")
    private String orderingSpecification;

    /**
     *
     */
    @DBHelper.ColumnName("NULL_ORDERING")
    private String nullOrdering;

    /**
     * 是否是唯一索引
     */
    @DBHelper.ColumnName("IS_UNIQUE")
    private Boolean isUnique;

    /**
     * 备注
     */
    @DBHelper.ColumnName("REMARKS")
    private String remarks;

}
