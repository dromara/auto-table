package org.dromara.autotable.test.core.entity.mysql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.test.core.BaseIDEntity;

@EqualsAndHashCode(callSuper = true)
@Data
@AutoTable
public class TestColumnSort extends BaseIDEntity<String, String> {
    @AutoColumn
    private String sortColumn;
}
