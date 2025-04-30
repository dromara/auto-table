package org.dromara.autotable.test.doris.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.doris.DorisColumn;
import org.dromara.autotable.annotation.doris.DorisIndex;
import org.dromara.autotable.annotation.doris.DorisTable;
import org.dromara.autotable.annotation.doris.emuns.AggregateFun;
import org.dromara.autotable.annotation.doris.emuns.DorisIndexType;
import org.dromara.autotable.core.constants.DatabaseDialect;

import java.math.BigDecimal;

@Data
@FieldNameConstants
@AutoTable(value = "doris_table7", comment = "创建一个带有倒排索引以及 bloom filter 索引的表", strategy = DatabaseDialect.Doris)
@DorisTable(
        aggregate_key = {Table7.Fields.k1, Table7.Fields.k2},
        distributed_by_hash = {Table7.Fields.k1},
        distributed_buckets = 32,
        properties = {
                "replication_allocation=tag.location.default:1",
                "bloom_filter_columns=k1",
        },
        indexes = {
                @DorisIndex(column = Table7.Fields.k1, using = DorisIndexType.inverted, comment = "my first index"),
        }
)

public class Table7 {

    private Integer k1;

    @AutoColumn(type = "decimal", length = 10, decimalLength = 2, defaultValue = "10.5")
    private BigDecimal k2;

    @DorisColumn(aggregateFun = AggregateFun.replace)
    private String v1;

    @DorisColumn(aggregateFun = AggregateFun.max)
    private Integer v2;

    @AutoColumn(defaultValue = "100")
    @DorisColumn(aggregateFun = AggregateFun.sum)
    private Integer v3;

}
