package org.dromara.autotable.core.strategy.mysql;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.strategy.DatabaseBuilder;
import org.dromara.autotable.core.utils.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class MysqlDatabaseBuilder implements DatabaseBuilder {

    @Override
    public boolean support(String jdbcUrl, String dialectOnEntity) {
        return jdbcUrl.startsWith("jdbc:mysql") && (StringUtils.noText(dialectOnEntity) || Objects.equals(dialectOnEntity, DatabaseDialect.MySQL));
    }

    @Override
    public BuildResult build(String jdbcUrl, String username, String password, Consumer<Boolean> dbStatusCallback) {
        String dbName = extractDbNameFromUrl(jdbcUrl);
        if (dbName == null) {
            return BuildResult.of(false, dbName);
        }

        // 使用 admin 配置优先，否则 fallback 到 username/password
        // 决定使用哪个账号连接
        PropertyConfig.MysqlConfig mysqlConfig = AutoTableGlobalConfig.instance().getAutoTableProperties().getMysql();
        String execUser = StringUtils.hasText(mysqlConfig.getAdminUser())
                ? mysqlConfig.getAdminUser()
                : username;

        String execPwd = StringUtils.hasText(mysqlConfig.getAdminPassword())
                ? mysqlConfig.getAdminPassword()
                : password;

        String adminUrl = jdbcUrl.replaceFirst("/" + dbName, "/information_schema");

        try {
            return createMysqlDatabaseIfAbsent(adminUrl, execUser, execPwd, dbName, dbStatusCallback);
        } catch (SQLException e) {
            log.error("创建数据库失败", e);
        }
        return BuildResult.of(false, dbName);
    }

    private String extractDbNameFromUrl(String jdbcUrl) {
        Matcher matcher = Pattern.compile("jdbc:mysql://[^/]+/([^?]+)").matcher(jdbcUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        log.warn("无法从url中解析数据库名：{}", jdbcUrl);
        return null;
    }

    private BuildResult createMysqlDatabaseIfAbsent(String adminUrl, String username, String password, String dbName, Consumer<Boolean> dbStatusCallback) throws SQLException {
        try (
                Connection conn = DriverManager.getConnection(adminUrl, username, password);
                PreparedStatement ps = conn.prepareStatement("SELECT SCHEMA_NAME FROM SCHEMATA WHERE SCHEMA_NAME = ?")
        ) {
            ps.setString(1, dbName);
            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next();
            // 数据库状态回调
            dbStatusCallback.accept(exists);
            if (!exists) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("CREATE DATABASE `" + dbName + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                    log.info("创建数据库成功：{}", dbName);
                    return BuildResult.of(true, dbName);
                }
            } else {
                log.info("数据库已存在：{}", dbName);
            }
        }
        return BuildResult.of(false, dbName);
    }
}
