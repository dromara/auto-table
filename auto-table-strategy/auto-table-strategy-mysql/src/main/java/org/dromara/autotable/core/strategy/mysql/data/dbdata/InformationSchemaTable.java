package org.dromara.autotable.core.strategy.mysql.data.dbdata;

import lombok.Data;
import org.dromara.autotable.core.utils.DBHelper;

/**
 * 数据库表查询的表信息
 * @author don
 */
@Data
public class InformationSchemaTable {

    /**
     * 数据表登记目录
     */
    @DBHelper.ColumnName("table_catalog")
    private String tableCatalog;
    /**
     * 数据表所属的数据库名
     */
    @DBHelper.ColumnName("table_schema")
    private String tableSchema;
    /**
     * 表名称
     */
    @DBHelper.ColumnName("table_name")
    private String tableName;
    /**
     * 表类型[system view|base table]
     */
    @DBHelper.ColumnName("table_type")
    private String tableType;
    /**
     * 使用的数据库引擎[MyISAM|CSV|InnoDB]
     */
    @DBHelper.ColumnName("engine")
    private String engine;
    /**
     * 版本，默认值10
     */
    @DBHelper.ColumnName("version")
    private Long version;
    /**
     * 行格式[Compact|Dynamic|Fixed]
     */
    @DBHelper.ColumnName("row_format")
    private String rowFormat;
    /**
     * 表里所存多少行数据
     */
    @DBHelper.ColumnName("table_rows")
    private Long tableRows;
    /**
     * 平均行长度
     */
    @DBHelper.ColumnName("avg_row_length")
    private Long avgRowLength;
    /**
     * 数据长度
     */
    @DBHelper.ColumnName("data_length")
    private Long dataLength;
    /**
     * 最大数据长度
     */
    @DBHelper.ColumnName("max_data_length")
    private Long maxDataLength;
    /**
     * 索引长度
     */
    @DBHelper.ColumnName("index_length")
    private Long indexLength;
    /**
     * 空间碎片
     */
    @DBHelper.ColumnName("data_free")
    private Long dataFree;
    /**
     * 做自增主键的自动增量当前值
     */
    @DBHelper.ColumnName("auto_increment")
    private Long autoIncrement;
    /**
     * 表的创建时间 (时间格式，涉及了兼容性问题[https://gitee.com/dromara/auto-table/issues/IC3SDB]，注释掉，不查询)
     */
    // @DBHelper.ColumnName("create_time")
    // private Date createTime;
    /**
     * 表的更新时间 (时间格式，涉及了兼容性问题[https://gitee.com/dromara/auto-table/issues/IC3SDB]，注释掉，不查询)
     */
    // @DBHelper.ColumnName("update_time")
    // private Date updateTime;
    /**
     * 表的检查时间 (时间格式，涉及了兼容性问题[https://gitee.com/dromara/auto-table/issues/IC3SDB]，注释掉，不查询)
     */
    // @DBHelper.ColumnName("check_time")
    // private Date checkTime;
    /**
     * 表的字符校验编码集
     */
    @DBHelper.ColumnName("table_collation")
    private String tableCollation;
    /**
     * 校验和
     */
    @DBHelper.ColumnName("checksum")
    private Long checksum;
    /**
     * 	创建选项
     */
    @DBHelper.ColumnName("create_options")
    private String createOptions;
    /**
     * 表的注释、备注
     */
    @DBHelper.ColumnName("table_comment")
    private String tableComment;
}
