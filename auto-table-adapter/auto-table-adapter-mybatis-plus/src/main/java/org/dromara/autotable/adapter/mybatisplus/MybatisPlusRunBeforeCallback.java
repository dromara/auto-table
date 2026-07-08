package org.dromara.autotable.adapter.mybatisplus;

import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import org.dromara.autotable.core.callback.RunBeforeCallback;

/**
 * 单表执行前回调（零 Spring 依赖）。
 * <p>
 * 设置 MyBatis-Plus 拦截器忽略策略，
 * 跳过租户插件（tenantLine）、非法 SQL 插件（illegalSql）、防全表更新插件（blockAttack），
 * 避免 AutoTable 执行 DDL 时被这些插件干扰。
 *
 * @author auto-table
 */
public class MybatisPlusRunBeforeCallback implements RunBeforeCallback {

    @Override
    public void before(Class<?> tableClass) {
        InterceptorIgnoreHelper.handle(IgnoreStrategy.builder()
                .tenantLine(true)
                .illegalSql(true)
                .blockAttack(true)
                .build());
    }
}
