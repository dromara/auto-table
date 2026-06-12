package org.dromara.autotable.test.springboot4;

import lombok.Data;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.PrimaryKey;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 测试实体类 - 订单（用于测试复杂字段类型）
 */
@Data
@AutoTable(value = "test_order")
public class TestOrder implements Serializable {

    @PrimaryKey
    @AutoColumn(comment = "主键ID", sort = 0)
    private Long id;

    @AutoColumn(comment = "订单号", sort = 1, length = 64)
    private String orderNo;

    @AutoColumn(comment = "用户ID", sort = 2)
    private Long userId;

    @AutoColumn(comment = "订单金额", sort = 3, length = 10, decimalLength = 2)
    private BigDecimal amount;

    @AutoColumn(comment = "订单状态", sort = 4)
    private Integer orderStatus;

    @AutoColumn(comment = "备注", sort = 5, length = 500)
    private String remark;

    @AutoColumn(comment = "下单日期", sort = 6)
    private LocalDate orderDate;

    @AutoColumn(comment = "创建时间", sort = 7)
    private java.time.LocalDateTime createTime;
}
