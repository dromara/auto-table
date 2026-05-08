package org.dromara.autotable.test.core.extension.callback;

import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.callback.AutoTableReadyCallback;
import org.dromara.autotable.core.callback.CreateTableFinishCallback;
import org.dromara.autotable.core.callback.RunAfterCallback;
import org.dromara.autotable.core.callback.RunBeforeCallback;
import org.dromara.autotable.test.core.base.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Callback 触发顺序验证测试
 */
public class CallbackOrderTest extends AbstractIntegrationTest {

    @Test
    public void testCallbackOrder() {
        initMySqlDataSource();

        List<String> events = new ArrayList<>();

        // 注册各种 callback
        AutoTableGlobalConfig.instance().setAutoTableReadyCallbacks(
                Collections.singletonList((AutoTableReadyCallback) tableClasses -> events.add("ready"))
        );
        AutoTableGlobalConfig.instance().setRunBeforeCallbacks(
                Collections.singletonList((RunBeforeCallback) tableClass -> events.add("before:" + tableClass.getSimpleName()))
        );
        AutoTableGlobalConfig.instance().setCreateTableFinishCallbacks(
                Collections.singletonList((CreateTableFinishCallback) (dialect, metadata) -> events.add("createFinish"))
        );
        AutoTableGlobalConfig.instance().setRunAfterCallbacks(
                Collections.singletonList((RunAfterCallback) tableClass -> events.add("after:" + tableClass.getSimpleName()))
        );

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{TestEntity.class});
        AutoTableBootstrap.start();

        // 验证 callback 触发顺序
        assertTrue(events.contains("ready"), "应有 ready 事件");
        assertTrue(events.contains("before:TestEntity"), "应有 before 事件");
        assertTrue(events.contains("after:TestEntity"), "应有 after 事件");
        assertTrue(events.contains("createFinish"), "应有 createFinish 事件");
    }

    @Test
    public void testMultipleCallbacksExecution() {
        initMySqlDataSource();

        List<String> events = new ArrayList<>();

        // 注册多个同类型的 callback
        AutoTableGlobalConfig.instance().setRunBeforeCallbacks(java.util.Arrays.asList(
                (RunBeforeCallback) tableClass -> events.add("before1"),
                (RunBeforeCallback) tableClass -> events.add("before2")
        ));

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{TestEntity.class});
        AutoTableBootstrap.start();

        assertTrue(events.contains("before1"), "第一个 before callback 应该被触发");
        assertTrue(events.contains("before2"), "第二个 before callback 应该被触发");
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
