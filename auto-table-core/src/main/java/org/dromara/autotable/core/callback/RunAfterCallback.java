package org.dromara.autotable.core.callback;

/**
 * 单个表执行后回调
 */
public interface RunAfterCallback {

    /**
     * 执行后
     *
     * @param tableClass 实体模型class
     */
    void after(final Class<?> tableClass);
}
