package org.dromara.autotable.test.core.unit.utils;

import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.strategy.mysql.MysqlStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AutoTableGlobalConfig 单元测试
 */
public class AutoTableGlobalConfigTest {

    @Test
    void testInstance_isThreadLocal() {
        AutoTableGlobalConfig config1 = AutoTableGlobalConfig.instance();
        AutoTableGlobalConfig config2 = AutoTableGlobalConfig.instance();

        assertSame(config1, config2);
    }

    @Test
    void testClear_removesInstance() {
        AutoTableGlobalConfig config1 = AutoTableGlobalConfig.instance();
        config1.setUnitTestMode(true);

        AutoTableGlobalConfig.clear();

        AutoTableGlobalConfig config2 = AutoTableGlobalConfig.instance();
        assertNotSame(config1, config2);
        assertFalse(config2.isUnitTestMode());
    }

    @Test
    void testAddAndGetStrategy() {
        AutoTableGlobalConfig config = AutoTableGlobalConfig.instance();
        MysqlStrategy mysqlStrategy = new MysqlStrategy();
        config.addStrategy(mysqlStrategy);

        IStrategy<?, ?> strategy = config.getStrategy("MySQL");
        assertNotNull(strategy);
        assertSame(mysqlStrategy, strategy);
    }

    @Test
    void testGetAllStrategy() {
        AutoTableGlobalConfig config = AutoTableGlobalConfig.instance();
        config.addStrategy(new MysqlStrategy());

        assertFalse(config.getAllStrategy().isEmpty());
    }

    @Test
    void testGetStrategy_notFound() {
        AutoTableGlobalConfig config = AutoTableGlobalConfig.instance();
        assertNull(config.getStrategy("NonExistentDB"));
    }

    @Test
    void testUnitTestMode() {
        AutoTableGlobalConfig config = AutoTableGlobalConfig.instance();

        config.setUnitTestMode(true);
        assertTrue(config.isUnitTestMode());

        config.setUnitTestMode(false);
        assertFalse(config.isUnitTestMode());
    }
}
