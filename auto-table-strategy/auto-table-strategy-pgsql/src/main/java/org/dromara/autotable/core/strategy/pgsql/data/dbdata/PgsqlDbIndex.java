package org.dromara.autotable.core.strategy.pgsql.data.dbdata;

import lombok.Data;
import org.dromara.autotable.core.utils.DBHelper;

/**
 * pgsql数据库，索引信息
 */
@Data
public class PgsqlDbIndex {

    /**
     * 索引注释
     */
    @DBHelper.ColumnName("description")
    private String description;

    /**
     * 索引所属的模式（命名空间）名称。
     */
    @DBHelper.ColumnName("schemaname")
    private String schemaname;

    /**
     * 索引所属的表名称。
     */
    @DBHelper.ColumnName("tablename")
    private String tablename;

    /**
     * 索引名
     */
    @DBHelper.ColumnName("indexname")
    private String indexname;

    /**
     *
     */
    @DBHelper.ColumnName("tablespace")
    private String tablespace;

    /**
     * 索引创建语句
     */
    @DBHelper.ColumnName("indexdef")
    private String indexdef;
}
