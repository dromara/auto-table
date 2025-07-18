package org.dromara.autotable.test.core;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.JavaTypeToDatabaseTypeConverter;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.interceptor.CreateTableInterceptor;
import org.dromara.autotable.strategy.h2.data.H2DefaultTypeEnum;
import org.dromara.autotable.strategy.mysql.data.MysqlColumnMetadata;
import org.dromara.autotable.strategy.mysql.data.MysqlTableMetadata;
import org.dromara.autotable.test.core.entity.h2.TestH2;
import org.dromara.autotable.test.core.entity.mysql.custome_add_column.MyBuildTableMetadataInterceptor;
import org.dromara.autotable.test.core.entity.pgsql.TestNoColumnComment;
import org.dromara.autotable.test.core.entity.sqlite.TestSqlite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ApplicationSingleTest {

    @AfterEach
    void cleanup() {
        // 清除当前线程中的配置，防止下一个测试复用
        AutoTableGlobalConfig.clear();
    }

    @Test
    public void testMysqlColumnSort() {

        initSqlSessionFactory("mybatis-config-mysql.xml");

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        // 指定扫描包
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{
                org.dromara.autotable.test.core.entity.mysql.TestColumnSort.class
        });

        CreateTableInterceptor createTableInterceptor = (databaseDialect, tableMetadata) -> {
            assert databaseDialect.equals(DatabaseDialect.MySQL);
            assert tableMetadata instanceof MysqlTableMetadata;
            MysqlTableMetadata mysqlTableMetadata = (MysqlTableMetadata) tableMetadata;
            List<MysqlColumnMetadata> columnMetadataList = mysqlTableMetadata.getColumnMetadataList();
            assert "id".equals(columnMetadataList.get(0).getName());
            assert "update_time".equals(columnMetadataList.get(columnMetadataList.size() - 1).getName());
        };
        AutoTableGlobalConfig.instance().setCreateTableInterceptors(Collections.singletonList(
                createTableInterceptor
        ));

        // 开始
        AutoTableBootstrap.start();
    }

    /**
     * 测试自定义添加列，是否引发表更新的问题
     */
    @Test
    public void testMysqlConsumeAddColumn() {

        initSqlSessionFactory("mybatis-config-mysql.xml");

        AutoTableGlobalConfig.instance().setBuildTableMetadataInterceptors(Collections.singletonList(new MyBuildTableMetadataInterceptor()));
        // 指定扫描包
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{
                org.dromara.autotable.test.core.entity.mysql.custome_add_column.SoftwareClassify.class
        });
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        // 开始
        AutoTableBootstrap.start();

        AutoTableGlobalConfig.instance().setCompareTableFinishCallbacks(
                Collections.singletonList((databaseDialect, tableMetadata, compareTableInfo) -> {
                    boolean needModify = compareTableInfo.needModify();
                    // 判断是否需要更新
                    assert !needModify;
                })
        );
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.update);
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testH2Create() {

        initSqlSessionFactory("mybatis-config-h2.xml");

        /* 修改表的逻辑 */
        PropertyConfig autoTableProperties = AutoTableGlobalConfig.instance().getAutoTableProperties();

        autoTableProperties.setMode(RunMode.create);
        // 开启 删除不存在的列
        autoTableProperties.setAutoDropColumn(true);
        // 测试所有的公共测试类
        autoTableProperties.setModelClass(new Class[]{
                TestH2.class
        });
        // 自定义java类型与数据库类型映射关系
        JavaTypeToDatabaseTypeConverter.addTypeMapping(DatabaseDialect.H2, Date.class, H2DefaultTypeEnum.TIMESTAMP);
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testSqliteCreate() {

        initSqlSessionFactory("mybatis-config-sqlite.xml");

        /* 修改表的逻辑 */
        PropertyConfig autoTableProperties = AutoTableGlobalConfig.instance().getAutoTableProperties();

        autoTableProperties.setMode(RunMode.create);
        // 测试所有的公共测试类
        autoTableProperties.setModelClass(new Class[]{
                TestSqlite.class
        });
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testPgsqlCreate() {

        initSqlSessionFactory("mybatis-config-pgsql.xml");

        /* 修改表的逻辑 */
        PropertyConfig autoTableProperties = AutoTableGlobalConfig.instance().getAutoTableProperties();

        autoTableProperties.setMode(RunMode.update);
        // 测试所有的公共测试类
        autoTableProperties.setModelClass(new Class[]{
                TestNoColumnComment.class
        });
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testPgsqlUpdate() {

        initSqlSessionFactory("mybatis-config-pgsql.xml");

        /* 修改表的逻辑 */
        PropertyConfig autoTableProperties = AutoTableGlobalConfig.instance().getAutoTableProperties();

        autoTableProperties.setMode(RunMode.update);
        // 测试所有的公共测试类
        autoTableProperties.setModelClass(new Class[]{
                org.dromara.autotable.test.core.entity.common_update.TestTableIndex.class
        });
        // 开始
        AutoTableBootstrap.start();
    }

    private void initSqlSessionFactory(String resource) {
        try (InputStream inputStream = ApplicationSingleTest.class.getClassLoader().getResourceAsStream(resource)) {
            // 使用SqlSessionFactoryBuilder加载配置文件
            SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            // 设置当前数据源
            DataSourceManager.setDataSource(sessionFactory.getConfiguration().getEnvironment().getDataSource());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
