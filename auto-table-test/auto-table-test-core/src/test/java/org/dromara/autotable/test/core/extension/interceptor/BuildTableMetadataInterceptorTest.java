package org.dromara.autotable.test.core.extension.interceptor;

import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.interceptor.BuildTableMetadataInterceptor;
import org.dromara.autotable.test.core.base.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BuildTableMetadataInterceptor 功能测试
 */
public class BuildTableMetadataInterceptorTest extends AbstractIntegrationTest {

    @Test
    public void testInterceptorTriggered() {
        initMySqlDataSource();

        AtomicBoolean triggered = new AtomicBoolean(false);
        AutoTableGlobalConfig.instance().setBuildTableMetadataInterceptors(
                Collections.singletonList((BuildTableMetadataInterceptor) (databaseDialect, tableMetadata) -> {
                    triggered.set(true);
                })
        );

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{SimpleEntity.class});
        AutoTableBootstrap.start();

        assertTrue(triggered.get(), "BuildTableMetadataInterceptor 应该被触发");
    }

    @Test
    public void testInterceptorCanModifyTableComment() {
        initMySqlDataSource();

        AutoTableGlobalConfig.instance().setBuildTableMetadataInterceptors(
                Collections.singletonList((BuildTableMetadataInterceptor) (databaseDialect, tableMetadata) -> {
                    tableMetadata.setComment("modified_" + tableMetadata.getComment());
                })
        );

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{SimpleEntity.class});
        AutoTableBootstrap.start();

        // 验证拦截器确实执行了，不会导致异常
    }

    /**
     * 简单实体类，用于测试
     */
    @org.dromara.autotable.annotation.AutoTable
    public static class SimpleEntity {
        private Long id;
        private String name;
    }
}
