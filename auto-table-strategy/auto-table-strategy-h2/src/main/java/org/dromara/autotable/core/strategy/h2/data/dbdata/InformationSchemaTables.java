package org.dromara.autotable.core.strategy.h2.data.dbdata;

import lombok.Data;

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
    private String tableCatalog;
    /**
     * schema名称
     */
    private String tableSchema;
    /**
     * 表名
     */
    private String tableName;
    /**
     * 例：BASE TABLE
     */
    private String tableType;
    /**
     * 例：YES
     */
    private String isInsertableInto;
    /**
     * 例：
     */
    private String commitAction;
    /**
     * 例：CACHED
     */
    private String storageType;
    /**
     * 表注释
     */
    private String remarks;
    /**
     * 例：9
     */
    private String lastModification;
    /**
     * 例：org.h2.mvstore.db.MVTable
     */
    private String tableClass;
    /**
     * 例：0
     */
    private String rowCountEstimate;
}
