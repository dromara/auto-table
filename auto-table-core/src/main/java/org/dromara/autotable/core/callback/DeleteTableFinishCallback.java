package org.dromara.autotable.core.callback;

/**
 * 删除表回调
 *
 * @author don
 */
@FunctionalInterface
public interface DeleteTableFinishCallback {

    /**
     * 删除表后回调
     *
     * @param schema    schema
     * @param tableName 表名
     */
    void afterDeleteTables(String schema, final String tableName);
}
