package org.dromara.autotable.test.doris.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.doris.DorisColumn;
import org.dromara.autotable.annotation.doris.DorisPartition;
import org.dromara.autotable.annotation.doris.DorisTable;
import org.dromara.autotable.annotation.doris.emuns.AggregateFun;
import org.dromara.autotable.core.constants.DatabaseDialect;

import java.time.LocalDate;

@Data
@FieldNameConstants
@AutoTable(value = "doris_table4", comment = "创建一个聚合模型表，使用固定范围分区描述", strategy = DatabaseDialect.Doris)
@DorisTable(
        aggregate_key = {Table4.Fields.k1, Table4.Fields.k2, Table4.Fields.k3},
        distributed_by_hash = {Table4.Fields.k2},
        distributed_buckets = 32,
        partition_by_range = {Table4.Fields.k1, Table4.Fields.k2, Table4.Fields.k3},
        partitions = {
                @DorisPartition(partition = "p1", values_left_include = {"2014-01-01", "10", "200"}, values_right_exclude = {"2014-01-02", "20", "300"}),
                @DorisPartition(partition = "p2", values_left_include = {"2014-06-01", "100", "200"}, values_right_exclude = {"2014-07-01", "100", "300"}),

        },
        properties = {
                "replication_allocation=tag.location.default:1",
        }
)

public class Table4 {

    private LocalDate k1;

    private Integer k2;

    private Integer k3;

    @AutoColumn(length = 2048)
    @DorisColumn(aggregateFun = AggregateFun.replace)
    private String v1;

    @AutoColumn(defaultValue = "1")
    @DorisColumn(aggregateFun = AggregateFun.sum)
    private Integer v2;

}
