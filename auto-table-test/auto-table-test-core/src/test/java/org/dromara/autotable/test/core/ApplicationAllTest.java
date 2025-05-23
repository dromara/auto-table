package org.dromara.autotable.test.core;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.constants.Version;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;

public class ApplicationAllTest {

    @BeforeEach
    public void init() {

        // 配置信息
        PropertyConfig autoTableProperties = AutoTableGlobalConfig.instance().getAutoTableProperties();
        // create模式
        autoTableProperties.setMode(RunMode.create);
        // 开启 删除不存在的列
        autoTableProperties.setAutoDropColumn(true);
        // 父类字段加到子类的前面
        autoTableProperties.setSuperInsertPosition(PropertyConfig.SuperInsertPosition.after);

        AutoTableGlobalConfig.instance().setAutoTableProperties(autoTableProperties);
    }

    @AfterEach
    void cleanup() {
        // 清除当前线程中的配置，防止下一个测试复用
        AutoTableGlobalConfig.clear();
    }

    @Test
    public void testRecordSqlByFlyway() {

        initSqlSessionFactory("mybatis-config-mysql.xml");

        // 指定扫描包
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common",
                "org.dromara.autotable.test.core.entity.mysql"
        });

        // 记录sql
        PropertyConfig.RecordSqlProperties recordSqlProperties = new PropertyConfig.RecordSqlProperties();
        recordSqlProperties.setEnable(true);
        recordSqlProperties.setVersion(Version.VALUE);
        // 自定义，以Flyway的格式记录sql
        recordSqlProperties.setRecordType(PropertyConfig.RecordSqlProperties.TypeEnum.custom);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setRecordSql(recordSqlProperties);
        AutoTableGlobalConfig.instance().setCustomRecordSqlHandler(new RecordSqlFlywayHandler("/Users/don/Downloads/sqlLogs"));

        // 开始
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testRecordSqlByFile() {

        initSqlSessionFactory("mybatis-config-mysql.xml");

        // 指定扫描包
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common",
                "org.dromara.autotable.test.core.entity.mysql"
        });

        // 记录sql
        PropertyConfig.RecordSqlProperties recordSqlProperties = new PropertyConfig.RecordSqlProperties();
        recordSqlProperties.setEnable(true);
        recordSqlProperties.setVersion(Version.VALUE);
        // 自定义，以文件形式记录sql
        recordSqlProperties.setRecordType(PropertyConfig.RecordSqlProperties.TypeEnum.file);
        recordSqlProperties.setFolderPath("/Users/don/Downloads/sqlLogs");
        AutoTableGlobalConfig.instance().getAutoTableProperties().setRecordSql(recordSqlProperties);

        // 开始
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testMysqlCreateAndUpdate() {

        initSqlSessionFactory("mybatis-config-mysql.xml");

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        // 指定扫描包
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common",
                "org.dromara.autotable.test.core.entity.mysql"
        });
        // 开始
        AutoTableBootstrap.start();

        /* 修改表的逻辑 */
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.update);
        // 测试所有的公共测试类
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common_update",
                "org.dromara.autotable.test.core.entity.mysql_update",
        });
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testPgsqlCreate() {

        initSqlSessionFactory("mybatis-config-pgsql.xml");

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        // 测试所有的公共测试类
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common",
                "org.dromara.autotable.test.core.entity.pgsql",
        });
        // 开始
        AutoTableBootstrap.start();


        /* 修改表的逻辑 */
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.update);
        // 测试所有的公共测试类
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common_update",
                "org.dromara.autotable.test.core.entity.pgsql_update",
        });
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testPgsqlUpdateIndexColumnSort() {

        initSqlSessionFactory("mybatis-config-pgsql.xml");

        testRecordSqlByDB();

        // 测试
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class<?>[]{
                org.dromara.autotable.test.core.entity.pgsql.TestIndexSort.class
        });

        /* 新建表 */
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        // 开始
        AutoTableBootstrap.start();

        /* 修改表的逻辑 */
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.update);
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testH2Create() {

        initSqlSessionFactory("mybatis-config-h2.xml");

        testRecordSqlByDB();

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        // 测试所有的公共测试类
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common",
                "org.dromara.autotable.test.core.entity.h2",
        });
        // 开始
        AutoTableBootstrap.start();


        /* 修改表的逻辑 */
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.update);
        // 测试所有的公共测试类
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common_update",
                "org.dromara.autotable.test.core.entity.h2_update",
        });
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testSqliteCreate() {

        initSqlSessionFactory("mybatis-config-sqlite.xml");

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common",
                "org.dromara.autotable.test.core.entity.sqlite",
        });
        // 开始
        AutoTableBootstrap.start();


        /* 修改表的逻辑 */
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.update);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common_update",
                "org.dromara.autotable.test.core.entity.sqlite_update",
        });
        // 开始
        AutoTableBootstrap.start();
    }

    private void testRecordSqlByDB() {

        // 记录sql
        PropertyConfig.RecordSqlProperties recordSqlProperties = new PropertyConfig.RecordSqlProperties();
        recordSqlProperties.setEnable(true);
        recordSqlProperties.setVersion(Version.VALUE);
        // 以数据库的方式记录sql
        recordSqlProperties.setRecordType(PropertyConfig.RecordSqlProperties.TypeEnum.db);
        recordSqlProperties.setTableName("my_record_sql");
        AutoTableGlobalConfig.instance().getAutoTableProperties().setRecordSql(recordSqlProperties);
    }

    private void initSqlSessionFactory(String resource) {
        try (InputStream inputStream = ApplicationAllTest.class.getClassLoader().getResourceAsStream(resource)) {
            // 使用SqlSessionFactoryBuilder加载配置文件
            SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            // 设置当前数据源
            DataSource dataSource = sessionFactory.getConfiguration().getEnvironment().getDataSource();
            DataSourceManager.setDataSource(dataSource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
