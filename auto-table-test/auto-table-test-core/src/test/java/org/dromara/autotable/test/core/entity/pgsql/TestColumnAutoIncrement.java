package org.dromara.autotable.test.core.entity.pgsql;

import lombok.Data;
import org.dromara.autotable.annotation.AutoIncrement;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.ColumnComment;

/**
 * @author don
 */
@Data
@AutoTable
public class TestColumnAutoIncrement {

    @ColumnComment("电话")
    @AutoIncrement
    private Integer num;
}
