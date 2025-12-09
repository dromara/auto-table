package org.dromara.autotable.test.core;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.test.core.entity.mysql.TestTableIndexes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

public class ApplicationSimpleTest {

    @AfterEach
    void cleanup() {
        // 清除当前线程中的配置，防止下一个测试复用
        AutoTableGlobalConfig.clear();
    }

    @Test
    public void testMysqlAlterTableDrop() {

        initSqlSessionFactory("mybatis-config-mysql.xml");

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

    private void initSqlSessionFactory(String resource) {
        try (InputStream inputStream = ApplicationSimpleTest.class.getClassLoader().getResourceAsStream(resource)) {
            // 使用SqlSessionFactoryBuilder加载配置文件
            SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            // 设置当前数据源
            DataSourceManager.setDataSource(sessionFactory.getConfiguration().getEnvironment().getDataSource());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
