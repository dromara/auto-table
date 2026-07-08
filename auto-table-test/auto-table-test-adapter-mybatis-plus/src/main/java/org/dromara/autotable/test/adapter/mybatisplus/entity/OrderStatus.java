package org.dromara.autotable.test.adapter.mybatisplus.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 测试枚举（@EnumValue）
 */
public enum OrderStatus {

    PENDING(0),
    PAID(1),
    CANCELLED(2);

    @EnumValue
    private final int code;

    OrderStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
