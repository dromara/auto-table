package org.dromara.autotable.strategy.sqlite.data.dbdata;

import lombok.Data;

/**
 * sqlite记录列数据
 * @author don
 */
@Data
public class SqliteColumns {

    /**
     * 列的序号（从0开始）
     */
    private Integer cid;

    /**
     * 列名
     */
    private String name;

    /**
     * 列的数据类型
     */
    private String type;

    /**
     * 是否不允许为空（1表示NOT NULL，0表示可以为空）
     */
    private Integer notnull;

    /**
     * 默认值
     */
    private String dfltValue;

    /**
     * 是否是主键的一部分（>0表示是主键，值表示在主键中的位置）
     */
    private Integer pk;
}
