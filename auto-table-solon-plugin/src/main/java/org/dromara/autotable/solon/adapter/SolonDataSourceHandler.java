package org.dromara.autotable.solon.adapter;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.dromara.autotable.solon.exception.DataSourceNotFoundException;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.BeanWrap;
import org.noear.solon.data.dynamicds.DynamicDataSource;
import org.noear.solon.data.dynamicds.DynamicDs;
import org.noear.solon.data.dynamicds.DynamicDsKey;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Solon数据源处理器
 *
 * @author chengliang
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SolonDataSourceHandler implements IDataSourceHandler {

    @Override
    public void useDataSource(String dataSourceName) {

        // 获取框架中的所有数据源
        Map<String, DataSource> dataSourceMap = Solon.context().getBeansMapOfType(DataSource.class);

        // 根据数据源名称获取数据源，如果存在的话，直接使用。
        DataSource staticDataSource = dataSourceMap.get(dataSourceName);
        if (staticDataSource != null) {
            DataSourceManager.setDataSource(staticDataSource);
            return;
        }

        // 获取动态数据源列表
        List<DataSource> dynamicDataSourceList = dataSourceMap.values().stream()
                .filter(ds -> ds instanceof DynamicDataSource).collect(Collectors.toList());

        Assert.notEmpty(dynamicDataSourceList,() -> new DataSourceNotFoundException("未找到数据源"));

        if (dynamicDataSourceList.size() != 1){
            log.warn("项目中存在多个动态数据源，仅使用第一个动态数据源。");
        }

        // 找到第一个动态数据源
        DynamicDataSource dynamicDataSource = (DynamicDataSource) dynamicDataSourceList.get(0);

        // 获取内部数据源
        DataSource dataSource = dynamicDataSource.getDefaultTargetDataSource();
        if (StrUtil.isNotBlank(dataSourceName)){
            dataSource = dynamicDataSource.getTargetDataSource(dataSourceName);
        }

        DataSourceManager.setDataSource(dataSource);
    }

    @Override
    public void clearDataSource(String dataSourceName) {
        DynamicDsKey.remove();
        DataSourceManager.cleanDataSource();
    }

    @Override
    public @NonNull String getDataSourceName(Class<?> clazz) {
        DynamicDs annotation = clazz.getAnnotation(DynamicDs.class);
        if (annotation != null) {
            return annotation.value();
        }
        // 动态数据源优先
        String current = DynamicDsKey.current();
        if (StrUtil.isNotBlank(current)){
            return current;
        }
        // 默认数据源
        BeanWrap beanWrap = Solon.context().getWrap(DataSource.class);
        return beanWrap.name();
    }

}
