package org.dromara.autotable.test.doris.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.doris.DorisPartition;
import org.dromara.autotable.annotation.doris.DorisTable;
import org.dromara.autotable.core.constants.DatabaseDialect;


@Data
@FieldNameConstants
@AutoTable(value = "doris_table15", comment = "list分区表", dialect = DatabaseDialect.Doris)
@DorisTable(
        duplicate_key = {
                Table15.Fields.k1,
                Table15.Fields.k2,
        },
        distributed_by_hash = {Table15.Fields.k1},
        distributed_buckets = 32,
        partition_by_list = {Table15.Fields.k1, Table15.Fields.k2},
        partitions = {
                @DorisPartition(partition = "p1_city", values_in = {"1", "北京", "1", "上海"}),
                @DorisPartition(partition = "p2_city", values_in = {"2", "北京", "2", "上海"}),
                @DorisPartition(partition = "p3_city", values_in = {"3", "北京", "3", "上海"}),
        },
        properties = {
                "replication_allocation=tag.location.default:1",
                // "replication_allocation=tag.location.group_a:1,tag.location.group_b:2",
        }
)

public class Table15 {
    private String k1;
    private String k2;
    private String v1;
}
