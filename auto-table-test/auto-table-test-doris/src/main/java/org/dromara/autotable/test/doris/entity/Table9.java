package org.dromara.autotable.test.doris.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.doris.DorisColumn;
import org.dromara.autotable.annotation.doris.DorisRollup;
import org.dromara.autotable.annotation.doris.DorisTable;
import org.dromara.autotable.annotation.doris.emuns.AggregateFun;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.constants.DatabaseDialect;

import java.time.LocalDate;


@Data
@FieldNameConstants
@AutoTable(value = "doris_table9", comment = "创建一个带有物化视图（ROLLUP）的表", dialect = DatabaseDialect.Doris)
@DorisTable(
        aggregate_key = {
                Table9.Fields.eventDay,
                Table9.Fields.siteid,
                Table9.Fields.citycode,
                Table9.Fields.username
        },
        distributed_by_hash = {Table9.Fields.siteid},
        distributed_buckets = 10,
        properties = {
                "replication_allocation=tag.location.default:1",
        },
        rollup = {
                @DorisRollup(name = "r1", columns = {Table9.Fields.eventDay, Table9.Fields.siteid}),
                @DorisRollup(name = "r2", columns = {Table9.Fields.eventDay, Table9.Fields.citycode}),
                @DorisRollup(name = "r3", columns = {Table9.Fields.eventDay}),
        }
)

public class Table9 {

    private LocalDate eventDay;

    private Integer siteid;

    private Integer citycode;

    @AutoColumn(length = 2048, notNull = true, defaultValueType = DefaultValueEnum.EMPTY_STRING)
    private String username;

    @AutoColumn(defaultValue = "0", notNull = true)
    @DorisColumn(aggregateFun = AggregateFun.sum)
    private Integer pv;

}
