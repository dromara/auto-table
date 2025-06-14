package org.dromara.autotable.core;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.dynamicds.DataSourceInfoExtractor;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.dromara.autotable.core.initdata.InitDataHandler;
import org.dromara.autotable.core.strategy.DatabaseBuilder;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.TableMetadata;
import org.dromara.autotable.core.utils.SpiLoader;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.core.utils.TableMetadataHandler;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 启动时进行处理的实现类
 *
 * @author chenbin.sun
 */
@Slf4j
public class AutoTableBootstrap {

    public static void start() {

        PropertyConfig autoTableProperties = AutoTableGlobalConfig.instance().getAutoTableProperties();

        // 判断模式，none或者禁用，不启动
        if (autoTableProperties.getMode() == RunMode.none || !autoTableProperties.getEnable()) {
            return;
        }

        if (autoTableProperties.getShowBanner()) {
            Banner.print();
        }

        final long start = System.currentTimeMillis();

        // 注册不同数据源策略
        registerAllDbStrategy();

        // 注册不同数据库的构建器
        registerAllDatabaseBuilder();

        // 扫描所有的类，过滤出指定注解的实体
        Set<Class<?>> classes = findAllEntityClass(autoTableProperties);

        AutoTableGlobalConfig.instance().getAutoTableReadyCallbacks().forEach(fn -> fn.ready(classes));

        // 获取对应的数据源，根据不同数据库方言，执行不同的处理
        handleAnalysis(classes);

        AutoTableGlobalConfig.instance().getAutoTableFinishCallbacks().forEach(fn -> fn.finish(classes));
        AutoTableGlobalConfig.instance().clear();
        log.info("AutoTable执行结束。耗时：{}ms", System.currentTimeMillis() - start);
    }

    /**
     * 开始分析处理模型
     * 处理ignore and repeat表
     *
     * @param classList 待处理的类
     */
    private static void handleAnalysis(Set<Class<?>> classList) {

        IDataSourceHandler datasourceHandler = AutoTableGlobalConfig.instance().getDatasourceHandler();
        // <数据源，Set<表>>
        Map<String, Set<Class<?>>> needHandleTableMap = classList.stream()
                .collect(Collectors.groupingBy(datasourceHandler::getDataSourceName, Collectors.toSet()));

        needHandleTableMap.forEach((dataSource, entityClasses) -> {
            // 使用数据源
            if (StringUtils.hasText(dataSource)) {
                log.info("使用数据源：{}", dataSource);
            }

            // 同一个数据源下，检查重名的表
            checkRepeatTableName(entityClasses);

            datasourceHandler.useDataSource(dataSource);
            DataSourceManager.setDatasourceName(dataSource);

            // 查找实体上的数据库方言标注
            String dialectOnEntity = findDialectOnEntity(dataSource, entityClasses);

            // 确保数据库存在，不存在则构建数据库
            DatabaseBuilder.BuildResult buildResult = buildDatabaseIfAbsent(dataSource, entityClasses, dialectOnEntity);
            boolean buildNewDb = buildResult != null && buildResult.isSuccess();

            try {
                // 如果实体上没有指定方言，则从链接中获取数据库方言
                String dialect = StringUtils.hasText(dialectOnEntity) ? dialectOnEntity : getDatabaseDialectFromConnection(dataSource);
                IStrategy<?, ?> databaseStrategy = AutoTableGlobalConfig.instance().getStrategy(dialect);
                if (databaseStrategy == null) {
                    log.warn("没有找到对应的数据库（{}）方言策略，无法自动维护表结构", dialect);
                    return;
                }

                // 设置当前策略
                IStrategy.setCurrentStrategy(databaseStrategy);

                // 执行数据源策略 建表
                executeStrategy(databaseStrategy, entityClasses);

                // 初始化库数据
                if (buildNewDb) {
                    // 创建的新的数据库，执行库级别的sql文件
                    InitDataHandler.initDbData();
                }
            } finally {
                if (StringUtils.hasText(dataSource)) {
                    log.info("清理数据源：{}", dataSource);
                }

                datasourceHandler.clearDataSource(dataSource);
                DataSourceManager.cleanDatasourceName();
                // 清理当前策略
                IStrategy.clean();
            }
        });
    }

