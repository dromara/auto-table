package org.dromara.autotable.core.strategy;

import java.util.Set;

public interface DatabaseBuilder {

    /**
     * 是否支持
     *
     * @param jdbcUrl jdbcUrl
     * @param classes 所有涉及到的实体类
     * @return true/false
     */
    boolean support(String jdbcUrl, Set<Class<?>> classes);

    /**
     * 构建数据库
     *
     * @param jdbcUrl  jdbcUrl
     * @param username 用户名
     * @param password 密码
     */
    boolean buildIfAbsent(String jdbcUrl, String username, String password);
}
