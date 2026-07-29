package org.dromara.autotable.adapter.mybatisplus.spring;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.ds.ItemDataSource;
import org.dromara.autotable.core.dynamicds.DataSourceInfoExtractor;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(MybatisPlusDataSourceInfoExtractor.class);

    @Override
    public DbInfo extract(DataSource dataSource) {
        if (dataSource instanceof DynamicRoutingDataSource) {
            String datasourceName = DataSourceManager.getDatasourceName();
            DataSource ds = ((DynamicRoutingDataSource) dataSource).getDataSource(datasourceName);
            if (ds == null) {
                log.warn("动态数据源 [{}] 不存在，回退到默认数据源", datasourceName);
                return DataSourceInfoExtractor.super.extract(dataSource);
            }
            // ItemDataSource 包装了真实的连接池数据源，需要取出内部 realDataSource
            if (ds instanceof ItemDataSource) {
                DataSource realDataSource = ((ItemDataSource) ds).getRealDataSource();
                return DataSourceInfoExtractor.super.extract(realDataSource);
            }
            // 非 ItemDataSource 类型（如 SecuredDataSource），直接尝试提取
            return DataSourceInfoExtractor.super.extract(ds);
        }
        return DataSourceInfoExtractor.super.extract(dataSource);
    }
}
