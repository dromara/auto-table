package org.dromara.autotable.adapter.mybatisplus.spring;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.springframework.util.StringUtils;

/**
 * 动态数据源处理器（Spring 集成层）。
 * <p>
 * 处理数据源切换逻辑，读取 MP 原生 {@code @DS} 注解。
 * <p>
 * 本类只处理 MP 原生注解。自定义注解（{@code @Table.dsName()}）的扩展
 * 由 mybatis-plus-ext 项目通过继承本类实现。
 * <p>
 * 注意：此类放在 starter 而非 adapter，因为 {@code DynamicDataSourceContextHolder}
 * 和 {@code @DS} 所在模块（dynamic-datasource-spring）依赖 Spring。
 *
 * @author auto-table
 */
public class MybatisPlusDynamicDataSourceHandler implements IDataSourceHandler {

    private final String primaryDataSourceName;

    public MybatisPlusDynamicDataSourceHandler(String primaryDataSourceName) {
        this.primaryDataSourceName = primaryDataSourceName;
    }

    @Override
    public void useDataSource(String dsName) {
        DynamicDataSourceContextHolder.push(dsName);
    }

    @Override
    public void clearDataSource(String serializable) {
        DynamicDataSourceContextHolder.poll();
    }

    @Override
    public String getDataSourceName(Class clazz) {
        // 读取 MP 原生 @DS
        DS ds = (DS) clazz.getAnnotation(DS.class);
        if (ds != null && StringUtils.hasText(ds.value())) {
            return ds.value();
        }
        // 回退到 primary 数据源
        return primaryDataSourceName;
    }
}
