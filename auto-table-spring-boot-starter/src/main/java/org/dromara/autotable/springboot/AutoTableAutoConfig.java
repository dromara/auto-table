package org.dromara.autotable.springboot;

import org.dromara.autotable.core.AutoTableAnnotationFinder;
import org.dromara.autotable.core.AutoTableClassScanner;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.AutoTableMetadataAdapter;
import org.dromara.autotable.core.callback.AutoTableFinishCallback;
import org.dromara.autotable.core.callback.AutoTableReadyCallback;
import org.dromara.autotable.core.callback.CompareTableFinishCallback;
import org.dromara.autotable.core.callback.CreateTableFinishCallback;
import org.dromara.autotable.core.callback.ModifyTableFinishCallback;
import org.dromara.autotable.core.callback.RunAfterCallback;
import org.dromara.autotable.core.callback.RunBeforeCallback;
import org.dromara.autotable.core.callback.ValidateFinishCallback;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.converter.JavaTypeToDatabaseTypeConverter;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.interceptor.AutoTableAnnotationInterceptor;
import org.dromara.autotable.core.interceptor.BuildTableMetadataInterceptor;
import org.dromara.autotable.core.interceptor.CreateTableInterceptor;
import org.dromara.autotable.core.interceptor.ModifyTableInterceptor;
import org.dromara.autotable.core.recordsql.RecordSqlHandler;
import org.dromara.autotable.core.strategy.CompareTableInfo;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.TableMetadata;
import org.dromara.autotable.springboot.properties.AutoTableProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import javax.sql.DataSource;
import java.util.stream.Collectors;

/**
 * @author don
 */
@AutoConfigureAfter({DataSourceAutoConfiguration.class})
@ConditionalOnMissingBean(AutoTableAutoConfig.class)
public class AutoTableAutoConfig {

