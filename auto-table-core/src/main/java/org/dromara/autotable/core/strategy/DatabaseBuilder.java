package org.dromara.autotable.core.strategy;

import java.util.function.Consumer;

public interface DatabaseBuilder {

    /**
     * 是否支持
     *
     * @param jdbcUrl         jdbcUrl
     * @param dialectOnEntity 实体上指定的数据库方言
     * @return true/false
     */
    boolean support(String jdbcUrl, String dialectOnEntity);

    /**
     * 构建数据库
     *
     * @param jdbcUrl  jdbcUrl
     * @param username 用户名
     * @param password 密码
     */
    boolean build(String jdbcUrl, String username, String password, Consumer<Boolean> dbStatusCallback);
}
