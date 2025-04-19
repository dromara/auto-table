package org.dromara.autotable.core.strategy.sqlite.data.dbdata;

import lombok.Data;

/**
 * sqlite记录列数据
 * @author don
 */
@Data
public class SqliteColumns {

    private String cid;
    private String name;
    private String type;
    private String notnull;
    private String dfltValue;
    private String pk;
}
