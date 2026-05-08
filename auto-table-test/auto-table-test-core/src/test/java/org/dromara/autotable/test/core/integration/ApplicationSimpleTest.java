package org.dromara.autotable.test.core.integration;

import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.test.core.base.AbstractIntegrationTest;
import org.dromara.autotable.test.core.entity.mysql.TestTableIndexes;
import org.junit.jupiter.api.Test;

public class ApplicationSimpleTest extends AbstractIntegrationTest {

    @Test
    public void testMysqlAlterTableDrop() {

        initMySqlDataSource();

        // 创建表
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        // 指定扫描包
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{
                TestTableIndexes.class
        });
        // 开始
        AutoTableBootstrap.start();


        // 更新表
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.update);
        // 指定扫描包
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{
                org.dromara.autotable.test.core.entity.mysql_update.TestIndex.class
        });
        // 分离删除的sql
        AutoTableGlobalConfig.instance().getAutoTableProperties().getMysql().setAlterTableSeparateDrop(true);
        // 开始
        AutoTableBootstrap.start();
    }
}
