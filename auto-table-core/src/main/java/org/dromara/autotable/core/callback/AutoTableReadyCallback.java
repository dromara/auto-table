package org.dromara.autotable.core.callback;

import java.util.Set;

/**
 * AutoTable准备好了，即将开始执行的回调
 */
public interface AutoTableReadyCallback {

    /**
     * 执行前，可以做一些自定义配置相关的初始化工作
     *
     * @param tableClasses 实体模型class
     */
    void ready(final Set<Class<?>> tableClasses);
}
