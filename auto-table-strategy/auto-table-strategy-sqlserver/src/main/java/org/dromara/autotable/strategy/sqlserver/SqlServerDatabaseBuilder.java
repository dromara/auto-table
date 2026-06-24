package org.dromara.autotable.strategy.sqlserver;

import lombok.extern.slf4j.Slf4j;
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
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQLServer 自动建库 Builder。
 *
 * <p>SQLServer 建库前需连接到 {@code master} 库。本实现将原 URL 中的 databaseName 替换为 master，
 * 使用传入的账号（需具备建库权限，如 SA）连接并执行 {@code CREATE DATABASE [xxx]}。</p>
 *
 * <p>注意：首版不引入独立的 SqlServerConfig.adminUser/adminPassword 配置（避免侵入 core PropertyConfig），
 * 直接复用数据源账号。若账号无 master 建库权限，建库失败但表结构维护（已有库）不受影响。</p>
 *
 * @author don
 */
@Slf4j
public class SqlServerDatabaseBuilder implements DatabaseBuilder {

    private static final Pattern DATABASE_NAME_PATTERN =
            Pattern.compile("databaseName=([^;]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATABASE_PATTERN =
            Pattern.compile("database=([^;]+)", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean support(String jdbcUrl, String dialectOnEntity) {
        return jdbcUrl.contains(":sqlserver:")
                && (StringUtils.noText(dialectOnEntity) || Objects.equals(dialectOnEntity, DatabaseDialect.SQLServer));
    }

    @Override
    public BuildResult build(String jdbcUrl, String username, String password, Set<Class<?>> entityClasses, Consumer<Boolean> dbStatusCallback) {

        String dbName = extractDbNameFromUrl(jdbcUrl);
        if (dbName == null) {
            return BuildResult.of(false, dbName);
        }

        // 连接 master 库建库
        String masterUrl = toMasterUrl(jdbcUrl);

        boolean created = false;
        try (Connection conn = DriverManager.getConnection(masterUrl, username, password)) {
            created = createDatabase(dbStatusCallback, conn, dbName);
        } catch (SQLException e) {
            log.error("创建 SQLServer 数据库失败", e);
        }

        return BuildResult.of(created, dbName);
    }

    private boolean createDatabase(Consumer<Boolean> dbStatusCallback, Connection conn, String dbName) throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM sys.databases WHERE name = ?")) {
            ps.setString(1, dbName);
            try (ResultSet rs = ps.executeQuery()) {
                boolean exists = rs.next();
                // 回调数据库状态
                dbStatusCallback.accept(exists);
                if (!exists) {
                    try (Statement stmt = conn.createStatement()) {
                        // 用方括号包裹库名，避免保留字冲突
                        stmt.executeUpdate("CREATE DATABASE [" + dbName.replace("]", "]]") + "]");
                        log.info("创建 SQLServer 数据库：{}", dbName);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 从 SQLServer JDBC URL 中提取数据库名。
     * <p>支持 {@code databaseName=xxx} 与 {@code database=xxx} 两种形式。</p>
     */
    private String extractDbNameFromUrl(String jdbcUrl) {
        Matcher matcher = DATABASE_NAME_PATTERN.matcher(jdbcUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        matcher = DATABASE_PATTERN.matcher(jdbcUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        log.warn("无法从url中解析数据库名：{}", jdbcUrl);
        return null;
    }

    /**
     * 将 URL 中的数据库名替换为 master，用于连接 master 库建库。
     * <p>若原 URL 不含 databaseName/database 参数，则追加 ;databaseName=master。</p>
     */
    private String toMasterUrl(String jdbcUrl) {
        if (DATABASE_NAME_PATTERN.matcher(jdbcUrl).find()) {
            return jdbcUrl.replaceAll("(?i)databaseName=[^;]+", "databaseName=master");
        }
        if (DATABASE_PATTERN.matcher(jdbcUrl).find()) {
            return jdbcUrl.replaceAll("(?i)database=[^;]+", "database=master");
        }
        return jdbcUrl + ";databaseName=master";
    }
}
