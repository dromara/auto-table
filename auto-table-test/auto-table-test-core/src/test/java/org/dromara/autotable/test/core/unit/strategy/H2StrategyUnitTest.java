package org.dromara.autotable.strategy.h2;

import org.dromara.autotable.core.strategy.IStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * H2Strategy 单元测试
 */
public class H2StrategyUnitTest {

    @Test
    void testDatabaseDialect() {
        H2Strategy strategy = new H2Strategy();
        assertEquals("H2", strategy.databaseDialect());
    }

    @Test
    void testIdentifier() {
        H2Strategy strategy = new H2Strategy();
        assertEquals("\"", strategy.identifier());
    }

    @Test
    void testDropTable() {
        H2Strategy strategy = new H2Strategy();
        IStrategy.setCurrentStrategy(strategy);
        try {
            String sql = strategy.dropTable("PUBLIC", "test_table");
            assertEquals("DROP TABLE IF EXISTS \"PUBLIC\".\"test_table\" CASCADE", sql);
        } finally {
            IStrategy.clean();
        }
    }

    @Test
    void testDropTable_withoutSchema() {
        H2Strategy strategy = new H2Strategy();
        IStrategy.setCurrentStrategy(strategy);
        try {
            String sql = strategy.dropTable(null, "test_table");
            assertEquals("DROP TABLE IF EXISTS \"test_table\" CASCADE", sql);
        } finally {
            IStrategy.clean();
        }
    }

    @Test
    void testWrapIdentifier() {
        H2Strategy strategy = new H2Strategy();
        IStrategy.setCurrentStrategy(strategy);
        try {
            assertEquals("\"test\"", strategy.wrapIdentifier("test"));
        } finally {
            IStrategy.clean();
        }
    }

    @Test
    void testConcatWrapName() {
        H2Strategy strategy = new H2Strategy();
        IStrategy.setCurrentStrategy(strategy);
        try {
            assertEquals("\"PUBLIC\".\"table\"", strategy.concatWrapName("PUBLIC", "table"));
            assertEquals("\"table\"", strategy.concatWrapName(null, "table"));
        } finally {
            IStrategy.clean();
        }
    }

    @Test
    void testTypeMapping_notEmpty() {
        H2Strategy strategy = new H2Strategy();
        assertFalse(strategy.typeMapping().isEmpty());
        assertTrue(strategy.typeMapping().containsKey(String.class));
        assertTrue(strategy.typeMapping().containsKey(Integer.class));
        assertTrue(strategy.typeMapping().containsKey(Long.class));
    }

    @Test
    void testTypeMapping_byteMapping() {
        H2Strategy strategy = new H2Strategy();
        assertTrue(strategy.typeMapping().containsKey(byte.class));
        assertTrue(strategy.typeMapping().containsKey(Byte.class));
        assertEquals("TINYINT", strategy.typeMapping().get(byte.class).getTypeName());
        assertEquals("TINYINT", strategy.typeMapping().get(Byte.class).getTypeName());
    }

    @Test
    void testTypeMapping_isUnmodifiable() {
        H2Strategy strategy = new H2Strategy();
        assertThrows(UnsupportedOperationException.class, () -> {
            strategy.typeMapping().put(String.class, null);
        });
    }

    @Test
    void testTypeMapping_commonTypes() {
        H2Strategy strategy = new H2Strategy();

        // 测试常见类型映射
        assertEquals("CHARACTER VARYING", strategy.typeMapping().get(String.class).getTypeName());
        assertEquals("INTEGER", strategy.typeMapping().get(Integer.class).getTypeName());
        assertEquals("BIGINT", strategy.typeMapping().get(Long.class).getTypeName());
        assertEquals("BOOLEAN", strategy.typeMapping().get(Boolean.class).getTypeName());
        assertEquals("REAL", strategy.typeMapping().get(Float.class).getTypeName());
        assertEquals("NUMERIC", strategy.typeMapping().get(Double.class).getTypeName());
    }

    @Test
    void testTypeMapping_allTypes() {
        H2Strategy strategy = new H2Strategy();

        // 测试所有类型映射
        assertEquals("CHARACTER", strategy.typeMapping().get(Character.class).getTypeName());
        assertEquals("CHARACTER", strategy.typeMapping().get(char.class).getTypeName());
        assertEquals("SMALLINT", strategy.typeMapping().get(short.class).getTypeName());
        assertEquals("SMALLINT", strategy.typeMapping().get(Short.class).getTypeName());
        assertEquals("TIME", strategy.typeMapping().get(java.sql.Time.class).getTypeName());
        assertEquals("TIME", strategy.typeMapping().get(java.time.LocalTime.class).getTypeName());
        assertEquals("DATE", strategy.typeMapping().get(java.sql.Date.class).getTypeName());
        assertEquals("DATE", strategy.typeMapping().get(java.time.LocalDate.class).getTypeName());
        assertEquals("TIMESTAMP", strategy.typeMapping().get(java.util.Date.class).getTypeName());
        assertEquals("TIMESTAMP", strategy.typeMapping().get(java.time.LocalDateTime.class).getTypeName());
    }
}
