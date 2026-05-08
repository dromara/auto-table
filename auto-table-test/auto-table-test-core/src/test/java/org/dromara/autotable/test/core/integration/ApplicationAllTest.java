package org.dromara.autotable.test.core.integration;

import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.constants.Version;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.test.core.RecordSqlFlywayHandler;
import org.dromara.autotable.test.core.base.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApplicationAllTest extends AbstractIntegrationTest {

    @BeforeEach
    public void init() {

        // 配置信息
        PropertyConfig autoTableProperties = AutoTableGlobalConfig.instance().getAutoTableProperties();
        // 开启自动创建数据库
        autoTableProperties.setAutoBuildDatabase(true);
        // create模式
        autoTableProperties.setMode(RunMode.create);
        // 开启 删除不存在的列
        autoTableProperties.setAutoDropColumn(true);
        // 父类字段加到子类的前面
        autoTableProperties.setSuperInsertPosition(PropertyConfig.SuperInsertPosition.after);

        AutoTableGlobalConfig.instance().setAutoTableProperties(autoTableProperties);
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
    public void testRecordSqlByCustomDadasource() {

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
        recordSqlProperties.setRecordType(PropertyConfig.RecordSqlProperties.TypeEnum.db);
        PropertyConfig.Datasource datasource = new PropertyConfig.Datasource();
        datasource.setUrl("jdbc:mysql://localhost:3306/auto-table-record-sql");
        datasource.setUsername("root");
        datasource.setPassword("12345678");
        recordSqlProperties.setDatasource(datasource);
        recordSqlProperties.setTableName("auto_table_exe_sql_record");
        AutoTableGlobalConfig.instance().getAutoTableProperties().setRecordSql(recordSqlProperties);

        // 开始
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
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

    @Test
    public void testMysqlCreateAndUpdate() {

        initSqlSessionFactory("mybatis-config-mysql.xml");

        testRecordSqlByDB();

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
    public void testPgsqlCreateAndUpdate() {

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
}
