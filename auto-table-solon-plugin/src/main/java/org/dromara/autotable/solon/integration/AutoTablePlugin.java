package org.dromara.autotable.solon.integration;

import cn.hutool.core.util.ObjUtil;
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
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.dromara.autotable.core.interceptor.AutoTableAnnotationInterceptor;
import org.dromara.autotable.core.interceptor.BuildTableMetadataInterceptor;
import org.dromara.autotable.core.interceptor.CreateTableInterceptor;
import org.dromara.autotable.core.interceptor.ModifyTableInterceptor;
import org.dromara.autotable.core.recordsql.RecordSqlHandler;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.solon.adapter.CustomAnnotationFinder;
import org.dromara.autotable.solon.adapter.SolonDataSourceHandler;
import org.dromara.autotable.solon.annotation.EnableAutoTable;
import org.dromara.autotable.solon.properties.AutoTablePropertiesRegister;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.Plugin;

import javax.sql.DataSource;
import java.util.List;
import java.util.function.Consumer;

/**
 * 配置 AutoTable 插件。
 *
 * @author chengliang
 * @date 2025/01/08
 */
public class AutoTablePlugin implements Plugin {

    @Override
    public void start(AppContext context) throws Throwable {

        // 根据EnableAutoTable注解决定是否启动插件
        if (context.app().source().getAnnotation(EnableAutoTable.class) == null) {
            return;
        }

        // 配置 自动装配属性
        context.beanMake(AutoTablePropertiesRegister.class);
        PropertyConfig autoTableProperties = context.getBean(PropertyConfig.class);
        // 设置全局的配置
        AutoTableGlobalConfig.instance().setAutoTableProperties(autoTableProperties);

        // 资源加载完成后启动AutoTable
        context.lifecycle(-100, () -> resourceLoadFinish(context));
    }

    /**
     * 资源加载完成
     *
     * @param context SolonContext
     */
    private void resourceLoadFinish(AppContext context) {

        // 注入自定义的注解扫描器
        AutoTableAnnotationFinder annotationFinder = context.getBean(AutoTableAnnotationFinder.class);
        AutoTableGlobalConfig.instance().setAutoTableAnnotationFinder(ObjUtil.defaultIfNull(annotationFinder, new CustomAnnotationFinder()));

        // 资源全部加载完成后
        DataSource dataSource = context.getWrap(DataSource.class).get();
        DataSourceManager.setDataSource(dataSource);

        // 默认的数据源处理器
        if (context.getBean(IDataSourceHandler.class) == null) {
            context.beanMake(SolonDataSourceHandler.class);
        }

        // 配置 自定义的IStrategy
        this.getAndSetBean(context, IStrategy.class, AutoTableGlobalConfig.instance()::addStrategy);
        // 配置 class扫描器
        this.getAndSetBean(context, AutoTableClassScanner.class, AutoTableGlobalConfig.instance()::setAutoTableClassScanner);
        // 配置 ORM框架适配器
        this.getAndSetBean(context, AutoTableMetadataAdapter.class, AutoTableGlobalConfig.instance()::setAutoTableMetadataAdapter);
        // 配置 数据库类型转换
        this.getAndSetBean(context, JavaTypeToDatabaseTypeConverter.class, AutoTableGlobalConfig.instance()::setJavaTypeToDatabaseTypeConverter);
        // 配置 自定义记录sql的方式
        this.getAndSetBean(context, RecordSqlHandler.class, AutoTableGlobalConfig.instance()::setCustomRecordSqlHandler);
        // IDataSourceHandler
        this.getAndSetBean(context, IDataSourceHandler.class, AutoTableGlobalConfig.instance()::setDatasourceHandler);

        /* 拦截器 */
        // AutoTableAnnotationInterceptor
        this.getAndSetBeans(context, AutoTableAnnotationInterceptor.class, AutoTableGlobalConfig.instance()::setAutoTableAnnotationInterceptors);
        // BuildTableMetadataInterceptor
        this.getAndSetBeans(context, BuildTableMetadataInterceptor.class, AutoTableGlobalConfig.instance()::setBuildTableMetadataInterceptors);
        // CreateTableInterceptor
        this.getAndSetBeans(context, CreateTableInterceptor.class, AutoTableGlobalConfig.instance()::setCreateTableInterceptors);
        // ModifyTableInterceptor
        this.getAndSetBeans(context, ModifyTableInterceptor.class, AutoTableGlobalConfig.instance()::setModifyTableInterceptors);

        /* 回调事件 */
        // CreateDatabaseFinishCallback
        this.getAndSetBeans(context, CreateDatabaseFinishCallback.class, AutoTableGlobalConfig.instance()::setCreateDatabaseFinishCallbacks);
        // CreateTableFinishCallback
        this.getAndSetBeans(context, CreateTableFinishCallback.class, AutoTableGlobalConfig.instance()::setCreateTableFinishCallbacks);
        // ModifyTableFinishCallback
        this.getAndSetBeans(context, ModifyTableFinishCallback.class, AutoTableGlobalConfig.instance()::setModifyTableFinishCallbacks);
        // CompareTableFinishCallback
        this.getAndSetBeans(context, CompareTableFinishCallback.class, AutoTableGlobalConfig.instance()::setCompareTableFinishCallbacks);
        // DeleteTableFinishCallback
        this.getAndSetBeans(context, DeleteTableFinishCallback.class, AutoTableGlobalConfig.instance()::setDeleteTableFinishCallbacks);
        // RunBeforeCallback
        this.getAndSetBeans(context, RunBeforeCallback.class, AutoTableGlobalConfig.instance()::setRunBeforeCallbacks);
        // RunAfterCallback
        this.getAndSetBeans(context, RunAfterCallback.class, AutoTableGlobalConfig.instance()::setRunAfterCallbacks);
        // 加载完成回调
        this.getAndSetBeans(context, AutoTableReadyCallback.class, AutoTableGlobalConfig.instance()::setAutoTableReadyCallbacks);
        // 验证完成回调
        this.getAndSetBeans(context, ValidateFinishCallback.class, AutoTableGlobalConfig.instance()::setValidateFinishCallbacks);
        // 完成回调
        this.getAndSetBeans(context, AutoTableFinishCallback.class, AutoTableGlobalConfig.instance()::setAutoTableFinishCallbacks);

        //最后启动
        AutoTableBootstrap.start();
    }

    private <C> void getAndSetBean(AppContext context, Class<C> clazz, Consumer<C> consumer) {
        C bean = context.getBean(clazz);
        if (bean != null) {
            consumer.accept(bean);
        }
    }

    private <C> void getAndSetBeans(AppContext context, Class<C> clazz, Consumer<List<C>> consumer) {
        List<C> beans = context.getBeansOfType(clazz);
        if (!beans.isEmpty()) {
            consumer.accept(beans);
        }
    }

}
