package org.dromara.autotable.core;

import lombok.Getter;
import lombok.Setter;
import org.dromara.autotable.core.callback.AutoTableFinishCallback;
import org.dromara.autotable.core.callback.AutoTableReadyCallback;
import org.dromara.autotable.core.callback.CreateTableFinishCallback;
import org.dromara.autotable.core.callback.ModifyTableFinishCallback;
import org.dromara.autotable.core.callback.RunStateCallback;
import org.dromara.autotable.core.callback.ValidateFinishCallback;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.converter.JavaTypeToDatabaseTypeConverter;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.dromara.autotable.core.dynamicds.impl.DefaultDataSourceHandler;
import org.dromara.autotable.core.interceptor.AutoTableAnnotationInterceptor;
import org.dromara.autotable.core.interceptor.BuildTableMetadataInterceptor;
import org.dromara.autotable.core.interceptor.CreateTableInterceptor;
import org.dromara.autotable.core.interceptor.ModifyTableInterceptor;
import org.dromara.autotable.core.recordsql.RecordSqlHandler;
import org.dromara.autotable.core.strategy.CompareTableInfo;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.TableMetadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局配置
 *
 * @author don
 */
public class AutoTableGlobalConfig {

    /**
     * 全局配置
     */
    @Setter
    @Getter
    private static PropertyConfig autoTableProperties = new PropertyConfig();

    /**
     * class扫描器
     */
    @Setter
    @Getter
    private static AutoTableClassScanner autoTableClassScanner = new AutoTableClassScanner() {};

    /**
     * 数据源处理器
     */
    @Setter
    @Getter
    private static IDataSourceHandler datasourceHandler = new DefaultDataSourceHandler();

    /**
     * 自定义注解查找器
     */
    @Setter
    @Getter
    private static AutoTableAnnotationFinder autoTableAnnotationFinder = new AutoTableAnnotationFinder() {
    };

    /**
     * ORM框架适配器
     */
    @Setter
    @Getter
    private static AutoTableMetadataAdapter autoTableMetadataAdapter = new AutoTableMetadataAdapter() {
    };

    /**
     * 数据库类型转换
     */
    @Setter
    @Getter
    private static JavaTypeToDatabaseTypeConverter javaTypeToDatabaseTypeConverter = new JavaTypeToDatabaseTypeConverter() {
    };

    /**
     * 自定义记录sql的方式
     */
    @Setter
    @Getter
    private static RecordSqlHandler customRecordSqlHandler = sqlLog -> {
    };

    /**
     * 自动表注解拦截器
     */
    @Setter
    @Getter
    private static AutoTableAnnotationInterceptor autoTableAnnotationInterceptor = (includeAnnotations, excludeAnnotations) -> {
    };

    /**
     * 创建表拦截
     */
    @Setter
    @Getter
    private static BuildTableMetadataInterceptor buildTableMetadataInterceptor = (databaseDialect, tableMetadata) -> {
    };

    /**
     * 创建表拦截
     */
    @Setter
    @Getter
    private static CreateTableInterceptor createTableInterceptor = (databaseDialect, tableMetadata) -> {
    };

    /**
     * 修改表拦截
     */
    @Setter
    @Getter
    private static ModifyTableInterceptor modifyTableInterceptor = (databaseDialect, tableMetadata, compareTableInfo) -> {
    };

    /**
     * 验证完成回调
     */
    @Setter
    @Getter
    private static ValidateFinishCallback validateFinishCallback = (status, databaseDialect, compareTableInfo) -> {
    };

    /**
     * 创建表回调
     */
    @Setter
    @Getter
    private static CreateTableFinishCallback createTableFinishCallback = (databaseDialect, tableMetadata) -> {
    };

    /**
     * 修改表回调
     */
    @Setter
    @Getter
    private static ModifyTableFinishCallback modifyTableFinishCallback = (databaseDialect, tableMetadata, compareTableInfo) -> {

    };
    /**
     * 单个表执行前后回调
     */
    @Setter
    @Getter
    private static RunStateCallback runStateCallback = new RunStateCallback() {
        @Override
        public void before(Class<?> tableClass) {
        }

        @Override
        public void after(Class<?> tableClass) {
        }
    };

    /**
     * 执行结束回调
     */
    @Setter
    @Getter
    private static AutoTableReadyCallback autoTableReadyCallback = (classes) -> {

    };

    /**
     * 执行结束回调
     */
    @Setter
    @Getter
    private static AutoTableFinishCallback autoTableFinishCallback = (classes) -> {

    };

    private final static Map<String, IStrategy<? extends TableMetadata, ? extends CompareTableInfo, ?>> STRATEGY_MAP = new HashMap<>();

    public static void addStrategy(IStrategy<? extends TableMetadata, ? extends CompareTableInfo, ?> strategy) {
        STRATEGY_MAP.put(strategy.databaseDialect(), strategy);
        JavaTypeToDatabaseTypeConverter.addTypeMapping(strategy.databaseDialect(), strategy.typeMapping());
    }

    public static IStrategy<?, ?, ?> getStrategy(String databaseDialect) {
        return STRATEGY_MAP.get(databaseDialect);
    }

    public static Collection<IStrategy<?, ?, ?>> getAllStrategy() {
        return STRATEGY_MAP.values();
    }

}
