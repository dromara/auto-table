package org.dromara.autotable.adapter.mybatisflex.spring;

import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.datasource.DataSourceKey;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import lombok.NonNull;
import org.springframework.util.StringUtils;

/**
 * MyBatis-Flex 动态数据源处理器。
 * <p>
 * 使用 mybatis-flex 原生 {@link DataSourceKey} 和 {@code @Table(dataSource)} 处理多数据源切换。
 *
 * @author auto-table
 */
public class MybatisFlexDynamicDataSourceHandler implements IDataSourceHandler {

    @Override
    public void useDataSource(String dsName) {
        DataSourceKey.use(dsName);
    }

    @Override
    public void clearDataSource(String dataSourceName) {
        DataSourceKey.clear();
    }

    @NonNull
    @Override
    public String getDataSourceName(Class<?> clazz) {
        Table table = (Table) clazz.getAnnotation(Table.class);
        if (table != null && StringUtils.hasText(table.dataSource())) {
            return table.dataSource();
        }
        return "";
    }
}
