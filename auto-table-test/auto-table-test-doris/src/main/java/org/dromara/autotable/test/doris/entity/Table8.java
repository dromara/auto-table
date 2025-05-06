package org.dromara.autotable.test.doris.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.doris.DorisDynamicPartition;
import org.dromara.autotable.annotation.doris.DorisTable;
import org.dromara.autotable.annotation.doris.emuns.DorisTimeUnit;
import org.dromara.autotable.core.constants.DatabaseDialect;

import java.time.LocalDate;
import java.util.Date;

/**
 * 该表每天提前创建 3 天的分区，并删除 3 天前的分区。例如今天为 2020-01-08，则会创建分区名为p20200108, p20200109, p20200110, p20200111 的分区。
 */
@Data
@FieldNameConstants
@AutoTable(value = "doris_table8", comment = "创建一个动态分区表", dialect = DatabaseDialect.Doris)
@DorisTable(
        duplicate_key = {Table8.Fields.k1, Table8.Fields.k2, Table8.Fields.k3},
        distributed_by_hash = {Table8.Fields.k2},
        distributed_buckets = 32,
        partition_by_range = {Table8.Fields.k1},
        dynamic_partition = @DorisDynamicPartition(
                enable = true,
                time_unit = DorisTimeUnit.day,
                start = "-3",
                end = "3",
                prefix = "p",
                buckets = "32"
        ),
        properties = {
                "replication_allocation=tag.location.default:1",
        }
)

public class Table8 {

    private LocalDate k1;

    private Integer k2;

    private Integer k3;

    @AutoColumn(length = 2048)
    private String v1;

    @AutoColumn(defaultValue = "2014-02-04 15:36:00")
    private Date v2;

    @AutoColumn(defaultValue = "0")
    private Integer v3;

}
