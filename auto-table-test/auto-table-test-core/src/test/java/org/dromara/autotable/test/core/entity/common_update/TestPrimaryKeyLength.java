package org.dromara.autotable.test.core.entity.common_update;

import lombok.Data;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.PrimaryKey;

@Data
@AutoTable
public class TestPrimaryKeyLength {

    @PrimaryKey
    @AutoColumn(value = "event_id", length = 64, comment = "事件id")
    private String eventId;
}
