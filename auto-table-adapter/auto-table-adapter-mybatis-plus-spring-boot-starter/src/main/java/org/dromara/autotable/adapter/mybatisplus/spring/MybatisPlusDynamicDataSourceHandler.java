package org.dromara.autotable.adapter.mybatisplus.spring;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.dromara.autotable.adapter.mybatisplus.spring.annotation.Table;
import org.dromara.autotable.adapter.mybatisplus.spring.util.AnnotatedElementUtilsPlus;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.springframework.util.StringUtils;

/**
 * 动态数据源处理器（Spring 集成层）。
 * <p>
 * 处理数据源切换逻辑，读取 MP 原生 {@code @DS} 注解和自定义 {@code @Table.dsName()}。
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
        // 1. 优先读 MP 原生 @DS
        DS ds = (DS) AnnotatedElementUtilsPlus.findDeepMergedAnnotation(clazz, DS.class);
        if (ds != null && StringUtils.hasText(ds.value())) {
            return ds.value();
        }
        // 2. 读自定义 @Table.dsName()
        Table tableAnno = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(clazz, Table.class);
        if (tableAnno != null && StringUtils.hasText(tableAnno.dsName())) {
            return tableAnno.dsName();
        }
        // 3. 回退到 primary 数据源
        return primaryDataSourceName;
    }
}
