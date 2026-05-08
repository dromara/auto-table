package org.dromara.autotable.test.core.base;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;

/**
 * AutoTable 测试基类，提供统一的初始化和清理逻辑
 */
public abstract class AbstractAutoTableTest {

    /**
     * 每个测试方法执行前的初始化
     */
    @BeforeEach
    public void setUp() {
        // 子类可覆盖此方法进行额外的初始化
    }

    /**
     * 每个测试方法执行后的清理
     */
    @AfterEach
    public void tearDown() {
        // 清除当前线程中的配置，防止测试间相互影响
        AutoTableGlobalConfig.clear();
        try {
            DataSourceManager.cleanDataSource();
        } catch (Exception e) {
            // 忽略数据源清理异常，因为并非所有测试都会初始化数据源
        }
    }

    /**
     * 初始化 MyBatis SqlSessionFactory
     *
     * @param resource MyBatis 配置文件路径
     */
    protected void initSqlSessionFactory(String resource) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resource)) {
            SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            DataSource dataSource = sessionFactory.getConfiguration().getEnvironment().getDataSource();
            DataSourceManager.setDataSource(dataSource);
        } catch (IOException e) {
            throw new RuntimeException("初始化数据源失败: " + resource, e);
        }
    }

    /**
     * 初始化 MySQL 数据源
     */
    protected void initMySqlDataSource() {
        initSqlSessionFactory("mybatis-config-mysql.xml");
    }

    /**
     * 初始化 PostgreSQL 数据源
     */
    protected void initPgSqlDataSource() {
        initSqlSessionFactory("mybatis-config-pgsql.xml");
    }

    /**
     * 初始化 H2 数据源
     */
    protected void initH2DataSource() {
        initSqlSessionFactory("mybatis-config-h2.xml");
    }

    /**
     * 初始化 SQLite 数据源
     */
    protected void initSqliteDataSource() {
        initSqlSessionFactory("mybatis-config-sqlite.xml");
    }
}
