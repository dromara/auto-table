package org.dromara.autotable.core.dynamicds;

import org.dromara.autotable.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * 从数据源中提取 JDBC 连接信息。
 *
 * <p>数据源可能被 p6spy、spring 等组件层层包装（如 datasource-decorator 的
 * DecoratedDataSource、spring 的 DelegatingDataSource），默认实现会沿包装链
 * 逐层解包，直到找到能提供 JDBC URL 的真实数据源。若默认实现无法提取，
 * 可实现本接口并注册为自定义提取器。</p>
 */
public interface DataSourceInfoExtractor {

    Logger log = LoggerFactory.getLogger(DataSourceInfoExtractor.class);

    class DbInfo {
        public final String jdbcUrl;
        public final String username;
        public final String password;

        public DbInfo(String jdbcUrl, String username, String password) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }

        @Override
        public String toString() {
            return "JDBC地址：" + jdbcUrl + ", 用户名：" + username + ", 密码：" + (password == null ? "null" : "******");
        }
    }

    default DbInfo extract(DataSource dataSource) {
        if (dataSource == null) {
            log.warn("当前数据源为空，无法提取数据链接信息");
            return null;
        }

        // 逐层解包包装数据源，直到某一层能提供 JDBC URL；visited 防御自引用或循环包装导致的死循环
        Set<DataSource> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        DataSource current = dataSource;
        while (current != null && visited.add(current)) {
            String url = tryGet(current, "getJdbcUrl", "getUrl", "getURL");
            if (StringUtils.hasText(url)) {
                String username = tryGet(current, "getUsername", "getUser");
                String password = tryGet(current, "getPassword");
                return new DbInfo(url, username, password);
            }
            current = unwrapDataSource(current);
        }

        log.warn("未能通过反射从 {} 获取到 JDBC URL, 若有建库需要可自行实现提取逻辑", dataSource.getClass().getName());
        return null;
    }

    /**
     * 尝试解一层包装，返回被包装的真实数据源，无法解包时返回 null。
     *
     * <p>方法名与常见包装器对应：
     * getRealDataSource 对应 p6spy 的 P6DataSource、datasource-decorator 的 DecoratedDataSource；
     * getTargetDataSource 对应 spring 的 DelegatingDataSource 及其子类；
     * getWrappedDataSource、getInnermostDelegate* 为其他常见包装器；
     * getDataSource 为部分代理数据源（如 net.ttddyy ds-proxy 旧版）。
     */
    default DataSource unwrapDataSource(DataSource dataSource) {
        String[] unwrapMethodNames = {"getRealDataSource", "getTargetDataSource", "getWrappedDataSource", "getInnermostDelegate", "getDataSource"};
        for (String name : unwrapMethodNames) {
            try {
                Method method = dataSource.getClass().getMethod(name);
                if (!DataSource.class.isAssignableFrom(method.getReturnType())) {
                    continue;
                }
                Object value = method.invoke(dataSource);
                if (value instanceof DataSource && value != dataSource) {
                    return (DataSource) value;
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                log.debug("调用方法 {} 解包数据源失败：{}", name, e.getMessage());
            }
        }
        return null;
    }

    default String tryGet(DataSource obj, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method method = obj.getClass().getMethod(name);
                method.setAccessible(true);
                Object value = method.invoke(obj);
                if (value instanceof String && StringUtils.hasText((String) value)) {
                    return (String) value;
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                System.out.println("调用方法 " + name + " 失败：" + e.getMessage());
            }
        }

        // 再尝试找字段（万一是public的或者通过getXxx取不到）
        for (String name : methodNames) {
            String fieldName = name.replaceFirst("^get", "");
            if (!fieldName.isEmpty()) {
                fieldName = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
                try {
                    Field field = obj.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value instanceof String && StringUtils.hasText((String) value)) {
                        return (String) value;
                    }
                } catch (NoSuchFieldException ignored) {
                } catch (Exception e) {
                    System.out.println("读取字段 " + fieldName + " 失败：" + e.getMessage());
                }
            }
        }

        return null;
    }
}
