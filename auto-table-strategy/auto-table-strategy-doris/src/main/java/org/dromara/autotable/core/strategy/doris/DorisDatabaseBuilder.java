package org.dromara.autotable.core.strategy.doris;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.strategy.DatabaseBuilder;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.core.utils.TableMetadataHandler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DorisDatabaseBuilder implements DatabaseBuilder {

    @Override
    public boolean support(String jdbcUrl, Set<Class<?>> classes) {
        return jdbcUrl.startsWith("jdbc:mysql:") && classes.stream()
                .allMatch(entityClass -> Objects.equals(DatabaseDialect.Doris, TableMetadataHandler.getTableDialect(entityClass)));
    }

    @Override
    public boolean buildIfAbsent(String jdbcUrl, String username, String password) {
        String dbName = extractDbName(jdbcUrl);
        String baseUrl = removeDbFromUrl(jdbcUrl);

        if (dbName == null || baseUrl == null) {
            log.warn("无法解析 Doris JDBC URL：{}", jdbcUrl);
            return false;
        }

        // 使用 admin 配置优先，否则 fallback 到 username/password
        // 决定使用哪个账号连接
        PropertyConfig.DorisConfig dorisConfig = AutoTableGlobalConfig.instance().getAutoTableProperties().getDoris();
        String execUser = StringUtils.hasText(dorisConfig.getAdminUser())
                ? dorisConfig.getAdminUser()
                : username;

        String execPwd = StringUtils.hasText(dorisConfig.getAdminPassword())
                ? dorisConfig.getAdminPassword()
                : password;

        try (Connection conn = DriverManager.getConnection(baseUrl, execUser, execPwd);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT SCHEMA_NAME FROM information_schema.schemata WHERE SCHEMA_NAME = '" + dbName + "'")) {

            if (!rs.next()) {
                String sql = "CREATE DATABASE `" + dbName + "`";
                stmt.executeUpdate(sql);
                log.info("成功创建 Doris 数据库：{}", dbName);
                return true;
            }

        } catch (SQLException e) {
            log.error("创建 Doris 数据库失败，连接或执行异常", e);
        }
        return false;
    }

    /**
     * 从 JDBC URL 中提取数据库名
     */
    private String extractDbName(String jdbcUrl) {
        Matcher matcher = Pattern.compile("jdbc:mysql://[^/]+/([^?]+)").matcher(jdbcUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 移除 JDBC URL 中的数据库名部分，保留 baseUrl
     */
    private String removeDbFromUrl(String jdbcUrl) {
        int index = jdbcUrl.indexOf("/", "jdbc:mysql://".length());
        if (index != -1) {
            return jdbcUrl.substring(0, index);
        }
        return null;
    }
}
