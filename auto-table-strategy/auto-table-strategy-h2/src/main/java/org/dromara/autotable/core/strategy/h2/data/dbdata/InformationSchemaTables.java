package org.dromara.autotable.core.strategy.h2.data.dbdata;

import lombok.Data;
import org.dromara.autotable.core.utils.DBHelper;

/**
 * 数据库表查询的表信息
 *
 * @author don
 */
@Data
public class InformationSchemaTables {

    /**
     * 例：H2.DB
     */
    @DBHelper.ColumnName("TABLE_CATALOG")
    private String tableCatalog;
    /**
     * schema名称
     */
    @DBHelper.ColumnName("TABLE_SCHEMA")
    private String tableSchema;
    /**
     * 表名
     */
    @DBHelper.ColumnName("TABLE_NAME")
    private String tableName;
    /**
     * 例：BASE TABLE
     */
    @DBHelper.ColumnName("TABLE_TYPE")
    private String tableType;
    /**
     * 例：YES
     */
    @DBHelper.ColumnName("IS_INSERTABLE_INTO")
    private String isInsertableInto;
    /**
     * 例：
     */
    @DBHelper.ColumnName("COMMIT_ACTION")
    private String commitAction;
    /**
     * 例：CACHED
     */
    @DBHelper.ColumnName("STORAGE_TYPE")
    private String storageType;
    /**
     * 表注释
     */
    @DBHelper.ColumnName("REMARKS")
    private String remarks;
    /**
     * 例：9
     */
    @DBHelper.ColumnName("LAST_MODIFICATION")
    private String lastModification;
    /**
     * 例：org.h2.mvstore.db.MVTable
     */
    @DBHelper.ColumnName("TABLE_CLASS")
    private String tableClass;
    /**
     * 例：0
     */
    @DBHelper.ColumnName("ROW_COUNT_ESTIMATE")
    private String rowCountEstimate;
}
