package com.tangzc.autotable.core.dynamicds;

import lombok.NonNull;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author don
 */
public interface IDataSourceHandler {

    Logger log = LoggerFactory.getLogger(IDataSourceHandler.class);

    /**
     * 开始分析处理模型
     * 处理ignore and repeat表
     *
     * @param classList 待处理的类
     * @param consumer  实体消费回调
     */
    default void handleAnalysis(Set<Class<?>> classList, BiConsumer<String, Set<Class<?>>> consumer) {

        // <数据源，Set<表>>
        Map<String, Set<Class<?>>> needHandleTableMap = classList.stream()
                .collect(Collectors.groupingBy(this::getDataSourceName, Collectors.toSet()));

        needHandleTableMap.forEach((dataSource, entityClasses) -> {
            // 使用数据源
            log.info("使用数据源：{}", dataSource);
            this.useDataSource(dataSource);
            DatasourceNameManager.setDatasourceName(dataSource);
            try {
                String databaseDialect = this.getDatabaseDialect(dataSource);
                log.info("数据库方言（" + databaseDialect + "）");
                consumer.accept(databaseDialect, entityClasses);
            } finally {
                log.info("清理数据源：{}", dataSource);
                this.clearDataSource(dataSource);
                DatasourceNameManager.cleanDatasourceName();
            }
        });
    }


    /**
     * 自动获取当前数据源的方言
     *
     * @param dataSource 数据源名称
     * @return 返回数据方言
     */
    default String getDatabaseDialect(String dataSource) {

        // 获取Configuration对象
        Configuration configuration = SqlSessionFactoryManager.getSqlSessionFactory().getConfiguration();

        try (Connection connection = configuration.getEnvironment().getDataSource().getConnection()) {
            // 通过连接获取DatabaseMetaData对象
            DatabaseMetaData metaData = connection.getMetaData();
            log.info("数据库链接 => {}", metaData.getURL());
            // 获取数据库方言
            return metaData.getDatabaseProductName();
        } catch (SQLException e) {
            throw new RuntimeException("获取数据方言失败", e);
        }
    }

    /**
     * 切换指定的数据源
     *
     * @param dataSourceName 数据源名称
     */
    void useDataSource(String dataSourceName);

    /**
     * 清除当前数据源
     *
     * @param dataSourceName 数据源名称
     */
    void clearDataSource(String dataSourceName);

    /**
     * 获取指定类的数据库数据源
     *
     * @param clazz 指定类
     * @return 数据源名称，表分组的依据，届时，根据该值分组所有的表，同一数据源下的统一处理
     */
    @NonNull String getDataSourceName(Class<?> clazz);
}
