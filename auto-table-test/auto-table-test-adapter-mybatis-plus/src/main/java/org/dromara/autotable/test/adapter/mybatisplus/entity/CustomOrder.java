package org.dromara.autotable.test.adapter.mybatisplus.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import org.dromara.autotable.adapter.mybatisplus.spring.annotation.Column;
import org.dromara.autotable.adapter.mybatisplus.spring.annotation.ColumnId;
import org.dromara.autotable.adapter.mybatisplus.spring.annotation.Table;
import org.dromara.autotable.annotation.AutoTable;

/**
 * 自定义注解实体，验证 starter 扩展层（@AliasFor 合并）
 */
@Data
@AutoTable
@Table(value = "custom_order", comment = "订单表")
public class CustomOrder {

    @ColumnId(comment = "主键ID")
    private Long id;

    @Column(value = "order_no", comment = "订单号", notNull = true)
    private String orderNo;

    @Column(comment = "金额", type = "DECIMAL", length = 10, decimalLength = 2)
    private String amount;

    @Column(comment = "状态")
    private OrderStatus status;

    @TableField(exist = false)
    private String tempData;
}
