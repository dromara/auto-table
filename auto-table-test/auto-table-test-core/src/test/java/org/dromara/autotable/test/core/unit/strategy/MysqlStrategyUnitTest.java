package org.dromara.autotable.test.core.unit.strategy;

import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.strategy.mysql.MysqlStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MysqlStrategy 单元测试
 */
public class MysqlStrategyUnitTest {

    @Test
    void testDatabaseDialect() {
        MysqlStrategy strategy = new MysqlStrategy();
        assertEquals("MySQL", strategy.databaseDialect());
    }

    @Test
    void testIdentifier() {
        MysqlStrategy strategy = new MysqlStrategy();
        assertEquals("`", strategy.identifier());
    }

    @Test
    void testDropTable() {
        MysqlStrategy strategy = new MysqlStrategy();
        IStrategy.setCurrentStrategy(strategy);
        try {
            String sql = strategy.dropTable("test_db", "test_table");
            assertEquals("DROP TABLE IF EXISTS `test_table`", sql);
        } finally {
            IStrategy.clean();
        }
    }

    @Test
    void testWrapIdentifier() {
        MysqlStrategy strategy = new MysqlStrategy();
        IStrategy.setCurrentStrategy(strategy);
        try {
            assertEquals("`test`", strategy.wrapIdentifier("test"));
            assertEquals("`test`", strategy.wrapIdentifier("`test`"));
        } finally {
            IStrategy.clean();
        }
    }

    @Test
    void testConcatWrapName() {
        MysqlStrategy strategy = new MysqlStrategy();
        IStrategy.setCurrentStrategy(strategy);
        try {
            assertEquals("`db`.`table`", strategy.concatWrapName("db", "table"));
            assertEquals("`table`", strategy.concatWrapName("table"));
        } finally {
            IStrategy.clean();
        }
    }

    @Test
    void testTypeMapping_notEmpty() {
        MysqlStrategy strategy = new MysqlStrategy();
        assertFalse(strategy.typeMapping().isEmpty());
        assertTrue(strategy.typeMapping().containsKey(String.class));
        assertTrue(strategy.typeMapping().containsKey(Integer.class));
        assertTrue(strategy.typeMapping().containsKey(Long.class));
    }

    @Test
    void testTypeMapping_byteMapping() {
        MysqlStrategy strategy = new MysqlStrategy();
        assertTrue(strategy.typeMapping().containsKey(byte.class));
        assertTrue(strategy.typeMapping().containsKey(Byte.class));
        assertEquals("tinyint", strategy.typeMapping().get(byte.class).getTypeName());
        assertEquals("tinyint", strategy.typeMapping().get(Byte.class).getTypeName());
    }

    @Test
    void testTypeMapping_isUnmodifiable() {
        MysqlStrategy strategy = new MysqlStrategy();
        assertThrows(UnsupportedOperationException.class, () -> {
            strategy.typeMapping().put(String.class, null);
        });
    }
}
