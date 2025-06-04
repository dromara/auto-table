package org.dromara.autotable.core;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.dromara.autotable.core.strategy.DatabaseBuilder;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.TableMetadata;
import org.dromara.autotable.core.utils.SpiLoader;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.core.utils.TableMetadataHandler;

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
        IDataSourceHandler datasourceHandler = AutoTableGlobalConfig.instance().getDatasourceHandler();
        datasourceHandler.handleAnalysis(classes, (databaseDialect, entityClasses) -> {

            // 同一个数据源下，检查重名的表
            checkRepeatTableName(entityClasses);

            // 查找对应的数据源策略并执行
            start(databaseDialect, entityClasses);
        });
        AutoTableGlobalConfig.instance().getAutoTableFinishCallbacks().forEach(fn -> fn.finish(classes));
        AutoTableGlobalConfig.instance().clear();
        log.info("AutoTable执行结束。耗时：{}ms", System.currentTimeMillis() - start);
    }

    private static void start(String databaseDialect, Set<Class<?>> entityClasses) {
        IStrategy<?, ?> databaseStrategy = AutoTableGlobalConfig.instance().getStrategy(databaseDialect);
        if (databaseStrategy != null) {
            Map<String, Set<String>> registerTableNameMap = new HashMap<>();
            for (Class<?> entityClass : entityClasses) {
                log.info("{}执行{}方言策略", entityClass.getName(), databaseDialect);
                TableMetadata tableMetadata = databaseStrategy.start(entityClass);
                // 记录声明过的表
                registerTableNameMap.computeIfAbsent(tableMetadata.getSchema(), k -> new HashSet<>()).add(tableMetadata.getTableName());
            }

            // 删除没有声明的表
            Boolean autoDropTable = AutoTableGlobalConfig.instance().getAutoTableProperties().getAutoDropTable();
            if (autoDropTable) {
                deleteUnregisterTables(registerTableNameMap, databaseStrategy);
            }
        } else {
            log.warn("没有找到对应的数据库（{}）方言策略，无法自动维护表结构", databaseDialect);
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
                .collect(Collectors.groupingBy(entity -> TableMetadataHandler.getTableSchema(entity) + "." + TableMetadataHandler.getTableName(entity)));
        for (Map.Entry<String, List<Class<?>>> repeatCheckItem : repeatCheckMap.entrySet()) {
            int sameTableNameCount = repeatCheckItem.getValue().size();
            if (sameTableNameCount > 1) {
                String tableName = repeatCheckItem.getKey();
                throw new RuntimeException(String.format("存在重名的表：%s(%s)，请检查！", tableName,
                        String.join(",", repeatCheckItem.getValue().stream().map(Class::getName).collect(Collectors.toSet()))));
            }
        }
    }

    private static Set<Class<?>> findAllEntityClass(PropertyConfig autoTableProperties) {
        Class<?>[] modelClass = autoTableProperties.getModelClass();
        Set<Class<?>> classes = new HashSet<>(Arrays.asList(modelClass));
        String[] packs = getModelPackage(autoTableProperties);
        Set<Class<?>> packClasses = AutoTableGlobalConfig.instance().getAutoTableClassScanner().scan(packs);
        classes.addAll(packClasses);
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
