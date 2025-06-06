package org.dromara.autotable.test.core.dynamicdatasource;

import lombok.NonNull;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author don
 */
public class DynamicDataSourceHandler implements IDataSourceHandler {

    private static final Map<String, String> CONFIG_MAP = new HashMap<String, String>() {{
        put("dm", "mybatis-config-dmsql.xml");
    }};
    private static final Map<String, DataSource> STRING_DATA_SOURCE_MAP = new HashMap<>();

    @Override
    public void useDataSource(String dataSourceName) {

        DataSource dataSource = STRING_DATA_SOURCE_MAP.computeIfAbsent(dataSourceName, key -> {

            String resource = CONFIG_MAP.get(dataSourceName);

            try (InputStream inputStream = DynamicDataSourceHandler.class.getClassLoader().getResourceAsStream(resource)) {
                // 使用SqlSessionFactoryBuilder加载配置文件
                SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
                return sessionFactory.getConfiguration().getEnvironment().getDataSource();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // 设置新的dataSource
        DataSourceManager.setDataSource(dataSource);
    }

    @Override
    public void clearDataSource(String dataSourceName) {
        DataSourceManager.cleanDataSource();
    }

    @Override
    public @NonNull String getDataSourceName(Class<?> clazz) {
        Ds annotation = clazz.getAnnotation(Ds.class);
        if (annotation != null) {
            return annotation.value();
        }
        // 默认mysql
        return "mysql";
    }
}
