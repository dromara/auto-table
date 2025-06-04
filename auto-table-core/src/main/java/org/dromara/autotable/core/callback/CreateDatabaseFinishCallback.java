package org.dromara.autotable.core.callback;

import org.dromara.autotable.core.utils.DataSourceInfoExtractor;

import java.util.Set;

/**
 * 建库回调
 *
 * @author don
 */
@FunctionalInterface
public interface CreateDatabaseFinishCallback {

    /**
     * 建库后回调
     *
     * @param dataSource 数据源
     * @param classes    该数据源下所有相关的实体
     * @param dbInfo     数据库信息
     */
    void afterCreateDatabase(String dataSource, Set<Class<?>> classes, DataSourceInfoExtractor.DbInfo dbInfo);
}
