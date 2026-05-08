package org.dromara.autotable.test.core.integration;

import org.anyline.adapter.init.DefaultEnvironmentWorker;
import org.anyline.data.datasource.DataSourceHolder;
import org.anyline.metadata.Column;
import org.anyline.metadata.Index;
import org.anyline.metadata.Table;
import org.anyline.proxy.ServiceProxy;
import org.anyline.service.AnylineService;
import org.anyline.util.ConfigTable;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;

public class AnylineTest {
    public static void main(String[] args) {
        ConfigTable.METADATA_CACHE_SCOPE = 0;
        DefaultEnvironmentWorker.start();
        regDs("mysql", "mybatis-config-mysql.xml");
        regDs("pgsql", "mybatis-config-pgsql.xml");

        AnylineService.MetaDataService metadataed = ServiceProxy.service("pgsql").metadata();
        Table table = metadataed.table("anyline_test");
        List<Column> columns = table.columns();
        LinkedHashMap<String, Index> indexes = table.getIndexes();

        System.out.println("------------------------");
        System.out.println(table);
        System.out.println("------------------------");
        columns.forEach((value) -> {
            System.out.println(value);
        });
        // System.out.println("------------------------");
        // indexes.forEach((key, value) -> {
        //     System.out.println(key);
        //     DataRow metadata = value.getMetadata();
        //     metadata.forEach((k, v) -> {
        //         System.out.println(k + ": " + v);
        //     });
        // });
        // System.out.println("------------------------");
    }

    private static void regDs(String dsName, String resource) {
        try (InputStream inputStream = ApplicationAllTest.class.getClassLoader().getResourceAsStream(resource)) {// 使用SqlSessionFactoryBuilder加载配置文件
            SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            // 使用SqlSessionFactoryBuilder加载配置文件
            // 获取 Configuration 对象
            Configuration configuration = sessionFactory.getConfiguration();
            Environment environment = configuration.getEnvironment();
            DataSource dataSource = environment.getDataSource();

            DataSourceHolder.reg(dsName, dataSource, true);
            // 设置当前数据源
            // DataSourceManager.setSqlSessionFactory(sessionFactory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
