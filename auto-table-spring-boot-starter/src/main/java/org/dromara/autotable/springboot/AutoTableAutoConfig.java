package org.dromara.autotable.springboot;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.AutoTableAnnotationFinder;
import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableClassScanner;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.AutoTableMetadataAdapter;
import org.dromara.autotable.core.callback.AutoTableFinishCallback;
import org.dromara.autotable.core.callback.AutoTableReadyCallback;
import org.dromara.autotable.core.callback.CompareTableFinishCallback;
import org.dromara.autotable.core.callback.CreateDatabaseFinishCallback;
import org.dromara.autotable.core.callback.CreateTableFinishCallback;
import org.dromara.autotable.core.callback.DeleteTableFinishCallback;
import org.dromara.autotable.core.callback.ModifyTableFinishCallback;
import org.dromara.autotable.core.callback.RunAfterCallback;
import org.dromara.autotable.core.callback.RunBeforeCallback;
import org.dromara.autotable.core.callback.ValidateFinishCallback;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.converter.JavaTypeToDatabaseTypeConverter;
import org.dromara.autotable.core.dynamicds.DataSourceInfoExtractor;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.dromara.autotable.core.interceptor.AutoTableAnnotationInterceptor;
import org.dromara.autotable.core.interceptor.BuildTableMetadataInterceptor;
import org.dromara.autotable.core.interceptor.CreateTableInterceptor;
import org.dromara.autotable.core.interceptor.ModifyTableInterceptor;
import org.dromara.autotable.core.recordsql.RecordSqlHandler;
import org.dromara.autotable.core.strategy.CompareTableInfo;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.TableMetadata;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Lazy;

import javax.sql.DataSource;
import java.util.stream.Collectors;

/**
 * @author don
 */
@Slf4j
@Lazy(false)
@ConditionalOnMissingBean(AutoTableAutoConfig.class)
public class AutoTableAutoConfig {

