package org.dromara.autotable.test.doris.entity;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoIncrement;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.ColumnComment;
import org.dromara.autotable.annotation.doris.DorisColumn;
import org.dromara.autotable.annotation.doris.DorisTable;
import org.dromara.autotable.core.constants.DatabaseDialect;

import java.math.BigDecimal;

@Data
@FieldNameConstants
@AutoTable(value = "doris_table1", comment = "创建一个明细模型的表", dialect = DatabaseDialect.Doris)
@DorisTable(
        duplicate_key = {Table1.Fields.k1, Table1.Fields.k2},
        properties = {
                "replication_num=1"
        }
)
public class Table1 {

    @AutoIncrement
    @AutoColumn(type = "bigint")
    @DorisColumn(autoIncrementStartValue = 100)
    private Integer k1;

    @AutoColumn(type = "decimal", length = 10, decimalLength = 2, defaultValue = "10.5")
    private BigDecimal k2;

    @ColumnComment("string column")
    private String k3;

    @AutoColumn(notNull = true)
    private String k4;

}
