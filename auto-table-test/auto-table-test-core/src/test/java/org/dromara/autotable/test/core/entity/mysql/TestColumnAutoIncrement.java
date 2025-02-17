package org.dromara.autotable.test.core.entity.mysql;

import lombok.Data;
import org.dromara.autotable.annotation.AutoIncrement;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.ColumnComment;
import org.dromara.autotable.annotation.ColumnType;
import org.dromara.autotable.annotation.Index;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;

/**
 * @author don
 */
@Data
@AutoTable
public class TestColumnAutoIncrement {

    @ColumnComment("电话")
    @AutoIncrement
    @Index(type = IndexTypeEnum.UNIQUE)
    @ColumnType(length = 13)
    private Integer num;
}
