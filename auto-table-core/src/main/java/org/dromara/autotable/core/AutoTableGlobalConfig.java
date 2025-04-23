package org.dromara.autotable.core;

import lombok.Getter;
import lombok.Setter;
import org.dromara.autotable.core.callback.AutoTableFinishCallback;
import org.dromara.autotable.core.callback.AutoTableReadyCallback;
import org.dromara.autotable.core.callback.CreateTableFinishCallback;
import org.dromara.autotable.core.callback.ModifyTableFinishCallback;
import org.dromara.autotable.core.callback.RunAfterCallback;
import org.dromara.autotable.core.callback.RunBeforeCallback;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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

    /* 拦截器与回调监听 ↓↓↓↓↓↓↓↓↓↓↓↓↓ */

    /**
     * 自动表注解拦截器
     */
    @Setter
    @Getter
    private static List<AutoTableAnnotationInterceptor> autoTableAnnotationInterceptors = new ArrayList<>();

    /**
     * 创建表拦截
     */
    @Setter
    @Getter
    private static List<BuildTableMetadataInterceptor> buildTableMetadataInterceptors = new ArrayList<>();

    /**
     * 创建表拦截
     */
    @Setter
    @Getter
    private static List<CreateTableInterceptor> createTableInterceptors = new ArrayList<>();

    /**
     * 修改表拦截
     */
    @Setter
    @Getter
    private static List<ModifyTableInterceptor> modifyTableInterceptors = new ArrayList<>();

    /**
     * 验证完成回调
     */
    @Setter
    @Getter
    private static List<ValidateFinishCallback> validateFinishCallbacks = new ArrayList<>();

    /**
     * 创建表回调
     */
    @Setter
    @Getter
    private static List<CreateTableFinishCallback> createTableFinishCallbacks = new ArrayList<>();

    /**
     * 修改表回调
     */
    @Setter
    @Getter
    private static List<ModifyTableFinishCallback> modifyTableFinishCallbacks = new ArrayList<>();

    /**
     * 单个表执行前回调
     */
    @Setter
    @Getter
    private static List<RunBeforeCallback> runBeforeCallbacks = new ArrayList<>();

    /**
     * 单个表执行后回调
     */
    @Setter
    @Getter
    private static List<RunAfterCallback> runAfterCallbacks = new ArrayList<>();

    /**
     * 执行结束回调
     */
    @Setter
    @Getter
    private static List<AutoTableReadyCallback> autoTableReadyCallbacks = new ArrayList<>();

    /**
     * 执行结束回调
     */
    @Setter
    @Getter
    private static List<AutoTableFinishCallback> autoTableFinishCallbacks = new ArrayList<>();

    /* 拦截器与回调监听 ↑↑↑↑↑↑↑↑↑ */

    private final static Map<String, IStrategy<? extends TableMetadata, ? extends CompareTableInfo>> STRATEGY_MAP = new HashMap<>();

    public static void addStrategy(IStrategy<? extends TableMetadata, ? extends CompareTableInfo> strategy) {
        STRATEGY_MAP.put(strategy.databaseDialect(), strategy);
        JavaTypeToDatabaseTypeConverter.addTypeMapping(strategy.databaseDialect(), strategy.typeMapping());
    }

    public static IStrategy<?, ?> getStrategy(String databaseDialect) {
        return STRATEGY_MAP.get(databaseDialect);
    }

    public static Collection<IStrategy<?, ?>> getAllStrategy() {
        return STRATEGY_MAP.values();
    }

}
