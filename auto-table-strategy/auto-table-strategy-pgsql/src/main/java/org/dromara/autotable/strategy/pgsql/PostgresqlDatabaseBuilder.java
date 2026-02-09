package org.dromara.autotable.strategy.pgsql;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.strategy.DatabaseBuilder;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.core.utils.TableMetadataHandler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class PostgresqlDatabaseBuilder implements DatabaseBuilder {

    @Override
    public boolean support(String jdbcUrl, String dialectOnEntity) {
        return jdbcUrl.contains(":postgresql://") && (StringUtils.noText(dialectOnEntity) || Objects.equals(dialectOnEntity, DatabaseDialect.PostgreSQL));
    }

    @Override
    public BuildResult build(String jdbcUrl, String username, String password, Set<Class<?>> entityClasses, Consumer<Boolean> dbStatusCallback) {
        String dbName = extractDbNameFromUrl(jdbcUrl);
        if (dbName == null) {
            return BuildResult.of(false, null);
        }

        // 使用 admin 配置优先，否则 fallback 到 username/password
        // 决定使用哪个账号连接
        PropertyConfig.PgsqlConfig pgsqlConfig = AutoTableGlobalConfig.instance().getAutoTableProperties().getPgsql();
        String execUser = StringUtils.hasText(pgsqlConfig.getAdminUser())
                ? pgsqlConfig.getAdminUser()
                : username;

        String execPwd = StringUtils.hasText(pgsqlConfig.getAdminPassword())
                ? pgsqlConfig.getAdminPassword()
                : password;

        String adminUrl = jdbcUrl.replaceFirst("/" + dbName, "/postgres");

        boolean createDatabase = false;
        try (Connection conn = DriverManager.getConnection(adminUrl, execUser, execPwd)) {

            // 创建数据库
            createDatabase = createDatabase(dbStatusCallback, conn, dbName);
        } catch (SQLException e) {
            log.error("创建PostgreSQL数据库失败", e);
        }

        return BuildResult.of(createDatabase, dbName);
    }

    private boolean createDatabase(Consumer<Boolean> dbStatusCallback, Connection conn, String dbName) throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
            ps.setString(1, dbName);
            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next();
            // 回调数据库状态
            dbStatusCallback.accept(exists);
            if (!exists) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(String.format(
                            "CREATE DATABASE \"%s\" WITH ENCODING='%s'",
                            dbName, "UTF8" // 默认 UTF8
                    ));
                    log.info("创建 PostgreSQL 数据库：{}", dbName);
                    return true;
                }
            }
        }
        return false;
    }

    private String extractDbNameFromUrl(String jdbcUrl) {
        Matcher matcher = Pattern.compile(".*:postgresql://[^/]+/([^?]+)").matcher(jdbcUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        log.warn("无法从url中解析数据库名：{}", jdbcUrl);
        return null;
    }
}
