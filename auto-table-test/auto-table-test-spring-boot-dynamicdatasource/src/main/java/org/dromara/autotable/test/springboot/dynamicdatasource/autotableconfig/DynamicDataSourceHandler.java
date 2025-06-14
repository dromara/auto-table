package org.dromara.autotable.test.springboot.dynamicdatasource.autotableconfig;

import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.dromara.autotable.test.springboot.dynamicdatasource.dynamicdatasourceframe.Ds;
import org.dromara.autotable.test.springboot.dynamicdatasource.dynamicdatasourceframe.DynamicDataSourceContextHolder;
import lombok.NonNull;
import org.springframework.stereotype.Component;

/**
 * @author don
 */
@Component
public class DynamicDataSourceHandler implements IDataSourceHandler {

    @Override
    public void useDataSource(String dataSourceName) {
        // 切换数据源，因为spring的数据源是动态的，所以直接调用spring的内部切换逻辑即可
        DynamicDataSourceContextHolder.setContextKey(dataSourceName);
    }

    @Override
    public void clearDataSource(String dataSourceName) {
        // 清除数据源
        DynamicDataSourceContextHolder.removeContextKey();
    }

    @Override
    public @NonNull String getDataSourceName(Class<?> clazz) {
        // 根据实体类获取对应的数据源名称，假定自定义的多数据源，有一个注解Ds
        Ds ds = clazz.getAnnotation(Ds.class);
        if (ds != null) {
            return ds.value();
        } else {
            return DynamicDataSourceContextHolder.getContextKey();
        }
    }
}
