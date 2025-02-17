package org.dromara.autotable.test.core;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.constants.Version;
import org.dromara.autotable.core.dynamicds.SqlSessionFactoryManager;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.io.InputStream;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ApplicationAllTest {

    @Before
    public void init() {

        // 配置信息
        PropertyConfig autoTableProperties = AutoTableGlobalConfig.getAutoTableProperties();
        // create模式
        autoTableProperties.setMode(RunMode.create);
        // 开启 删除不存在的列
        autoTableProperties.setAutoDropColumn(true);
        // 父类字段加到子类的前面
        autoTableProperties.setSuperInsertPosition(PropertyConfig.SuperInsertPosition.after);

        AutoTableGlobalConfig.setAutoTableProperties(autoTableProperties);
    }

    @Test
    public void testRecordSqlByFlyway() {

        initSqlSessionFactory("mybatis-config-mysql.xml");

        // 指定扫描包
        AutoTableGlobalConfig.getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common",
                "org.dromara.autotable.test.core.entity.mysql"
        });

        // 记录sql
        PropertyConfig.RecordSqlProperties recordSqlProperties = new PropertyConfig.RecordSqlProperties();
        recordSqlProperties.setEnable(true);
        recordSqlProperties.setVersion(Version.VALUE);
        // 自定义，以Flyway的格式记录sql
        recordSqlProperties.setRecordType(PropertyConfig.RecordSqlProperties.TypeEnum.custom);
        AutoTableGlobalConfig.getAutoTableProperties().setRecordSql(recordSqlProperties);
        AutoTableGlobalConfig.setCustomRecordSqlHandler(new RecordSqlFlywayHandler("/Users/don/Downloads/sqlLogs"));

        // 开始
        testMysqlCreate();
    }

    @Test
    public void testMysqlCreate() {

        initSqlSessionFactory("mybatis-config-mysql.xml");

        testRecordSqlByDB();

        AutoTableGlobalConfig.getAutoTableProperties().setMode(RunMode.create);
        // 指定扫描包
        AutoTableGlobalConfig.getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common",
                "org.dromara.autotable.test.core.entity.mysql"
        });
        // 开始
        AutoTableBootstrap.start();


        /* 修改表的逻辑 */
        AutoTableGlobalConfig.getAutoTableProperties().setMode(RunMode.create);
        // 测试所有的公共测试类
        AutoTableGlobalConfig.getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common_update",
                "org.dromara.autotable.test.core.entity.mysql_update",
        });
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testPgsqlCreate() {

        initSqlSessionFactory("mybatis-config-pgsql.xml");

        testRecordSqlByDB();

        AutoTableGlobalConfig.getAutoTableProperties().setMode(RunMode.create);
        // 测试所有的公共测试类
        AutoTableGlobalConfig.getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common",
                "org.dromara.autotable.test.core.entity.pgsql",
        });
        // 开始
        AutoTableBootstrap.start();


        /* 修改表的逻辑 */
        AutoTableGlobalConfig.getAutoTableProperties().setMode(RunMode.update);
        // 测试所有的公共测试类
        AutoTableGlobalConfig.getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common_update",
                "org.dromara.autotable.test.core.entity.pgsql_update",
        });
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testH2Create() {

        initSqlSessionFactory("mybatis-config-h2.xml");

        testRecordSqlByDB();

        AutoTableGlobalConfig.getAutoTableProperties().setMode(RunMode.create);
        // 测试所有的公共测试类
        AutoTableGlobalConfig.getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common",
                "org.dromara.autotable.test.core.entity.h2",
        });
        // 开始
        AutoTableBootstrap.start();


        /* 修改表的逻辑 */
        AutoTableGlobalConfig.getAutoTableProperties().setMode(RunMode.update);
        // 测试所有的公共测试类
        AutoTableGlobalConfig.getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common_update",
                "org.dromara.autotable.test.core.entity.h2_update",
        });
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testSqliteCreate() {

        initSqlSessionFactory("mybatis-config-sqlite.xml");

        testRecordSqlByFile();

        AutoTableGlobalConfig.getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common",
                "org.dromara.autotable.test.core.entity.sqlite",
        });
        // 开始
        AutoTableBootstrap.start();


        /* 修改表的逻辑 */
        AutoTableGlobalConfig.getAutoTableProperties().setMode(RunMode.update);
        AutoTableGlobalConfig.getAutoTableProperties().setModelPackage(new String[]{
                "org.dromara.autotable.test.core.entity.common_update",
                "org.dromara.autotable.test.core.entity.sqlite_update",
        });
        // 开始
        AutoTableBootstrap.start();
    }

    private void testRecordSqlByFile() {

        // 记录sql
        PropertyConfig.RecordSqlProperties recordSqlProperties = new PropertyConfig.RecordSqlProperties();
        recordSqlProperties.setEnable(true);
        recordSqlProperties.setVersion(Version.VALUE);
        // 自定义，以文件形式记录sql
        recordSqlProperties.setRecordType(PropertyConfig.RecordSqlProperties.TypeEnum.file);
        recordSqlProperties.setFolderPath("/Users/don/Downloads/sqlLogs");
        AutoTableGlobalConfig.getAutoTableProperties().setRecordSql(recordSqlProperties);

        // 开始
        testMysqlCreate();
    }

    private void testRecordSqlByDB() {

        // 记录sql
        PropertyConfig.RecordSqlProperties recordSqlProperties = new PropertyConfig.RecordSqlProperties();
        recordSqlProperties.setEnable(true);
        recordSqlProperties.setVersion(Version.VALUE);
        // 以数据库的方式记录sql
        recordSqlProperties.setRecordType(PropertyConfig.RecordSqlProperties.TypeEnum.db);
        AutoTableGlobalConfig.getAutoTableProperties().setRecordSql(recordSqlProperties);
    }

    private void initSqlSessionFactory(String resource) {
        try (InputStream inputStream = ApplicationAllTest.class.getClassLoader().getResourceAsStream(resource)) {
            // 使用SqlSessionFactoryBuilder加载配置文件
            SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            // 设置当前数据源
            SqlSessionFactoryManager.setSqlSessionFactory(sessionFactory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
