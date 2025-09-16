package org.dromara.autotable.test.core.initdata;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

public class ApplicationInitDataTest {

    @AfterEach
    void cleanup() {
        // 清除当前线程中的配置，防止下一个测试复用
        AutoTableGlobalConfig.clear();
    }

    /**
     * 本测试需要先删除数据库
     */
    @Test
    public void testDefault() {

        initSqlSessionFactory("mybatis-config-mysql-init-db.xml");

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setAutoBuildDatabase(true);
        // 指定扫描包
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{
                org.dromara.autotable.test.core.initdata.InitData.class
        });
        PropertyConfig.InitDataProperties initData = new PropertyConfig.InitDataProperties();
        // initData.setBasePath("classpath:sql"); // 默认的
        // initData.setDefaultInitFileName("_init_"); // 默认的
        AutoTableGlobalConfig.instance().getAutoTableProperties().setInitData(initData);

        // 开始
        AutoTableBootstrap.start();
    }

    /**
     * 本测试需要先删除数据库
     */
    @Test
    public void testCustomizePath() {

        initSqlSessionFactory("mybatis-config-mysql-init-db.xml");

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        // 指定扫描包
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{
                org.dromara.autotable.test.core.initdata.InitData.class
        });
        PropertyConfig.InitDataProperties initData = new PropertyConfig.InitDataProperties();
        initData.setBasePath("classpath:customize_path");
        AutoTableGlobalConfig.instance().getAutoTableProperties().setInitData(initData);

        // 开始
        AutoTableBootstrap.start();
    }

    /**
     * 本测试需要先删除数据库
     */
    @Test
    public void testAutoTableProperties() {

        initSqlSessionFactory("mybatis-config-mysql-init-db.xml");

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        // 指定扫描包
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{
                org.dromara.autotable.test.core.initdata.InitDataCustomizeFile.class
        });
        PropertyConfig.InitDataProperties initData = new PropertyConfig.InitDataProperties();
        initData.setBasePath("classpath:customize_path");
        AutoTableGlobalConfig.instance().getAutoTableProperties().setInitData(initData);

        // 开始
        AutoTableBootstrap.start();
    }

    /**
     * 本测试需要先删除数据库
     */
    @Test
    public void testEntityJavaMethod() {

        initSqlSessionFactory("mybatis-config-mysql-init-db.xml");

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        // 指定扫描包
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{
                org.dromara.autotable.test.core.initdata.InitDataJavaMethod.class
        });

        // 开始
        AutoTableBootstrap.start();
    }

    private void initSqlSessionFactory(String resource) {
        try (InputStream inputStream = ApplicationInitDataTest.class.getClassLoader().getResourceAsStream(resource)) {
            // 使用SqlSessionFactoryBuilder加载配置文件
            SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            // 设置当前数据源
            DataSourceManager.setDataSource(sessionFactory.getConfiguration().getEnvironment().getDataSource());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
