package org.dromara.autotable.solon.adapter;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.BeanWrap;

import javax.sql.DataSource;

/**
 * Solon数据源处理器
 * @author chengliang
 * @date 2025/01/08
 */
@Component
@RequiredArgsConstructor
public class SolonDataSourceHandler implements IDataSourceHandler {

    @Override
    public void useDataSource(String dataSourceName) {

    }

    @Override
    public void clearDataSource(String dataSourceName) {

    }

    @Override
    public @NonNull String getDataSourceName(Class<?> clazz) {
        BeanWrap wrap = Solon.context().getWrap(DataSource.class);
        if (wrap == null){
            return "";
        }
        return wrap.name();
    }

}
