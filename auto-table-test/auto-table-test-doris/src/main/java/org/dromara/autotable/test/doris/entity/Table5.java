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

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@FieldNameConstants
@AutoTable(value = "doris_table5", comment = "创建一个包含 HLL 和 BITMAP 列类型的聚合模型表", strategy = DatabaseDialect.Doris)
@DorisTable(
        aggregate_key = {Table5.Fields.k1, Table5.Fields.k2},
        distributed_by_hash = {Table5.Fields.k1},
        distributed_buckets = 32,
        properties = {
                "replication_allocation=tag.location.default:1",
        }
)

public class Table5 {

    private Integer k1;

    @AutoColumn(type = "decimal", length = 10, decimalLength = 2, defaultValue = "10.5")
    private BigDecimal k2;


    @AutoColumn(type = "hll", notNull = true)
    @DorisColumn(aggregateFun = AggregateFun.hll_union)
    private byte[] v1;

    @AutoColumn(type = "bitmap", notNull = true)
    @DorisColumn(aggregateFun = AggregateFun.bitmap_union)
    private byte[] v2;

}
