package org.dromara.autotable.springboot;

import org.dromara.autotable.core.AutoTableAnnotationFinder;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.AutoTableOrmFrameAdapter;
import org.dromara.autotable.core.callback.AutoTableFinishCallback;
import org.dromara.autotable.core.callback.AutoTableReadyCallback;
import org.dromara.autotable.core.callback.CreateTableFinishCallback;
import org.dromara.autotable.core.callback.ModifyTableFinishCallback;
import org.dromara.autotable.core.callback.RunStateCallback;
import org.dromara.autotable.core.callback.ValidateFinishCallback;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.converter.JavaTypeToDatabaseTypeConverter;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.dromara.autotable.core.dynamicds.SqlSessionFactoryManager;
import org.dromara.autotable.core.interceptor.AutoTableAnnotationInterceptor;
import org.dromara.autotable.core.interceptor.BuildTableMetadataInterceptor;
import org.dromara.autotable.core.interceptor.CreateTableInterceptor;
import org.dromara.autotable.core.interceptor.ModifyTableInterceptor;
import org.dromara.autotable.core.recordsql.RecordSqlHandler;
import org.dromara.autotable.core.strategy.CompareTableInfo;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.TableMetadata;
import org.dromara.autotable.springboot.properties.AutoTableProperties;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * @author don
 */
@AutoConfigureAfter({DataSourceAutoConfiguration.class})
@ConditionalOnMissingBean(AutoTableAutoConfig.class)
public class AutoTableAutoConfig {

    public AutoTableAutoConfig(
            SqlSessionTemplate sqlSessionTemplate,
            AutoTableProperties autoTableProperties,
            ObjectProvider<IStrategy<? extends TableMetadata, ? extends CompareTableInfo, ?>> strategies,
            ObjectProvider<AutoTableAnnotationFinder> autoTableAnnotationFinder,
            ObjectProvider<AutoTableOrmFrameAdapter> autoTableOrmFrameAdapter,
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
            ObjectProvider<RunStateCallback> runStateCallback,
            ObjectProvider<ValidateFinishCallback> validateFinishCallback,
            ObjectProvider<AutoTableReadyCallback> autoTableReadyCallback,
            ObjectProvider<AutoTableFinishCallback> autoTableFinishCallbacks,

            ObjectProvider<JavaTypeToDatabaseTypeConverter> javaTypeToDatabaseTypeConverter) {

        // 默认设置全局的SqlSessionFactory
        SqlSessionFactoryManager.setSqlSessionFactory(sqlSessionTemplate.getSqlSessionFactory());

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

        // 假如有自定的注解扫描器，就使用自定义的注解扫描器。没有，则设置内置的注解扫描器
        AutoTableGlobalConfig.setAutoTableAnnotationFinder(autoTableAnnotationFinder.getIfAvailable(CustomAnnotationFinder::new));

        // 如果有自定义的数据库策略，则加载
        strategies.stream().forEach(AutoTableGlobalConfig::addStrategy);

        // 假如有自定义的orm框架适配器，就使用自定义的orm框架适配器
        autoTableOrmFrameAdapter.ifAvailable(AutoTableGlobalConfig::setAutoTableOrmFrameAdapter);

        // 假如有自定义的动态数据源处理器，就使用自定义的动态数据源处理器
        dynamicDataSourceHandler.ifAvailable(AutoTableGlobalConfig::setDatasourceHandler);

        // 假如有自定义的SQL记录处理器，就使用自定义的SQL记录处理器
        recordSqlHandler.ifAvailable(AutoTableGlobalConfig::setCustomRecordSqlHandler);

        /* 拦截器 */
        // 假如有自定义的注解拦截器，就使用自定义的注解拦截器
        autoTableAnnotationInterceptor.ifAvailable(AutoTableGlobalConfig::setAutoTableAnnotationInterceptor);
        // 假如有自定义的创建表拦截器，就使用自定义的创建表拦截器
        buildTableMetadataInterceptor.ifAvailable(AutoTableGlobalConfig::setBuildTableMetadataInterceptor);
        // 假如有自定义的创建表拦截器，就使用自定义的创建表拦截器
        createTableInterceptor.ifAvailable(AutoTableGlobalConfig::setCreateTableInterceptor);
        // 假如有自定义的修改表拦截器，就使用自定义的修改表拦截器
        modifyTableInterceptor.ifAvailable(AutoTableGlobalConfig::setModifyTableInterceptor);

        /* 回调事件 */
        // 假如有自定义的创建表回调，就使用自定义的创建表回调
        createTableFinishCallback.ifAvailable(AutoTableGlobalConfig::setCreateTableFinishCallback);
        // 假如有自定义的修改表回调，就使用自定义的修改表回调
        modifyTableFinishCallback.ifAvailable(AutoTableGlobalConfig::setModifyTableFinishCallback);
        // 假如有自定义的单个表执行前后回调，就使用自定义的单个表执行前后回调
        runStateCallback.ifAvailable(AutoTableGlobalConfig::setRunStateCallback);
        // 假如有自定义的验证表回调，就使用自定义的验证表回调
        validateFinishCallback.ifAvailable(AutoTableGlobalConfig::setValidateFinishCallback);
        // 假如有自定义的全局执行前回调，就使用自定义的全局执行前回调
        autoTableReadyCallback.ifAvailable(AutoTableGlobalConfig::setAutoTableReadyCallback);
        // 假如有自定义的全局执行后回调，就使用自定义的全局执行后回调
        autoTableFinishCallbacks.ifAvailable(AutoTableGlobalConfig::setAutoTableFinishCallback);

        // 假如有自定义的java到数据库的转换器，就使用自定义的java到数据库的转换器
        javaTypeToDatabaseTypeConverter.ifAvailable(AutoTableGlobalConfig::setJavaTypeToDatabaseTypeConverter);
    }
}
