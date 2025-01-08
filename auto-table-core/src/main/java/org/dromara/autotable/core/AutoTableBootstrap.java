package org.dromara.autotable.core;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.Ignore;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.h2.H2Strategy;
import org.dromara.autotable.core.strategy.mysql.MysqlStrategy;
import org.dromara.autotable.core.strategy.pgsql.PgsqlStrategy;
import org.dromara.autotable.core.strategy.sqlite.SqliteStrategy;
import org.dromara.autotable.core.utils.ClassScanner;
import org.dromara.autotable.core.utils.TableBeanUtils;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
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

        PropertyConfig autoTableProperties = AutoTableGlobalConfig.getAutoTableProperties();

        // 判断模式，none或者禁用，不启动
        if (autoTableProperties.getMode() == RunMode.none || !autoTableProperties.getEnable()) {
            return;
        }

        if (autoTableProperties.getShowBanner()) {
            Banner.print();
        }

        final long start = System.currentTimeMillis();

        // 注册内置的不同数据源策略
        AutoTableGlobalConfig.addStrategy(new MysqlStrategy());
        AutoTableGlobalConfig.addStrategy(new PgsqlStrategy());
        AutoTableGlobalConfig.addStrategy(new SqliteStrategy());
        AutoTableGlobalConfig.addStrategy(new H2Strategy());

        // 从包package中获取所有的Class
        Set<Class<? extends Annotation>> includeAnnotations = new HashSet<>(
                Collections.singletonList(AutoTable.class)
        );
        // 添加自定义的注解
        includeAnnotations.addAll(AutoTableGlobalConfig.getAutoTableOrmFrameAdapter().scannerAnnotations());
        Set<Class<? extends Annotation>> ignoreAnnotations = new HashSet<>(Collections.singleton(Ignore.class));
        // 经过自定义的拦截器，修改最终影响自动建表的注解
        AutoTableGlobalConfig.getAutoTableAnnotationInterceptor().intercept(includeAnnotations, ignoreAnnotations);

        // 扫描所有的类，过滤出指定注解的实体
        Class<?>[] modelClass = autoTableProperties.getModelClass();
        Set<Class<?>> classes = new HashSet<>(Arrays.asList(modelClass));
        String[] packs = getModelPackage(autoTableProperties);
        Set<Class<?>> packClasses = ClassScanner.scan(packs, includeAnnotations, ignoreAnnotations);
        classes.addAll(packClasses);

        AutoTableGlobalConfig.getAutoTableReadyCallback().ready(classes);

        // 获取对应的数据源，根据不同数据库方言，执行不同的处理
        IDataSourceHandler datasourceHandler = AutoTableGlobalConfig.getDatasourceHandler();
        datasourceHandler.handleAnalysis(classes, (databaseDialect, entityClasses) -> {

            // 同一个数据源下，检查重名的表
            Map<String, List<Class<?>>> repeatCheckMap = entityClasses.stream()
                    .collect(Collectors.groupingBy(entity -> TableBeanUtils.getTableSchema(entity) + "." + TableBeanUtils.getTableName(entity)));
            for (Map.Entry<String, List<Class<?>>> repeatCheckItem : repeatCheckMap.entrySet()) {
                int sameTableNameCount = repeatCheckItem.getValue().size();
                if (sameTableNameCount > 1) {
                    String tableName = repeatCheckItem.getKey();
                    throw new RuntimeException(String.format("存在重名的表：%s(%s)，请检查！", tableName,
                            String.join(",", repeatCheckItem.getValue().stream().map(Class::getName).collect(Collectors.toSet()))));
                }
            }

            // 查找对应的数据源策略
            IStrategy<?, ?, ?> databaseStrategy = AutoTableGlobalConfig.getStrategy(databaseDialect);
            if (databaseStrategy != null) {
                for (Class<?> entityClass : entityClasses) {
                    databaseStrategy.start(entityClass);
                }
            } else {
                log.warn("没有找到对应的数据库（{}）方言策略，无法自动维护表结构", databaseDialect);
            }
        });
        AutoTableGlobalConfig.getAutoTableFinishCallback().finish(classes);
        log.info("AutoTable执行结束。耗时：{}ms", System.currentTimeMillis() - start);
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
