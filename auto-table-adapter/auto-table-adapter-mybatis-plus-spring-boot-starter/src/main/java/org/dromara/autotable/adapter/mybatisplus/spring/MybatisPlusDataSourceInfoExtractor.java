package org.dromara.autotable.adapter.mybatisplus.spring;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.ds.ItemDataSource;
import org.dromara.autotable.core.dynamicds.DataSourceInfoExtractor;
import org.dromara.autotable.core.dynamicds.DataSourceManager;

import javax.sql.DataSource;

/**
 * 动态数据源场景下的数据源信息提取器（Spring 集成层）。
 * <p>
 * 当使用 dynamic-datasource 时，{@link DataSource} 实际是 {@link DynamicRoutingDataSource}，
 * 无法直接获取 JDBC URL。本类从动态数据源中取出当前真实数据源再提取信息。
 * <p>
 * 注意：此类放在 starter 而非 adapter，因为 {@link DynamicRoutingDataSource}
 * 所在模块（dynamic-datasource-spring）依赖 Spring。
 *
 * @author auto-table
 */
public class MybatisPlusDataSourceInfoExtractor implements DataSourceInfoExtractor {

    @Override
    public DbInfo extract(DataSource dataSource) {
        if (dataSource instanceof DynamicRoutingDataSource) {
            String datasourceName = DataSourceManager.getDatasourceName();
            ItemDataSource currentDatasource =
                    (ItemDataSource) ((DynamicRoutingDataSource) dataSource).getDataSource(datasourceName);
            DataSource realDataSource = currentDatasource.getRealDataSource();
            return DataSourceInfoExtractor.super.extract(realDataSource);
        }
        return DataSourceInfoExtractor.super.extract(dataSource);
    }
}