    public AutoTableAutoConfig(
            AutoTableProperties autoTableProperties,
            ObjectProvider<DataSource> dataSource,
            ObjectProvider<IStrategy<? extends TableMetadata, ? extends CompareTableInfo>> strategies,
            ObjectProvider<AutoTableClassScanner> autoTableClassScanner,
            ObjectProvider<AutoTableAnnotationFinder> autoTableAnnotationFinder,
            ObjectProvider<AutoTableMetadataAdapter> autoTableMetadataAdapter,
            ObjectProvider<IDataSourceHandler> dynamicDataSourceHandler,
            ObjectProvider<RecordSqlHandler> recordSqlHandler,
            /* 拦截器 */
            ObjectProvider<AutoTableAnnotationInterceptor> autoTableAnnotationInterceptor,
            ObjectProvider<BuildTableMetadataInterceptor> buildTableMetadataInterceptor,
            ObjectProvider<CreateTableInterceptor> createTableInterceptor,
            ObjectProvider<ModifyTableInterceptor> modifyTableInterceptor,
            /* 回调事件 */
            ObjectProvider<CreateTableFinishCallback> createTableFinishCallback,
            ObjectProvider<ModifyTableFinishCallback> modifyTableFinishCallback,
            ObjectProvider<CompareTableFinishCallback> compareTableFinishCallbacks,
            ObjectProvider<RunBeforeCallback> runBeforeCallbacks,
            ObjectProvider<RunAfterCallback> runAfterCallbacks,
            ObjectProvider<ValidateFinishCallback> validateFinishCallback,
            ObjectProvider<AutoTableReadyCallback> autoTableReadyCallback,
            ObjectProvider<AutoTableFinishCallback> autoTableFinishCallbacks,

            ObjectProvider<JavaTypeToDatabaseTypeConverter> javaTypeToDatabaseTypeConverter) {

        // 默认设置全局的dataSource
        dataSource.ifUnique(DataSourceManager::setDataSource);

        // 设置全局的配置
        PropertyConfig propertiesConfig = autoTableProperties.toConfig();
        // 假如有注解扫描的包，就覆盖设置
        if (AutoTableImportRegister.basePackagesFromAnno != null) {
            propertiesConfig.setModelPackage(AutoTableImportRegister.basePackagesFromAnno);
        }
        // 假如有注解扫描的类，就覆盖设置
        if (AutoTableImportRegister.classesFromAnno != null) {
            propertiesConfig.setModelClass(AutoTableImportRegister.classesFromAnno);
        }
        AutoTableGlobalConfig.setAutoTableProperties(propertiesConfig);

        // 配置自定义的注解扫描器。若没有，则配置内置的注解扫描器
        AutoTableGlobalConfig.setAutoTableAnnotationFinder(autoTableAnnotationFinder.getIfAvailable(CustomAnnotationFinder::new));

        // 如果有自定义的数据库策略，则加载
        strategies.stream().forEach(AutoTableGlobalConfig::addStrategy);

        // 配置自定义的class扫描器
        autoTableClassScanner.ifAvailable(AutoTableGlobalConfig::setAutoTableClassScanner);

        // 配置自定义的orm框架适配器
        autoTableMetadataAdapter.ifAvailable(AutoTableGlobalConfig::setAutoTableMetadataAdapter);

        // 配置自定义的动态数据源处理器
        dynamicDataSourceHandler.ifAvailable(AutoTableGlobalConfig::setDatasourceHandler);

        // 配置自定义的SQL记录处理器
        recordSqlHandler.ifAvailable(AutoTableGlobalConfig::setCustomRecordSqlHandler);

        /* 拦截器 */
        // 配置自定义的注解拦截器
        AutoTableGlobalConfig.setAutoTableAnnotationInterceptors(autoTableAnnotationInterceptor.orderedStream().collect(Collectors.toList()));
        // 配置自定义的创建表拦截器
        AutoTableGlobalConfig.setBuildTableMetadataInterceptors(buildTableMetadataInterceptor.orderedStream().collect(Collectors.toList()));
        // 配置自定义的创建表拦截器
        AutoTableGlobalConfig.setCreateTableInterceptors(createTableInterceptor.orderedStream().collect(Collectors.toList()));
        // 配置自定义的修改表拦截器
        AutoTableGlobalConfig.setModifyTableInterceptors(modifyTableInterceptor.orderedStream().collect(Collectors.toList()));

        /* 回调事件 */
        // 配置自定义的创建表回调
        AutoTableGlobalConfig.setCreateTableFinishCallbacks(createTableFinishCallback.orderedStream().collect(Collectors.toList()));
        // 配置自定义的修改表回调
        AutoTableGlobalConfig.setModifyTableFinishCallbacks(modifyTableFinishCallback.orderedStream().collect(Collectors.toList()));
        // 配置自定义的比对表回调
        AutoTableGlobalConfig.setCompareTableFinishCallbacks(compareTableFinishCallbacks.orderedStream().collect(Collectors.toList()));
        // 配置自定义的单个表执行前回调
        AutoTableGlobalConfig.setRunBeforeCallbacks(runBeforeCallbacks.orderedStream().collect(Collectors.toList()));
        // 配置自定义的单个表执行后回调
        AutoTableGlobalConfig.setRunAfterCallbacks(runAfterCallbacks.orderedStream().collect(Collectors.toList()));
        // 配置自定义的验证表回调
        AutoTableGlobalConfig.setValidateFinishCallbacks(validateFinishCallback.orderedStream().collect(Collectors.toList()));
        // 配置自定义的全局执行前回调
        AutoTableGlobalConfig.setAutoTableReadyCallbacks(autoTableReadyCallback.orderedStream().collect(Collectors.toList()));
        // 配置自定义的全局执行后回调
        AutoTableGlobalConfig.setAutoTableFinishCallbacks(autoTableFinishCallbacks.orderedStream().collect(Collectors.toList()));

        // 配置自定义的java到数据库的转换器
        javaTypeToDatabaseTypeConverter.ifAvailable(AutoTableGlobalConfig::setJavaTypeToDatabaseTypeConverter);
    }
}
