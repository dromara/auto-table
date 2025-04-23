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
import org.dromara.autotable.core.strategy.h2.data.H2DefaultTypeEnum;
import org.dromara.autotable.test.core.entity.h2.TestH2;
import org.dromara.autotable.test.core.entity.pgsql.TestNoColumnComment;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

@Disabled
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ApplicationSingleTest {

    @Test
    public void testMysqlColumnSort() {

        initSqlSessionFactory("mybatis-config-mysql.xml");

        AutoTableGlobalConfig.getAutoTableProperties().setMode(RunMode.update);
        // 指定扫描包
        AutoTableGlobalConfig.getAutoTableProperties().setModelClass(new Class[]{
                org.dromara.autotable.test.core.entity.mysql.TestColumnSort.class
        });
        // 开始
        AutoTableBootstrap.start();
    }

    @Test
    public void testH2Create() {

        initSqlSessionFactory("mybatis-config-h2.xml");

        /* 修改表的逻辑 */
        PropertyConfig autoTableProperties = AutoTableGlobalConfig.getAutoTableProperties();

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
    public void testPgsqlCreate() {

        initSqlSessionFactory("mybatis-config-pgsql.xml");

        /* 修改表的逻辑 */
        PropertyConfig autoTableProperties = AutoTableGlobalConfig.getAutoTableProperties();

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
        PropertyConfig autoTableProperties = AutoTableGlobalConfig.getAutoTableProperties();

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