    private static String findDialectOnEntity(String dataSource, Set<Class<?>> entityClasses) {
        // dialectInAnnotation可能是 只有一个空字符串的集合（未指定值），也有可能是指定的具体的方言值。但是不能是多种值
        List<String> dialectInAnnotation = entityClasses.stream()
                .map(TableMetadataHandler::getTableDialect)
                .distinct()
                .collect(Collectors.toList());
        if (dialectInAnnotation.size() > 1) {
            throw new RuntimeException("同一个数据源(" + dataSource + ")下，不能同时使用多个数据库方言[" + String.join(",", dialectInAnnotation) + "]");
        }

        String dialectOnEntity = dialectInAnnotation.get(0);
        return dialectOnEntity;
    }

    private static DatabaseBuilder.BuildResult buildDatabaseIfAbsent(String dataSource, Set<Class<?>> entityClasses, String dialectOnEntity) {
        PropertyConfig autoTableProperties = AutoTableGlobalConfig.instance().getAutoTableProperties();

        boolean autoBuildDatabase = autoTableProperties.getAutoBuildDatabase();
        if (autoBuildDatabase) {
            DataSourceInfoExtractor dataSourceInfoExtractor = AutoTableGlobalConfig.instance().getDataSourceInfoExtractor();
            DataSourceInfoExtractor.DbInfo dbInfo = dataSourceInfoExtractor.extract(DataSourceManager.getDataSource());
            if (dbInfo == null) {
                log.warn("数据库信息提取失败，跳过自动建库");
                return null;
            }
            DatabaseBuilder databaseBuilder = AutoTableGlobalConfig.instance().getDatabaseBuilder(dbInfo.jdbcUrl, dialectOnEntity);
            if (databaseBuilder != null) {
                // 构建数据库
                DatabaseBuilder.BuildResult buildResult = databaseBuilder.build(dbInfo.jdbcUrl, dbInfo.username, dbInfo.password, entityClasses, dbExists -> {
                    // 如果数据库不存在，且当前是validate模式，则抛出异常
                    if (!dbExists) {
                        boolean isValidateMode = autoTableProperties.getMode() == RunMode.validate;
                        if (isValidateMode) {
                            throw new RuntimeException("【validate模式】。数据源：" + dataSource + "数据库链接" + dbInfo.jdbcUrl + "无效，请检查数据库连接信息！");
                        }
                    }
                });
                if (buildResult.isSuccess()) {
                    // 触发回调
                    AutoTableGlobalConfig.instance().getCreateDatabaseFinishCallbacks()
                            .forEach(callback -> callback.afterCreateDatabase(dataSource, entityClasses, dbInfo));
                    return buildResult;
                }
            }
        }
        return null;
    }

