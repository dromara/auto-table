package org.dromara.autotable.core.dynamicds;

import lombok.NonNull;

/**
 * @author don
 */
public interface IDataSourceHandler {
    /**
     * 多数据源场景：切换指定的数据源
     *
     * @param dataSourceName 数据源名称
     */
    void useDataSource(String dataSourceName);

    /**
     * 多数据源场景：清除当前数据源
     *
     * @param dataSourceName 数据源名称
     */
    void clearDataSource(String dataSourceName);

    /**
     * 多数据源场景：获取指定类的数据库数据源
     *
     * @param clazz 指定类
     * @return 数据源名称，表分组的依据，届时，根据该值分组所有的表，同一数据源下的统一处理
     */
    @NonNull
    String getDataSourceName(Class<?> clazz);
}
