package org.dromara.autotable.core.strategy.h2.data.dbdata;

import lombok.Data;

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
    private String indexCatalog;
    /**
     * schema名
     */
    private String indexSchema;
    /**
     * 索引名
     */
    private String indexName;
    /**
     *
     */
    private String tableCatalog;
    /**
     * schema名
     */
    private String tableSchema;
    /**
     * 表名
     */
    private String tableName;
    /**
     * 列名
     */
    private String columnName;
    /**
     * 列的序号
     */
    private Integer ordinalPosition;
    /**
     * 列的排序方式: ASC, DESC
     */
    private String orderingSpecification;
    /**
     *
     */
    private String nullOrdering;
    /**
     * 是否是唯一索引
     */
    private Boolean isUnique;
    /**
     * 备注
     */
    private String remarks;
}
