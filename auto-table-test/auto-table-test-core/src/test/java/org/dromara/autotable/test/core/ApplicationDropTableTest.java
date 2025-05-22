package org.dromara.autotable.test.core;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.test.core.entity.mysql.TestColumnCharset;
import org.dromara.autotable.test.core.entity.mysql.TestColumnSort;
import org.dromara.autotable.test.core.entity.mysql.TestMysqlDefineColumn;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

@Disabled
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ApplicationDropTableTest {

    @Test
    public void testMysqlColumnSort() {

        initSqlSessionFactory("mybatis-config-mysql-drop-table.xml");

        PropertyConfig autoTableProperties = AutoTableGlobalConfig.getAutoTableProperties();
        autoTableProperties.setMode(RunMode.create);
        // 指定扫描包
        Class[] modelClass = {
                TestColumnSort.class,
                TestMysqlDefineColumn.class,
                TestColumnCharset.class,
        };
        autoTableProperties.setModelClass(modelClass);

        AutoTableGlobalConfig.setAutoTableReadyCallbacks(Collections.singletonList(tableClasses -> {
            boolean sameSize = tableClasses.size() == modelClass.length;
            boolean containsAll = tableClasses.containsAll(Arrays.asList(modelClass));
            Assert.assertTrue(sameSize && containsAll);
        }));

        // 开始创建
        AutoTableBootstrap.start();
        AutoTableGlobalConfig.setAutoTableReadyCallbacks(Collections.emptyList());


        /* 开始修改表，同时删掉一个对象，观察数据库是否执行了删除指定的表 */
        // 开启自动删除多余的表
        autoTableProperties.setAutoDropTable(true);
        // 指定忽略某表不删除
        autoTableProperties.setAutoDropTableIgnores(new String[]{StringUtils.camelToUnderline(modelClass[2].getSimpleName())});
        autoTableProperties.setMode(RunMode.update);
        // 指定扫描包
        autoTableProperties.setModelClass(new Class[]{
                modelClass[0],
        });
        AutoTableGlobalConfig.setDeleteTableFinishCallbacks(Collections.singletonList((schema, tableName) -> {
            Assert.assertEquals(StringUtils.camelToUnderline(modelClass[1].getSimpleName()), tableName);
        }));
        // 开始修改表
        AutoTableBootstrap.start();
    }


    private void initSqlSessionFactory(String resource) {
        try (InputStream inputStream = ApplicationDropTableTest.class.getClassLoader().getResourceAsStream(resource)) {
            // 使用SqlSessionFactoryBuilder加载配置文件
            SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            // 设置当前数据源
            DataSourceManager.setDataSource(sessionFactory.getConfiguration().getEnvironment().getDataSource());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
