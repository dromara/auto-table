package org.dromara.autotable.test.core.extension.callback;

import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.callback.*;
import org.dromara.autotable.test.core.base.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Callback 参数验证测试
 */
public class CallbackParametersTest extends AbstractIntegrationTest {

    @Test
    public void testRunBeforeCallbackReceivesTableClass() {
        initMySqlDataSource();

        List<String> receivedClasses = new ArrayList<>();
        AutoTableGlobalConfig.instance().setRunBeforeCallbacks(
                Collections.singletonList((RunBeforeCallback) tableClass ->
                        receivedClasses.add(tableClass.getSimpleName()))
        );

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{TestEntity.class});
        AutoTableBootstrap.start();

        assertTrue(receivedClasses.contains("TestEntity"), "RunBeforeCallback 应该收到 TestEntity");
    }

    @Test
    public void testCreateTableFinishCallbackReceivesMetadata() {
        initMySqlDataSource();

        List<String> receivedTableNames = new ArrayList<>();
        AutoTableGlobalConfig.instance().setCreateTableFinishCallbacks(
                Collections.singletonList((CreateTableFinishCallback) (databaseDialect, tableMetadata) ->
                        receivedTableNames.add(tableMetadata.getTableName()))
        );

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{TestEntity.class});
        AutoTableBootstrap.start();

        assertFalse(receivedTableNames.isEmpty(), "CreateTableFinishCallback 应该收到表名");
        assertTrue(receivedTableNames.stream().anyMatch(name -> name.contains("test_entity")),
                "应该包含 test_entity 表名");
    }

    @Test
    public void testAutoTableReadyCallbackReceivesClassSet() {
        initMySqlDataSource();

        AtomicBoolean triggered = new AtomicBoolean(false);
        AutoTableGlobalConfig.instance().setAutoTableReadyCallbacks(
                Collections.singletonList((AutoTableReadyCallback) tableClasses ->
                        triggered.set(true))
        );

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{TestEntity.class});
        AutoTableBootstrap.start();

        assertTrue(triggered.get(), "AutoTableReadyCallback 应该被触发");
    }

    @Test
    public void testAutoTableFinishCallbackTriggered() {
        initMySqlDataSource();

        AtomicBoolean triggered = new AtomicBoolean(false);
        AutoTableGlobalConfig.instance().setAutoTableFinishCallbacks(
                Collections.singletonList((AutoTableFinishCallback) tableClasses -> triggered.set(true))
        );

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{TestEntity.class});
        AutoTableBootstrap.start();

        assertTrue(triggered.get(), "AutoTableFinishCallback 应该被触发");
    }

    /**
     * 测试用的简单实体类
     */
    @org.dromara.autotable.annotation.AutoTable
    public static class TestEntity {
        private Long id;
        private String name;
    }
}
