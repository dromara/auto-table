package org.dromara.autotable.core.strategy.pgsql;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PostgresqlDatabaseBuilder implements DatabaseBuilder {

    @Override
    public boolean support(String jdbcUrl, String dialectOnEntity) {
        return jdbcUrl.startsWith("jdbc:postgresql") && (dialectOnEntity.isEmpty() || dialectOnEntity.equals(DatabaseDialect.PostgreSQL));
    }

    @Override
    public boolean buildIfAbsent(String jdbcUrl, String username, String password) {
        String dbName = extractDbNameFromUrl(jdbcUrl);
        if (dbName == null) {
            return false;
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

        String adminUrl = jdbcUrl.replaceFirst("/" + dbName, "/postgres");

        try {
            return createPgDatabaseIfAbsent(adminUrl, execUser, execPwd, dbName);
        } catch (SQLException e) {
            log.error("创建PostgreSQL数据库失败", e);
        }
        return false;
    }

    private String extractDbNameFromUrl(String jdbcUrl) {
        Matcher matcher = Pattern.compile("jdbc:postgresql://[^/]+/([^?]+)").matcher(jdbcUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        log.warn("无法从url中解析数据库名：{}", jdbcUrl);
        return null;
    }

    private boolean createPgDatabaseIfAbsent(String adminUrl, String username, String password, String dbName) throws SQLException {
        try (
                Connection conn = DriverManager.getConnection(adminUrl, username, password);
                PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")
        ) {
            ps.setString(1, dbName);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("CREATE DATABASE \"" + dbName + "\" WITH ENCODING='UTF8'");
                    log.info("创建 PostgreSQL 数据库：{}", dbName);
                    return true;
                }
            }
        }
        return false;
    }
}
