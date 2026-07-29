package org.dromara.autotable.adapter.mybatisflex.spring;

import com.mybatisflex.core.datasource.FlexDataSource;
import org.dromara.autotable.core.dynamicds.DataSourceInfoExtractor;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * MyBatis-Flex 动态数据源信息提取器。
 * <p>
 * 从 {@link FlexDataSource} 中提取真实数据源的连接信息。
 *
 * @author auto-table
 */
public class MybatisFlexDataSourceInfoExtractor implements DataSourceInfoExtractor {

    private static final Logger log = LoggerFactory.getLogger(MybatisFlexDataSourceInfoExtractor.class);

    @Override
    public DbInfo extract(DataSource dataSource) {
        if (dataSource instanceof FlexDataSource) {
            String datasourceName = DataSourceManager.getDatasourceName();
            DataSource realDataSource = ((FlexDataSource) dataSource).getDataSourceMap().get(datasourceName);
            if (realDataSource == null) {
                log.warn("动态数据源 [{}] 不存在，回退到默认数据源", datasourceName);
                realDataSource = ((FlexDataSource) dataSource).getDefaultDataSource();
            }
            if (realDataSource != null) {
                return DataSourceInfoExtractor.super.extract(realDataSource);
            }
        }
        return DataSourceInfoExtractor.super.extract(dataSource);
    }
}
