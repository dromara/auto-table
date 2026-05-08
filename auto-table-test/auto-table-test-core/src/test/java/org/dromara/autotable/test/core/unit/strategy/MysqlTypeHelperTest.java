package org.dromara.autotable.test.core.unit.strategy;

import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.strategy.mysql.data.MysqlTypeHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MysqlTypeHelper 单元测试
 */
public class MysqlTypeHelperTest {

    @Test
    void testGetFullType_withLength() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("varchar", 255, null, java.util.Collections.emptyList());
        assertEquals("varchar(255)", type.getDefaultFullType());
    }

    @Test
    void testGetFullType_withLengthAndDecimal() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("decimal", 10, 2, java.util.Collections.emptyList());
        assertEquals("decimal(10,2)", type.getDefaultFullType());
    }

    @Test
    void testIsCharString_withVarchar() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("varchar", 255, null, java.util.Collections.emptyList());
        assertTrue(MysqlTypeHelper.isCharString(type));
    }

    @Test
    void testIsCharString_withInt() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("int", null, null, java.util.Collections.emptyList());
        assertFalse(MysqlTypeHelper.isCharString(type));
    }

    @Test
    void testIsDateTime_withDateTime() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("datetime", null, null, java.util.Collections.emptyList());
        assertTrue(MysqlTypeHelper.isDateTime(type));
    }

    @Test
    void testIsDateTime_withTimestamp() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("timestamp", null, null, java.util.Collections.emptyList());
        assertFalse(MysqlTypeHelper.isDateTime(type));
    }

    @Test
    void testNeedStringCompatibility_withText() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("text", null, null, java.util.Collections.emptyList());
        assertTrue(MysqlTypeHelper.needStringCompatibility(type));
    }

    @Test
    void testNeedStringCompatibility_withInt() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("int", null, null, java.util.Collections.emptyList());
        assertFalse(MysqlTypeHelper.needStringCompatibility(type));
    }

    @Test
    void testIsBoolean_withBit() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("bit", 1, null, java.util.Collections.emptyList());
        assertTrue(MysqlTypeHelper.isBoolean(type));
    }

    @Test
    void testIsBoolean_withTinyint() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("tinyint", 1, null, java.util.Collections.emptyList());
        assertFalse(MysqlTypeHelper.isBoolean(type));
    }

    @Test
    void testIsNumber_withInt() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("int", null, null, java.util.Collections.emptyList());
        assertTrue(MysqlTypeHelper.isNumber(type));
    }

    @Test
    void testIsNumber_withVarchar() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("varchar", 255, null, java.util.Collections.emptyList());
        assertFalse(MysqlTypeHelper.isNumber(type));
    }

    @Test
    void testIsEnum_withEnum() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("enum", null, null, java.util.Arrays.asList("A", "B"));
        assertTrue(MysqlTypeHelper.isEnum(type));
    }

    @Test
    void testIsEnum_withVarchar() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("varchar", 255, null, java.util.Collections.emptyList());
        assertFalse(MysqlTypeHelper.isEnum(type));
    }

    @Test
    void testIsFloatNumber_withDecimal() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("decimal", 10, 2, java.util.Collections.emptyList());
        assertTrue(MysqlTypeHelper.isFloatNumber(type));
    }

    @Test
    void testIsFloatNumber_withInt() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("int", null, null, java.util.Collections.emptyList());
        assertFalse(MysqlTypeHelper.isFloatNumber(type));
    }

    @Test
    void testIsNoLengthNumber_withBigint() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("bigint", null, null, java.util.Collections.emptyList());
        assertTrue(MysqlTypeHelper.isNoLengthNumber(type));
    }

    @Test
    void testIsNoLengthNumber_withVarchar() {
        DatabaseTypeAndLength type = new DatabaseTypeAndLength("varchar", 255, null, java.util.Collections.emptyList());
        assertFalse(MysqlTypeHelper.isNoLengthNumber(type));
    }
}