    public AutoTableAutoConfig(
            PropertyConfig propertiesConfig,
            ObjectProvider<InitializeBeans> initializeBeans,
            ObjectProvider<DataSource> dataSource,
            ObjectProvider<IStrategy<? extends TableMetadata, ? extends CompareTableInfo>> strategies,
            ObjectProvider<AutoTableClassScanner> autoTableClassScanner,
            ObjectProvider<AutoTableAnnotationFinder> autoTableAnnotationFinder,
            ObjectProvider<AutoTableMetadataAdapter> autoTableMetadataAdapter,
            ObjectProvider<IDataSourceHandler> dynamicDataSourceHandler,
            ObjectProvider<DataSourceInfoExtractor> dataSourceInfoExtractor,
            ObjectProvider<RecordSqlHandler> recordSqlHandler,
            /* 拦截器 */
            ObjectProvider<AutoTableAnnotationInterceptor> autoTableAnnotationInterceptor,
            ObjectProvider<BuildTableMetadataInterceptor> buildTableMetadataInterceptor,
            ObjectProvider<CreateTableInterceptor> createTableInterceptor,
            ObjectProvider<ModifyTableInterceptor> modifyTableInterceptor,
            /* 回调事件 */
            ObjectProvider<CreateDatabaseFinishCallback> createDatabaseFinishCallbacks,
            ObjectProvider<CreateTableFinishCallback> createTableFinishCallback,
            ObjectProvider<ModifyTableFinishCallback> modifyTableFinishCallback,
            ObjectProvider<CompareTableFinishCallback> compareTableFinishCallbacks,
            ObjectProvider<DeleteTableFinishCallback> deleteTableFinishCallbacks,
            ObjectProvider<RunBeforeCallback> runBeforeCallbacks,
            ObjectProvider<RunAfterCallback> runAfterCallbacks,
            ObjectProvider<ValidateFinishCallback> validateFinishCallback,
            ObjectProvider<AutoTableReadyCallback> autoTableReadyCallback,
            ObjectProvider<AutoTableFinishCallback> autoTableFinishCallbacks,

            ObjectProvider<JavaTypeToDatabaseTypeConverter> javaTypeToDatabaseTypeConverter) {

        initializeBeans.orderedStream().forEachOrdered(initializeBean -> {
            log.info("初始化{}", initializeBean.getClass().getName());
        });

        // 默认设置全局的dataSource
        dataSource.ifUnique(DataSourceManager::setDataSource);

        // 假如有注解扫描的包，就覆盖设置
        if (AutoTableImportRegister.basePackagesFromAnno != null) {
            propertiesConfig.setModelPackage(AutoTableImportRegister.basePackagesFromAnno);
        }
        // 假如有注解扫描的类，就覆盖设置
        if (AutoTableImportRegister.classesFromAnno != null) {
            propertiesConfig.setModelClass(AutoTableImportRegister.classesFromAnno);
        }
        AutoTableGlobalConfig.instance().setAutoTableProperties(propertiesConfig);

        // 配置自定义的注解扫描器。若没有，则配置内置的注解扫描器
        AutoTableGlobalConfig.instance().setAutoTableAnnotationFinder(autoTableAnnotationFinder.getIfAvailable(CustomAnnotationFinder::new));

        // 如果有自定义的数据库策略，则加载
        strategies.stream().forEach(AutoTableGlobalConfig.instance()::addStrategy);

        // 配置自定义的class扫描器
        autoTableClassScanner.ifAvailable(AutoTableGlobalConfig.instance()::setAutoTableClassScanner);

        // 配置自定义的orm框架适配器
        autoTableMetadataAdapter.ifAvailable(AutoTableGlobalConfig.instance()::setAutoTableMetadataAdapter);

        // 配置自定义的动态数据源处理器
        dynamicDataSourceHandler.ifAvailable(AutoTableGlobalConfig.instance()::setDatasourceHandler);

        // 配置自定义的数据源解析器
        dataSourceInfoExtractor.ifAvailable(AutoTableGlobalConfig.instance()::setDataSourceInfoExtractor);

        // 配置自定义的SQL记录处理器
        recordSqlHandler.ifAvailable(AutoTableGlobalConfig.instance()::setCustomRecordSqlHandler);

        /* 拦截器 */
        // 配置自定义的注解拦截器
        AutoTableGlobalConfig.instance().setAutoTableAnnotationInterceptors(autoTableAnnotationInterceptor.orderedStream().collect(Collectors.toList()));
        // 配置自定义的创建表拦截器
        AutoTableGlobalConfig.instance().setBuildTableMetadataInterceptors(buildTableMetadataInterceptor.orderedStream().collect(Collectors.toList()));
        // 配置自定义的创建表拦截器
        AutoTableGlobalConfig.instance().setCreateTableInterceptors(createTableInterceptor.orderedStream().collect(Collectors.toList()));
        // 配置自定义的修改表拦截器
        AutoTableGlobalConfig.instance().setModifyTableInterceptors(modifyTableInterceptor.orderedStream().collect(Collectors.toList()));

        /* 回调事件 */
        // 配置自定义的创建库回调
        AutoTableGlobalConfig.instance().setCreateDatabaseFinishCallbacks(createDatabaseFinishCallbacks.orderedStream().collect(Collectors.toList()));
        // 配置自定义的创建表回调
        AutoTableGlobalConfig.instance().setCreateTableFinishCallbacks(createTableFinishCallback.orderedStream().collect(Collectors.toList()));
        // 配置自定义的修改表回调
        AutoTableGlobalConfig.instance().setModifyTableFinishCallbacks(modifyTableFinishCallback.orderedStream().collect(Collectors.toList()));
        // 配置自定义的比对表回调
        AutoTableGlobalConfig.instance().setCompareTableFinishCallbacks(compareTableFinishCallbacks.orderedStream().collect(Collectors.toList()));
        // 配置自定义的删除表回调
        AutoTableGlobalConfig.instance().setDeleteTableFinishCallbacks(deleteTableFinishCallbacks.orderedStream().collect(Collectors.toList()));
        // 配置自定义的单个表执行前回调
        AutoTableGlobalConfig.instance().setRunBeforeCallbacks(runBeforeCallbacks.orderedStream().collect(Collectors.toList()));
        // 配置自定义的单个表执行后回调
        AutoTableGlobalConfig.instance().setRunAfterCallbacks(runAfterCallbacks.orderedStream().collect(Collectors.toList()));
        // 配置自定义的验证表回调
        AutoTableGlobalConfig.instance().setValidateFinishCallbacks(validateFinishCallback.orderedStream().collect(Collectors.toList()));
        // 配置自定义的全局执行前回调
        AutoTableGlobalConfig.instance().setAutoTableReadyCallbacks(autoTableReadyCallback.orderedStream().collect(Collectors.toList()));
        // 配置自定义的全局执行后回调
        AutoTableGlobalConfig.instance().setAutoTableFinishCallbacks(autoTableFinishCallbacks.orderedStream().collect(Collectors.toList()));

        // 配置自定义的java到数据库的转换器
        javaTypeToDatabaseTypeConverter.ifAvailable(AutoTableGlobalConfig.instance()::setJavaTypeToDatabaseTypeConverter);

        this.start();
    }

    protected void start() {
        // 单元测试模式下，不主动启动
        if (!AutoTableGlobalConfig.instance().isUnitTestMode()) {
            // 启动AutoTable
            AutoTableBootstrap.start();
        }
    }
}
