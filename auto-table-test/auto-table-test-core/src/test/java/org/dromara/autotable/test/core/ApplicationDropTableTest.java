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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

public class ApplicationDropTableTest {

    @AfterEach
    void cleanup() {
        // 清除当前线程中的配置，防止下一个测试复用
        AutoTableGlobalConfig.clear();
    }

    @Test
    public void test() {

        initSqlSessionFactory("mybatis-config-mysql-drop-table.xml");

        PropertyConfig autoTableProperties = AutoTableGlobalConfig.instance().getAutoTableProperties();
        autoTableProperties.setAutoBuildDatabase(true);
        autoTableProperties.setMode(RunMode.create);
        // 指定扫描包
        Class[] modelClass = {
                TestColumnSort.class,
                TestMysqlDefineColumn.class,
                TestColumnCharset.class,
        };
        autoTableProperties.setModelClass(modelClass);

        AutoTableGlobalConfig.instance().setAutoTableReadyCallbacks(Collections.singletonList(tableClasses -> {
            // 确认三张表
            boolean sameSize = tableClasses.size() == modelClass.length;
            Assertions.assertTrue(sameSize, "表数量不一致，请检查 " + modelClass.length + " vs " + tableClasses.size());
            boolean containsAll = tableClasses.containsAll(Arrays.asList(modelClass));
            Assertions.assertTrue(containsAll, tableClasses.stream().map(Class::getSimpleName).collect(Collectors.joining(",")) + " 不完全包含 " + Arrays.stream(modelClass).map(Class::getSimpleName).collect(Collectors.joining(",")));
        }));

        // 开始创建
        AutoTableBootstrap.start();
        AutoTableGlobalConfig.instance().setAutoTableReadyCallbacks(Collections.emptyList());


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
        AutoTableGlobalConfig.instance().setDeleteTableFinishCallbacks(Collections.singletonList((schema, tableName) -> {
            // 确认删除一张表
            Assertions.assertEquals(StringUtils.camelToUnderline(modelClass[1].getSimpleName()), tableName);
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
