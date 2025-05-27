package org.dromara.autotable.annotation.doris;

import org.dromara.autotable.annotation.doris.emuns.AggregateFun;

import java.lang.annotation.*;

/**
 * doris的列配置
 *
 * @author lizhian
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DorisColumn {

    /**
     * 聚合函数
     */
    AggregateFun aggregateFun() default AggregateFun.none;

    /**
     * 自增起始值
     */
    long autoIncrementStartValue() default -1;

    /**
     * 是否更新为当前时间戳
     */
    boolean onUpdateCurrentTimestamp() default false;
}
