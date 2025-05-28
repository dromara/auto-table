package org.dromara.autotable.core.strategy.oracle.data.dbdata;

import lombok.Data;
import org.dromara.autotable.core.utils.DBHelper;

import java.util.Date;

@Data
public class InformationSchemaTable {

    /** 对象名称（如表、视图、索引、过程等） */
    @DBHelper.ColumnName("OBJECT_NAME")
    private String objectName;

    /** 对象类型，例如 TABLE、VIEW、INDEX、SEQUENCE、PROCEDURE 等 */
    @DBHelper.ColumnName("OBJECT_TYPE")
    private String objectType;

    /** 对象的创建时间 */
    @DBHelper.ColumnName("CREATED")
    private Date created;

    /** 对象最近一次 DDL 操作（如 ALTER、TRUNCATE）的时间 */
    @DBHelper.ColumnName("LAST_DDL_TIME")
    private Date lastDdlTime;

    /** 对象的状态，VALID（有效）或 INVALID（无效） */
    @DBHelper.ColumnName("STATUS")
    private String status;

    /** 是否为临时对象，Y 表示临时，N 表示非临时 */
    @DBHelper.ColumnName("TEMPORARY")
    private String temporary;

    /**表备注 */
    @DBHelper.ColumnName("COMMENTS")
    private String comments;
}
