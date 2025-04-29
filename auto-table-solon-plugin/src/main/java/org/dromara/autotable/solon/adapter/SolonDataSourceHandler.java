package org.dromara.autotable.solon.adapter;

import cn.hutool.core.util.StrUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.BeanWrap;
import org.noear.solon.data.dynamicds.DynamicDs;
import org.noear.solon.data.dynamicds.DynamicDsKey;

import javax.sql.DataSource;

/**
 * Solon数据源处理器
 *
 * @author chengliang
 * @date 2025/01/08
 */
@Component
@RequiredArgsConstructor
public class SolonDataSourceHandler implements IDataSourceHandler {

    @Override
    public void useDataSource(String dataSourceName) {
        DynamicDsKey.use(dataSourceName);
        DataSource dataSource = Solon.context().getWrap(DataSource.class).get();
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
