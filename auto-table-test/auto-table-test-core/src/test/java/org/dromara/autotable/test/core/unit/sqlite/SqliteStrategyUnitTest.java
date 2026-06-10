package org.dromara.autotable.test.core.unit.sqlite;

import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.strategy.sqlite.SqliteStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SqliteStrategy 单元测试
 */
public class SqliteStrategyUnitTest {

    @Test
    void testDatabaseDialect() {
        SqliteStrategy strategy = new SqliteStrategy();
        assertEquals("SQLite", strategy.databaseDialect());
    }

    @Test
    void testIdentifier() {
        SqliteStrategy strategy = new SqliteStrategy();
        assertEquals("\"", strategy.identifier());
    }

    @Test
    void testDropTable() {
        SqliteStrategy strategy = new SqliteStrategy();
        IStrategy.setCurrentStrategy(strategy);
        try {
            String sql = strategy.dropTable("test_db", "test_table");
            assertEquals("DROP TABLE IF EXISTS \"test_table\";", sql);
        } finally {
            IStrategy.clean();
        }
    }

    @Test
    void testWrapIdentifier() {
        SqliteStrategy strategy = new SqliteStrategy();
        IStrategy.setCurrentStrategy(strategy);
        try {
            assertEquals("\"test\"", strategy.wrapIdentifier("test"));
        } finally {
            IStrategy.clean();
        }
    }

    @Test
    void testConcatWrapName() {
        SqliteStrategy strategy = new SqliteStrategy();
        IStrategy.setCurrentStrategy(strategy);
        try {
            assertEquals("\"table\"", strategy.concatWrapName("table"));
        } finally {
            IStrategy.clean();
        }
    }

    @Test
    void testTypeMapping_notEmpty() {
        SqliteStrategy strategy = new SqliteStrategy();
        assertFalse(strategy.typeMapping().isEmpty());
        assertTrue(strategy.typeMapping().containsKey(String.class));
        assertTrue(strategy.typeMapping().containsKey(Integer.class));
        assertTrue(strategy.typeMapping().containsKey(Long.class));
    }

    @Test
    void testTypeMapping_byteMapping() {
        SqliteStrategy strategy = new SqliteStrategy();
        assertTrue(strategy.typeMapping().containsKey(byte.class));
        assertTrue(strategy.typeMapping().containsKey(Byte.class));
        assertEquals("integer", strategy.typeMapping().get(byte.class).getTypeName());
        assertEquals("integer", strategy.typeMapping().get(Byte.class).getTypeName());
    }

    @Test
    void testTypeMapping_isUnmodifiable() {
        SqliteStrategy strategy = new SqliteStrategy();
        assertThrows(UnsupportedOperationException.class, () -> {
            strategy.typeMapping().put(String.class, null);
        });
    }

    @Test
    void testTypeMapping_commonTypes() {
        SqliteStrategy strategy = new SqliteStrategy();

        // 测试常见类型映射
        assertEquals("text", strategy.typeMapping().get(String.class).getTypeName());
        assertEquals("integer", strategy.typeMapping().get(Integer.class).getTypeName());
        assertEquals("integer", strategy.typeMapping().get(Long.class).getTypeName());
        assertEquals("integer", strategy.typeMapping().get(Boolean.class).getTypeName());
        assertEquals("real", strategy.typeMapping().get(Float.class).getTypeName());
        assertEquals("real", strategy.typeMapping().get(Double.class).getTypeName());
    }
}
