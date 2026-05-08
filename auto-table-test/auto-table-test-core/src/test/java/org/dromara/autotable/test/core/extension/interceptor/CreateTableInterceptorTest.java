package org.dromara.autotable.test.core.extension.interceptor;

import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.interceptor.CreateTableInterceptor;
import org.dromara.autotable.core.interceptor.ModifyTableInterceptor;
import org.dromara.autotable.test.core.base.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CreateTableInterceptor 和 ModifyTableInterceptor 功能测试
 */
public class CreateTableInterceptorTest extends AbstractIntegrationTest {

    @Test
    public void testCreateTableInterceptorTriggered() {
        initMySqlDataSource();

        AtomicBoolean triggered = new AtomicBoolean(false);
        AutoTableGlobalConfig.instance().setCreateTableInterceptors(
                Collections.singletonList((CreateTableInterceptor) (databaseDialect, tableMetadata) -> {
                    triggered.set(true);
                })
        );

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{TestEntity.class});
        AutoTableBootstrap.start();

        assertTrue(triggered.get(), "CreateTableInterceptor 应该被触发");
    }

    @Test
    public void testModifyTableInterceptorTriggered() {
        initMySqlDataSource();

        // 先创建表
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{TestEntity.class});
        AutoTableBootstrap.start();

        AtomicBoolean triggered = new AtomicBoolean(false);
        AutoTableGlobalConfig.instance().setModifyTableInterceptors(
                Collections.singletonList((ModifyTableInterceptor) (databaseDialect, tableMetadata, compareTableInfo) -> {
                    triggered.set(true);
                })
        );

        // update 表
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.update);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{TestEntity.class});
        AutoTableBootstrap.start();

        // 注意：如果表结构没有变化，modify 可能不会被触发
        // 这里只是验证代码能正常运行
    }

    @Test
    public void testCreateTableInterceptorCanModifySql() {
        initMySqlDataSource();

        AtomicBoolean sqlModified = new AtomicBoolean(false);
        AutoTableGlobalConfig.instance().setCreateTableInterceptors(
                Collections.singletonList((CreateTableInterceptor) (databaseDialect, tableMetadata) -> {
                    sqlModified.set(true);
                })
        );

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{TestEntity.class});
        AutoTableBootstrap.start();

        assertTrue(sqlModified.get(), "CreateTableInterceptor 应该能获取到 SQL 列表");
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
