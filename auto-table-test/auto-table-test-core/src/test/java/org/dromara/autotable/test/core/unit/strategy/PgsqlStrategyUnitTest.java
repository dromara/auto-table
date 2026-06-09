package org.dromara.autotable.test.core.unit.strategy;

import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.strategy.pgsql.PgsqlStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PgsqlStrategy 单元测试
 */
public class PgsqlStrategyUnitTest {

    @Test
    void testDatabaseDialect() {
        PgsqlStrategy strategy = new PgsqlStrategy();
        assertEquals("PostgreSQL", strategy.databaseDialect());
    }

    @Test
    void testIdentifier() {
        PgsqlStrategy strategy = new PgsqlStrategy();
        assertEquals("\"", strategy.identifier());
    }

    @Test
    void testDropTable() {
        PgsqlStrategy strategy = new PgsqlStrategy();
        IStrategy.setCurrentStrategy(strategy);
        try {
            String sql = strategy.dropTable("public", "test_table");
            assertEquals("DROP TABLE IF EXISTS \"public\".\"test_table\"", sql);
        } finally {
            IStrategy.clean();
        }
    }

    @Test
    void testDropTable_withoutSchema() {
        PgsqlStrategy strategy = new PgsqlStrategy();
        IStrategy.setCurrentStrategy(strategy);
        try {
            String sql = strategy.dropTable("", "test_table");
            assertTrue(sql.contains("\"test_table\""));
            assertTrue(sql.startsWith("DROP TABLE IF EXISTS"));
        } finally {
            IStrategy.clean();
        }
    }

    @Test
    void testTypeMapping_notEmpty() {
        PgsqlStrategy strategy = new PgsqlStrategy();
        assertFalse(strategy.typeMapping().isEmpty());
        assertTrue(strategy.typeMapping().containsKey(String.class));
        assertTrue(strategy.typeMapping().containsKey(Integer.class));
        assertTrue(strategy.typeMapping().containsKey(Long.class));
        assertTrue(strategy.typeMapping().containsKey(Boolean.class));
    }

    @Test
    void testTypeMapping_byteMapping() {
        PgsqlStrategy strategy = new PgsqlStrategy();
        assertTrue(strategy.typeMapping().containsKey(byte.class));
        assertTrue(strategy.typeMapping().containsKey(Byte.class));
        assertEquals("int2", strategy.typeMapping().get(byte.class).getTypeName());
        assertEquals("int2", strategy.typeMapping().get(Byte.class).getTypeName());
    }

    @Test
    void testTypeMapping_isUnmodifiable() {
        PgsqlStrategy strategy = new PgsqlStrategy();
        assertThrows(UnsupportedOperationException.class, () -> {
            strategy.typeMapping().put(String.class, null);
        });
    }

    @Test
    void testTypeMapping_pgsqlSpecificTypes() {
        PgsqlStrategy strategy = new PgsqlStrategy();
        // 验证 PG 特有类型映射
        assertEquals("varchar", strategy.typeMapping().get(String.class).getTypeName());
        assertEquals("int4", strategy.typeMapping().get(Integer.class).getTypeName());
        assertEquals("int8", strategy.typeMapping().get(Long.class).getTypeName());
        assertEquals("bool", strategy.typeMapping().get(Boolean.class).getTypeName());
        assertEquals("float4", strategy.typeMapping().get(Float.class).getTypeName());
        assertEquals("float8", strategy.typeMapping().get(Double.class).getTypeName());
        assertEquals("numeric", strategy.typeMapping().get(java.math.BigDecimal.class).getTypeName());
    }
}
