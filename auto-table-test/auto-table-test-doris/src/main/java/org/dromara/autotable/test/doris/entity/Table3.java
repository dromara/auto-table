package org.dromara.autotable.test.doris.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.ColumnComment;
import org.dromara.autotable.annotation.PrimaryKey;
import org.dromara.autotable.annotation.doris.DorisPartition;
import org.dromara.autotable.annotation.doris.DorisTable;
import org.dromara.autotable.core.constants.DatabaseDialect;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@FieldNameConstants
@AutoTable(value = "doris_table3", comment = "创建一个主键唯一模型的表，设置初始存储介质和冷却时间,使用@PrimaryKey定义unique_key", strategy = DatabaseDialect.Doris)
@DorisTable(
        // unique_key = {Table3.Fields.k1, Table3.Fields.k2},
        distributed_by_hash = {Table3.Fields.k1, Table3.Fields.k2},
        distributed_buckets = 32,
        properties = {
                "replication_allocation=tag.location.default:1",
                // "storage_medium=SSD",
                "storage_medium=HDD",
                "storage_cooldown_time=2025-06-04 00:00:00"
        }
)

public class Table3 {

    @PrimaryKey
    private Long k1;

    @PrimaryKey
    @AutoColumn(type = "largeint")
    private Long k2;

    @AutoColumn(length = 2048)
    private String v1;

    @AutoColumn(defaultValue = "10")
    private Integer v2;

}
