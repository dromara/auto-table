package org.dromara.autotable.core;

import lombok.Getter;
import lombok.Setter;
import org.dromara.autotable.core.callback.AutoTableFinishCallback;
import org.dromara.autotable.core.callback.AutoTableReadyCallback;
import org.dromara.autotable.core.callback.CompareTableFinishCallback;
import org.dromara.autotable.core.callback.CreateTableFinishCallback;
import org.dromara.autotable.core.callback.DeleteTableFinishCallback;
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
     * 方便单元测试，此处为每个线程创建一个配置实例
     */
    private static final ThreadLocal<AutoTableGlobalConfig> instance = ThreadLocal.withInitial(AutoTableGlobalConfig::new);

    /**
     * 获取当前线程的配置
     */
    public static AutoTableGlobalConfig instance() {
        return instance.get();
    }

    /**
     * 清除当前线程的配置
     */
    public static void clear() {
        instance.remove();
    }

    /**
     * 全局配置
     */
    @Setter
    @Getter
    private PropertyConfig autoTableProperties = new PropertyConfig();

    /**
     * class扫描器
     */
    @Setter
    @Getter
    private AutoTableClassScanner autoTableClassScanner = new AutoTableClassScanner() {
    };

    /**
     * 数据源处理器
     */
    @Setter
    @Getter
    private IDataSourceHandler datasourceHandler = new DefaultDataSourceHandler();

    /**
     * 自定义注解查找器
     */
    @Setter
    @Getter
    private AutoTableAnnotationFinder autoTableAnnotationFinder = new AutoTableAnnotationFinder() {
    };

    /**
     * ORM框架适配器
     */
    @Setter
    @Getter
    private AutoTableMetadataAdapter autoTableMetadataAdapter = new AutoTableMetadataAdapter() {
    };

    /**
     * 数据库类型转换
     */
    @Setter
    @Getter
    private JavaTypeToDatabaseTypeConverter javaTypeToDatabaseTypeConverter = new JavaTypeToDatabaseTypeConverter() {
    };

    /**
     * 自定义记录sql的方式
     */
    @Setter
    @Getter
    private RecordSqlHandler customRecordSqlHandler = sqlLog -> {
    };

    /* 拦截器与回调监听 ↓↓↓↓↓↓↓↓↓↓↓↓↓ */

    /**
     * 自动表注解拦截器
     */
    @Setter
    @Getter
    private List<AutoTableAnnotationInterceptor> autoTableAnnotationInterceptors = new ArrayList<>();

    /**
     * 创建表拦截
     */
    @Setter
    @Getter
    private List<BuildTableMetadataInterceptor> buildTableMetadataInterceptors = new ArrayList<>();

    /**
     * 创建表拦截
     */
    @Setter
    @Getter
    private List<CreateTableInterceptor> createTableInterceptors = new ArrayList<>();

    /**
     * 比对完回调
     */
    @Setter
    @Getter
    private List<CompareTableFinishCallback> CompareTableFinishCallbacks = new ArrayList<>();

    /**
     * 修改表拦截
     */
    @Setter
    @Getter
    private List<ModifyTableInterceptor> modifyTableInterceptors = new ArrayList<>();

    /**
     * 验证完成回调
     */
    @Setter
    @Getter
    private List<ValidateFinishCallback> validateFinishCallbacks = new ArrayList<>();

    /**
     * 创建表回调
     */
    @Setter
    @Getter
    private List<CreateTableFinishCallback> createTableFinishCallbacks = new ArrayList<>();

    /**
     * 修改表回调
     */
    @Setter
    @Getter
    private List<ModifyTableFinishCallback> modifyTableFinishCallbacks = new ArrayList<>();

    /**
     * 删除表回调
     */
    @Setter
    @Getter
    private List<DeleteTableFinishCallback> deleteTableFinishCallbacks = new ArrayList<>();

    /**
     * 单个表执行前回调
     */
    @Setter
    @Getter
    private List<RunBeforeCallback> runBeforeCallbacks = new ArrayList<>();

    /**
     * 单个表执行后回调
     */
    @Setter
    @Getter
    private List<RunAfterCallback> runAfterCallbacks = new ArrayList<>();

    /**
     * 所有准备工作完成，执行前回调
     */
    @Setter
    @Getter
    private List<AutoTableReadyCallback> autoTableReadyCallbacks = new ArrayList<>();

    /**
     * 所有表执行结束回调
     */
    @Setter
    @Getter
    private List<AutoTableFinishCallback> autoTableFinishCallbacks = new ArrayList<>();

    /* 拦截器与回调监听 ↑↑↑↑↑↑↑↑↑ */

    private final Map<String, IStrategy<? extends TableMetadata, ? extends CompareTableInfo>> STRATEGY_MAP = new HashMap<>();

    public void addStrategy(IStrategy<? extends TableMetadata, ? extends CompareTableInfo> strategy) {
        STRATEGY_MAP.put(strategy.databaseDialect(), strategy);
        JavaTypeToDatabaseTypeConverter.addTypeMapping(strategy.databaseDialect(), strategy.typeMapping());
    }

    public IStrategy<?, ?> getStrategy(String databaseDialect) {
        return STRATEGY_MAP.get(databaseDialect);
    }

    public Collection<IStrategy<?, ?>> getAllStrategy() {
        return STRATEGY_MAP.values();
    }

}
