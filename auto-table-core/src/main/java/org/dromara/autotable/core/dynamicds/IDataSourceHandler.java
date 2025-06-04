package org.dromara.autotable.core.dynamicds;

import lombok.NonNull;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.strategy.DatabaseBuilder;
import org.dromara.autotable.core.utils.DataSourceInfoExtractor;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.core.utils.TableMetadataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            if (StringUtils.hasText(dataSource)) {
                log.info("使用数据源：{}", dataSource);
            }
            this.useDataSource(dataSource);
            DataSourceManager.setDatasourceName(dataSource);

            // 确保数据库存在，不存在则构建数据库
            Boolean autoBuildDatabase = AutoTableGlobalConfig.instance().getAutoTableProperties().getAutoBuildDatabase();
            if (autoBuildDatabase) {
                DataSourceInfoExtractor.DbInfo dbInfo = DataSourceInfoExtractor.extract(DataSourceManager.getDataSource());
                DatabaseBuilder databaseBuilder = AutoTableGlobalConfig.instance().getDatabaseBuilder(dbInfo.jdbcUrl, entityClasses);
                if (databaseBuilder != null) {
                    boolean buildSuccess = databaseBuilder.buildIfAbsent(dbInfo.jdbcUrl, dbInfo.username, dbInfo.password);
                    if (buildSuccess) {
                        // 触发回调
                        AutoTableGlobalConfig.instance().getCreateDatabaseFinishCallbacks()
                                .forEach(callback -> callback.afterCreateDatabase(dataSource, entityClasses, dbInfo));
                    }
                }
            }

            // 常规情况下，同一个数据源下，也只会有一种数据库方言（Dialect），所以Map的大小应该只有一个
            // 【错误】非常规情况，同一个数据源下，@AutoTable指定了dialect属性
            Map<String, Set<Class<?>>> groupByDialect = entityClasses.stream().collect(
                    Collectors.groupingBy(
                            // 获取数据库方言，可能来自实体类注解，或者数据库连接
                            entityClass -> this.getDatabaseDialect(dataSource, entityClass),
                            Collectors.toSet()
                    )
            );
            if (groupByDialect.size() > 1) {
                throw new RuntimeException("同一个数据源(" + dataSource + ")下，不能同时使用多个数据库方言[" + String.join(",", groupByDialect.keySet()) + "]");
            }

            try {
                groupByDialect.forEach(consumer);
            } finally {
                if (StringUtils.hasText(dataSource)) {
                    log.info("清理数据源：{}", dataSource);
                }
                this.clearDataSource(dataSource);
                DataSourceManager.cleanDatasourceName();
            }
        });
    }

    /**
     * 自动获取当前数据源的方言
     *
     * @param dataSource  数据源名称
     * @param entityClass 实体类
     * @return 返回数据方言
     */
    default String getDatabaseDialect(String dataSource, Class<?> entityClass) {

        // 优先使用注解上的自定义方言
        if (entityClass != null) {
            String tableDialect = TableMetadataHandler.getTableDialect(entityClass);
            if (StringUtils.hasText(tableDialect)) {
                log.info("使用注解上的方言：{}", tableDialect);
                return tableDialect;
            }
        }

        return getDatabaseDialect(dataSource);
    }

    /**
     * 自动获取当前数据源的方言
     *
     * @param dataSource 数据源名称
     * @return 返回数据方言
     */
    default String getDatabaseDialect(String dataSource) {

        return DataSourceManager.useConnection(connection -> {
            try {
                // 通过连接获取DatabaseMetaData对象
                DatabaseMetaData metaData = connection.getMetaData();
                // 获取数据库方言
                String databaseProductName = metaData.getDatabaseProductName();
                log.debug("数据库链接 => {}, 方言 => {}", metaData.getURL(), databaseProductName);
                return databaseProductName;
            } catch (SQLException e) {
                throw new RuntimeException("获取数据方言失败", e);
            }
        });
    }

    /**
     * 多数据源场景：切换指定的数据源
     *
     * @param dataSourceName 数据源名称
     */
    void useDataSource(String dataSourceName);

    /**
     * 多数据源场景：清除当前数据源
     *
     * @param dataSourceName 数据源名称
     */
    void clearDataSource(String dataSourceName);

    /**
     * 多数据源场景：获取指定类的数据库数据源
     *
     * @param clazz 指定类
     * @return 数据源名称，表分组的依据，届时，根据该值分组所有的表，同一数据源下的统一处理
     */
    @NonNull
    String getDataSourceName(Class<?> clazz);
}
