package org.dromara.autotable.test.doris.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoIncrement;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.doris.DorisColumn;
import org.dromara.autotable.annotation.doris.DorisTable;
import org.dromara.autotable.core.constants.DatabaseDialect;

import java.time.LocalDateTime;


@Data
@FieldNameConstants
@AutoTable(value = "doris_table14", comment = "批量创建分区", strategy = DatabaseDialect.Doris)
@DorisTable(
        unique_key = {
                Table14.Fields.k1,
        },
        distributed_by_hash = {Table14.Fields.k1},
        distributed_buckets = 32,

        properties = {
                "replication_allocation=tag.location.default:1",
                // "replication_allocation=tag.location.group_a:1,tag.location.group_b:2",
        }
)

public class Table14 {
    @AutoIncrement
    @DorisColumn(autoIncrementStartValue = 100)
    private Long k1;

    @AutoColumn(defaultValue = "CURRENT_TIMESTAMP")
    @DorisColumn(onUpdateCurrentTimestamp = true)
    private LocalDateTime v1;
}
