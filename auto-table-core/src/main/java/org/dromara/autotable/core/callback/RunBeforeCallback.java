package org.dromara.autotable.core.callback;

/**
 * 单个表执行前回调
 */
public interface RunBeforeCallback {

    /**
     * 执行前
     *
     * @param tableClass 实体模型class
     */
    void before(final Class<?> tableClass);
}
