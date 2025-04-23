package org.dromara.autotable.solon.adapter;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
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
        DynamicDsKey.setCurrent(dataSourceName);
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
        return DynamicDsKey.getCurrent();
    }

}
