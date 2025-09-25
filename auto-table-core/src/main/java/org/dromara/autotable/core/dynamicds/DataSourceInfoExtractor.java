package org.dromara.autotable.core.dynamicds;

import org.dromara.autotable.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

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

        String url = tryGet(dataSource, "getJdbcUrl", "getUrl");
        String username = tryGet(dataSource, "getUsername", "getUser");
        String password = tryGet(dataSource, "getPassword");

        if (url == null) {
            log.warn("未能通过反射从 {} 获取到 JDBC URL, 若有建库需要可自行实现提取逻辑", dataSource.getClass().getName());
            return null;
        }

        return new DbInfo(url, username, password);
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
