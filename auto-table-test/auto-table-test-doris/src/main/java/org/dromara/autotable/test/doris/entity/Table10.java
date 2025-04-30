package org.dromara.autotable.test.doris.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.doris.DorisTable;
import org.dromara.autotable.core.constants.DatabaseDialect;

import java.math.BigDecimal;


@Data
@FieldNameConstants
@AutoTable(value = "doris_table10", comment = "通过 replication_allocation 属性设置表的副本", dialect = DatabaseDialect.Doris)
@DorisTable(
        duplicate_key = {
                Table10.Fields.k1,
                Table10.Fields.k2,
        },
        distributed_by_hash = {Table10.Fields.k1},
        distributed_buckets = 32,
        properties = {
                "replication_allocation=tag.location.default:1",
                // "replication_allocation=tag.location.group_a:1,tag.location.group_b:2",
        }
)

public class Table10 {

    private Integer k1;

    @AutoColumn(type = "decimal", length = 10, decimalLength = 2, defaultValue = "10.5")
    private BigDecimal k2;

}
