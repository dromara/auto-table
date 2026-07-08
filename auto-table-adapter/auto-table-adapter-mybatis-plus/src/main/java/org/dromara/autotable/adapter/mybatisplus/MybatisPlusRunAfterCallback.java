package org.dromara.autotable.adapter.mybatisplus;

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import org.dromara.autotable.core.callback.RunAfterCallback;

/**
 * 单表执行后回调（零 Spring 依赖）。
 * <p>
 * 清理 {@link MybatisPlusRunBeforeCallback} 中设置的拦截器忽略策略，恢复正常拦截。
 *
 * @author auto-table
 */
public class MybatisPlusRunAfterCallback implements RunAfterCallback {

    @Override
    public void after(Class<?> tableClass) {
        InterceptorIgnoreHelper.clearIgnoreStrategy();
    }
}
