package org.dromara.autotable.test.core.unit.converter;

import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DatabaseTypeAndLength 单元测试
 */
public class DatabaseTypeAndLengthTest {

    @Test
    void testGetDefaultFullType_withTypeOnly() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("int", null, null, Collections.emptyList());
        assertEquals("int", type.getDefaultFullType());
    }

    @Test
    void testGetDefaultFullType_withLength() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("varchar", 255, null, Collections.emptyList());
        assertEquals("varchar(255)", type.getDefaultFullType());
    }

    @Test
    void testGetDefaultFullType_withLengthAndDecimal() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("decimal", 10, 2, Collections.emptyList());
        assertEquals("decimal(10,2)", type.getDefaultFullType());
    }

    @Test
    void testGetDefaultFullType_withZeroLength() {
        // 长度为0时，代码逻辑中 length >= 0 允许0值
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("int", 0, null, Collections.emptyList());
        assertEquals("int(0)", type.getDefaultFullType());
    }

    @Test
    void testGetDefaultFullType_withNegativeLength() {
        // 负数长度时，不应被设置
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("int", -1, null, Collections.emptyList());
        assertEquals("int", type.getDefaultFullType());
    }
}
