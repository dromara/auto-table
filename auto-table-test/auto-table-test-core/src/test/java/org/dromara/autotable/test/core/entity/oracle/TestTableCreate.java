package org.dromara.autotable.test.core.entity.oracle;

import lombok.Data;
import org.dromara.autotable.annotation.*;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;

@Data
@AutoTable
public class TestTableCreate {
    @ColumnComment("电话")
    @Index(type = IndexTypeEnum.UNIQUE)
    @ColumnType(length = 20)
    private String phone;
}
