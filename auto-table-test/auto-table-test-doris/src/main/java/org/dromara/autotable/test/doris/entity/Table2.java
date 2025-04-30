package org.dromara.autotable.test.doris.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.ColumnComment;
import org.dromara.autotable.annotation.doris.DorisPartition;
import org.dromara.autotable.annotation.doris.DorisTable;
import org.dromara.autotable.core.constants.DatabaseDialect;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@FieldNameConstants
@AutoTable(value = "doris_table2", comment = "创建一个明细模型的表，分区，指定排序列，设置副本数为 1", dialect = DatabaseDialect.Doris)
@DorisTable(
        duplicate_key = {Table2.Fields.k1, Table2.Fields.k2},
        partition_by_range = {Table2.Fields.k1},
        partitions = {
                @DorisPartition(partition = "p1", values_less_than = "2020-02-01"),
                @DorisPartition(partition = "p2", values_less_than = "2020-03-01"),
                @DorisPartition(partition = "p3", values_less_than = "2020-04-01")
        },
        distributed_by_hash = {Table2.Fields.k1},
        distributed_buckets = 32,
        properties = {
                "replication_num=1"
        }
)

public class Table2 {

    private LocalDate k1;

    @AutoColumn(type = "decimal", length = 10, decimalLength = 2, defaultValue = "10.5")
    private BigDecimal k2;

    @ColumnComment("string column")
    private String k3;

    @AutoColumn(notNull = true, defaultValue = "1", comment = "int column")
    private Integer k4;

}
