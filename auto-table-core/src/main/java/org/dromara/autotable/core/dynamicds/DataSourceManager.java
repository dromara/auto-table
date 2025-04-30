package org.dromara.autotable.core.dynamicds;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author don
 */
@Slf4j
public class DataSourceManager {

    /**
     * 当前数据源名称
     */
    private static final ThreadLocal<String> DATASOURCE_NAME_THREAD_LOCAL = new ThreadLocal<>();
    /**
     * 当前数据源
     */
    private static final ThreadLocal<DataSource> DATA_SOURCE_THREAD_LOCAL = new ThreadLocal<>();

    public static <R> R useConnection(Function<Connection, R> function) {
        DataSource dataSource = getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            return function.apply(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void useConnection(Consumer<Connection> consumer) {
        DataSource dataSource = getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            consumer.accept(connection);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setDataSource(@NonNull DataSource dataSource) {
        DataSourceManager.DATA_SOURCE_THREAD_LOCAL.set(dataSource);
    }

    public static DataSource getDataSource() {
        DataSource sessionFactory = DATA_SOURCE_THREAD_LOCAL.get();
        if (sessionFactory == null) {
            throw new RuntimeException("当前数据源下，未找到对应的SqlSessionFactory");
        }
        return sessionFactory;
    }

    public static void cleanDataSource() {
        DATA_SOURCE_THREAD_LOCAL.remove();
    }

    public static void setDatasourceName(@NonNull String datasourceName) {
        DATASOURCE_NAME_THREAD_LOCAL.set(datasourceName);
    }

    public static String getDatasourceName() {
        String datasourceName = DATASOURCE_NAME_THREAD_LOCAL.get();
        if (datasourceName == null) {
            log.error("当前数据源下，未找到对应的DatasourceName");
        }
        return datasourceName;
    }

    public static void cleanDatasourceName() {
        DATASOURCE_NAME_THREAD_LOCAL.remove();
    }
}
