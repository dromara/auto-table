/*
 *  Copyright (c) 2022-2025, Mybatis-Flex (fuhai999@gmail.com).
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.dromara.autotable.solon.integration;

import cn.hutool.core.util.ObjUtil;
import org.apache.ibatis.solon.MybatisAdapter;
import org.apache.ibatis.solon.integration.MybatisAdapterManager;
import org.dromara.autotable.core.AutoTableAnnotationFinder;
import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.AutoTableMetadataAdapter;
import org.dromara.autotable.core.callback.AutoTableFinishCallback;
import org.dromara.autotable.core.callback.AutoTableReadyCallback;
import org.dromara.autotable.core.callback.CreateTableFinishCallback;
import org.dromara.autotable.core.callback.ModifyTableFinishCallback;
import org.dromara.autotable.core.callback.RunStateCallback;
import org.dromara.autotable.core.callback.ValidateFinishCallback;
import org.dromara.autotable.core.converter.JavaTypeToDatabaseTypeConverter;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.dromara.autotable.core.dynamicds.SqlSessionFactoryManager;
import org.dromara.autotable.core.interceptor.AutoTableAnnotationInterceptor;
import org.dromara.autotable.core.interceptor.BuildTableMetadataInterceptor;
import org.dromara.autotable.core.interceptor.CreateTableInterceptor;
import org.dromara.autotable.core.interceptor.ModifyTableInterceptor;
import org.dromara.autotable.core.recordsql.RecordSqlHandler;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.AutoTableClassScanner;
import org.dromara.autotable.solon.adapter.CustomAnnotationFinder;
import org.dromara.autotable.solon.adapter.SolonDataSourceHandler;
import org.dromara.autotable.solon.annotation.EnableAutoTable;
import org.dromara.autotable.solon.properties.AutoTableProperties;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.BeanWrap;
import org.noear.solon.core.Plugin;

import javax.sql.DataSource;

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
        AutoTableProperties autoTableProperties = context.beanMake(AutoTableProperties.class).get();

        // 设置全局的配置
        AutoTableGlobalConfig.setAutoTableProperties(autoTableProperties.toConfig());

        // 资源加载完成后启动AutoTable
        context.lifecycle(-100, ()-> resourceLoadFinish(context));

        // 配置 自定义的IStrategy
        context.subBeansOfType(IStrategy.class, AutoTableGlobalConfig::addStrategy);
        // 配置 class扫描器
        context.subBeansOfType(AutoTableClassScanner.class, AutoTableGlobalConfig::setAutoTableClassScanner);
        // 配置 ORM框架适配器
        context.subBeansOfType(AutoTableMetadataAdapter.class, AutoTableGlobalConfig::setAutoTableMetadataAdapter);
        // 配置 数据库类型转换
        context.subBeansOfType(JavaTypeToDatabaseTypeConverter.class, AutoTableGlobalConfig::setJavaTypeToDatabaseTypeConverter);
        // 配置 自定义记录sql的方式
        context.subBeansOfType(RecordSqlHandler.class, AutoTableGlobalConfig::setCustomRecordSqlHandler);
        // IDataSourceHandler
        context.subBeansOfType(IDataSourceHandler.class, AutoTableGlobalConfig::setDatasourceHandler);

        /* 拦截器 */
        // AutoTableAnnotationInterceptor
        context.subBeansOfType(AutoTableAnnotationInterceptor.class, AutoTableGlobalConfig::setAutoTableAnnotationInterceptor);
        // BuildTableMetadataInterceptor
        context.subBeansOfType(BuildTableMetadataInterceptor.class, AutoTableGlobalConfig::setBuildTableMetadataInterceptor);
        // CreateTableInterceptor
        context.subBeansOfType(CreateTableInterceptor.class, AutoTableGlobalConfig::setCreateTableInterceptor);
        // ModifyTableInterceptor
        context.subBeansOfType(ModifyTableInterceptor.class, AutoTableGlobalConfig::setModifyTableInterceptor);

        /* 回调事件 */
        // CreateTableFinishCallback
        context.subBeansOfType(CreateTableFinishCallback.class, AutoTableGlobalConfig::setCreateTableFinishCallback);
        // ModifyTableFinishCallback
        context.subBeansOfType(ModifyTableFinishCallback.class, AutoTableGlobalConfig::setModifyTableFinishCallback);
        // RunStateCallback
        context.subBeansOfType(RunStateCallback.class, AutoTableGlobalConfig::setRunStateCallback);
        // 加载完成回调
        context.subBeansOfType(AutoTableReadyCallback.class, AutoTableGlobalConfig::setAutoTableReadyCallback);
        // 验证完成回调
        context.subBeansOfType(ValidateFinishCallback.class, AutoTableGlobalConfig::setValidateFinishCallback);
        // 完成回调
        context.subBeansOfType(AutoTableFinishCallback.class, AutoTableGlobalConfig::setAutoTableFinishCallback);

    }

    /**
     * 资源加载完成
     * @param context SolonContext
     */
    private void resourceLoadFinish(AppContext context) {

        // 注入自定义的注解扫描器
        AutoTableAnnotationFinder annotationFinder = context.getBean(AutoTableAnnotationFinder.class);
        AutoTableGlobalConfig.setAutoTableAnnotationFinder(ObjUtil.defaultIfNull(annotationFinder, new CustomAnnotationFinder()));

        // 资源全部加载完成后
        BeanWrap wrap = context.getWrap(DataSource.class);
        MybatisAdapter mybatisAdapter = MybatisAdapterManager.get(wrap);
        SqlSessionFactoryManager.setSqlSessionFactory(mybatisAdapter.getFactory());

        // 默认的数据源处理器
        if (context.getBean(IDataSourceHandler.class) == null){
            context.beanMake(SolonDataSourceHandler.class);
        }

        //最后启动
        AutoTableBootstrap.start();
    }

}