    /**
     * 自动获取当前数据源的方言
     *
     * @param dataSource 数据源名称
     * @return 返回数据方言
     */
    private static String getDatabaseDialectFromConnection(String dataSource) {

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

    private static void executeStrategy(IStrategy<?, ?> databaseStrategy, Set<Class<?>> entityClasses) {
        Map<String, Set<String>> registerTableNameMap = new HashMap<>();
        for (Class<?> entityClass : entityClasses) {
            log.info("{}执行{}方言策略", entityClass.getName(), databaseStrategy.databaseDialect());
            TableMetadata tableMetadata = databaseStrategy.start(entityClass);
            // 记录声明过的表
            registerTableNameMap.computeIfAbsent(tableMetadata.getSchema(), k -> new HashSet<>()).add(tableMetadata.getTableName());
        }

        // 删除没有声明的表
        Boolean autoDropTable = AutoTableGlobalConfig.instance().getAutoTableProperties().getAutoDropTable();
        if (autoDropTable) {
            deleteUnregisterTables(registerTableNameMap, databaseStrategy);
        }
    }

    private static void deleteUnregisterTables(Map<String, Set<String>> registerTableNameMap, IStrategy<?, ?> databaseStrategy) {

        registerTableNameMap.forEach((schema, tableNames) -> {
            List<String> allTableNames = databaseStrategy.listAllTables(schema);

            // 剔除掉指定不删除的表
            String[] autoDropTableIgnores = AutoTableGlobalConfig.instance().getAutoTableProperties().getAutoDropTableIgnores();
            if (autoDropTableIgnores != null) {
                allTableNames.removeAll(Arrays.asList(autoDropTableIgnores));
            }

            // 剔除掉声明过的表
            allTableNames.removeAll(tableNames);

            // 删除剩余的表
            allTableNames.forEach(tableName -> {
                log.info("表{}{}没有声明，执行删除！", StringUtils.hasText(schema) ? schema + "." : "", tableName);
                DataSourceManager.useConnection(connection -> {
                    String sql = databaseStrategy.dropTable(schema, tableName);
                    try (Statement statement = connection.createStatement()) {
                        statement.execute(sql);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
                AutoTableGlobalConfig.instance().getDeleteTableFinishCallbacks().forEach(fn -> fn.afterDeleteTables(schema, tableName));
            });
        });
    }

    private static void checkRepeatTableName(Set<Class<?>> entityClasses) {
        Map<String, List<Class<?>>> repeatCheckMap = entityClasses.stream()
                .collect(Collectors.groupingBy(entity -> {
                    String tableSchema = TableMetadataHandler.getTableSchema(entity);
                    String tableName = TableMetadataHandler.getTableName(entity);
                    if (StringUtils.hasText(tableSchema)) {
                        return tableSchema + "." + tableName;
                    }
                    return tableName;
                }));
        for (Map.Entry<String, List<Class<?>>> repeatCheckItem : repeatCheckMap.entrySet()) {
            int sameTableNameCount = repeatCheckItem.getValue().size();
            if (sameTableNameCount > 1) {
                String tableName = repeatCheckItem.getKey();
                String repeatTableNames = repeatCheckItem.getValue().stream().map(Class::getName).collect(Collectors.joining(", "));
                throw new RuntimeException(String.format("存在重名的表：%s(%s)，请检查！", tableName, repeatTableNames));
            }
        }
    }

    private static Set<Class<?>> findAllEntityClass(PropertyConfig autoTableProperties) {
        Set<Class<?>> classes = new HashSet<>();

        Class<?>[] modelClass = autoTableProperties.getModelClass();
        // 优先添加指定的类
        boolean customModelClass = modelClass != null && modelClass.length > 0;
        if (customModelClass) {
            modelClass.forEach(classes::add);
        }
        // 添加指定的包下的类
        String[] packs = autoTableProperties.getModelPackage();
        boolean customModelPackage = packs != null && packs.length > 0;
        if (customModelPackage) {
            Set<Class<?>> packClasses = AutoTableGlobalConfig.instance().getAutoTableClassScanner().scan(packs);
            classes.addAll(packClasses);
        }

        // 扫描类和包都没有指定的情况下，扫描启动类根目录
        if (!customModelClass && !customModelPackage) {
            // 扫描根目录实体并返回
            String[] basePackages = {getBootPackage()};
            Set<Class<?>> packClasses = AutoTableGlobalConfig.instance().getAutoTableClassScanner().scan(basePackages);
            classes.addAll(packClasses);
        }

        return classes;

    }

    private static void registerAllDbStrategy() {
        List<IStrategy> strategies = SpiLoader.loadAll(IStrategy.class);
        if (strategies.isEmpty()) {
            log.warn("没有发现任何数据库策略！");
        } else {
            for (IStrategy provider : strategies) {
                log.info("注册数据库策略：{}", provider.databaseDialect());
                AutoTableGlobalConfig.instance().addStrategy(provider);
            }
        }
    }

    private static void registerAllDatabaseBuilder() {
        List<DatabaseBuilder> databaseBuilders = SpiLoader.loadAll(DatabaseBuilder.class);
        if (!databaseBuilders.isEmpty()) {
            for (DatabaseBuilder databaseBuilder : databaseBuilders) {
                AutoTableGlobalConfig.instance().addDatabaseBuilder(databaseBuilder);
            }
        }
    }

    private static String[] getModelPackage(PropertyConfig autoTableProperties) {
        String[] packs = autoTableProperties.getModelPackage();
        Class<?>[] modelClass = autoTableProperties.getModelClass();
        // 没有指定实体的class，则使用根包扫描
        if (packs.length == 0 && modelClass.length == 0) {
            packs = new String[]{getBootPackage()};
        }
        return packs;
    }

    private static String getBootPackage() {
        StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
        for (StackTraceElement stackTraceElement : stackTrace) {
            if ("main".equals(stackTraceElement.getMethodName())) {
                String mainClassName = stackTraceElement.getClassName();
                int lastDotIndex = mainClassName.lastIndexOf(".");
                return (lastDotIndex != -1 ? mainClassName.substring(0, lastDotIndex) : "");
            }
        }
        throw new RuntimeException("未找到主默认包");
    }
}
