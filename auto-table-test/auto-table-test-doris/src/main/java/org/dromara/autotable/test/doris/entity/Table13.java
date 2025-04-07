package org.dromara.autotable.test.doris.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.doris.DorisPartition;
import org.dromara.autotable.annotation.doris.DorisTable;
import org.dromara.autotable.annotation.doris.emuns.DorisTimeUnit;
import org.dromara.autotable.core.constants.DatabaseDialect;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
@FieldNameConstants
@AutoTable(value = "doris_table13", comment = "批量创建分区", strategy = DatabaseDialect.Doris)
@DorisTable(
        duplicate_key = {
                Table13.Fields.k1,
                Table13.Fields.k2,
        },
        distributed_by_hash = {Table13.Fields.k1},
        distributed_buckets = 32,
        partition_by_range = {Table13.Fields.k1},
        partitions = {
                @DorisPartition(from = "2000-11-14", to = "2021-11-14", interval = 1, unit = DorisTimeUnit.year),
                @DorisPartition(from = "2021-11-14", to = "2022-11-14", interval = 1, unit = DorisTimeUnit.month),
                @DorisPartition(from = "2022-11-14", to = "2023-01-03", interval = 1, unit = DorisTimeUnit.week),
                @DorisPartition(from = "2023-01-03", to = "2023-01-14", interval = 1, unit = DorisTimeUnit.day),
                @DorisPartition(partition = "p_20230114", values_left_include = "2023-01-14", values_right_exclude = "2023-01-15"),
        },
        properties = {
                "replication_allocation=tag.location.default:1",
                // "replication_allocation=tag.location.group_a:1,tag.location.group_b:2",
        }
)

public class Table13 {
    private LocalDate k1;
    private Integer k2;
    private String v1;
}
