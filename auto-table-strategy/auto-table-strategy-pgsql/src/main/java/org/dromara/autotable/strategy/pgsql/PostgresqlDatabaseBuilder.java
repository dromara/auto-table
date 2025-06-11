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
        return jdbcUrl.startsWith("jdbc:postgresql") && (StringUtils.noText(dialectOnEntity) || Objects.equals(dialectOnEntity, DatabaseDialect.PostgreSQL));
    }

    @Override
    public BuildResult build(String jdbcUrl, String username, String password, Set<Class<?>> entityClasses, Consumer<Boolean> dbStatusCallback) {
        String dbName = extractDbNameFromUrl(jdbcUrl);
        if (dbName == null) {
            return BuildResult.of(false, dbName);
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

        // 获取数据库所有schema
        Set<String> schemas = entityClasses.stream()
                .map(TableMetadataHandler::getTableName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        DataSourceManager.useConnection(connection -> {
            try {
                // 通过连接获取DatabaseMetaData对象
                String schema = connection.getSchema();
                if (StringUtils.hasText(schema)) {
                    schemas.add(schema);
                }
            } catch (Exception e) {
                log.error("获取数据库信息失败", e);
            }
        });

        try {
            return createPgDatabaseIfAbsent(adminUrl, execUser, execPwd, dbName, schemas, dbStatusCallback);
        } catch (SQLException e) {
            log.error("创建PostgreSQL数据库失败", e);
        }
        return BuildResult.of(false, dbName);
    }

    private String extractDbNameFromUrl(String jdbcUrl) {
        Matcher matcher = Pattern.compile("jdbc:postgresql://[^/]+/([^?]+)").matcher(jdbcUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        log.warn("无法从url中解析数据库名：{}", jdbcUrl);
        return null;
    }

    private BuildResult createPgDatabaseIfAbsent(String adminUrl, String username, String password, String dbName, Set<String> schemas, Consumer<Boolean> dbStatusCallback) throws SQLException {
        try (
                Connection conn = DriverManager.getConnection(adminUrl, username, password);
        ) {

            // 创建数据库
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
                        return BuildResult.of(true, dbName);
                    }
                }
            }

            // 创建schema
            for (String schemaName : schemas) {
                try (
                        PreparedStatement ps = conn.prepareStatement(
                                "SELECT schema_name FROM information_schema.schemata WHERE schema_name = ?")
                ) {
                    ps.setString(1, schemaName);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        log.info("PostgreSQL schema [{}] 已存在，无需创建。", schemaName);
                    } else {
                        try (Statement stmt = conn.createStatement()) {
                            stmt.executeUpdate("CREATE SCHEMA \"" + schemaName + "\"");
                            log.info("已成功创建 PostgreSQL schema [{}]", schemaName);
                        }
                    }
                } catch (SQLException e) {
                    log.error("检查或创建 schema [{}] 时出错", schemaName, e);
                }
            }
        }
        return BuildResult.of(false, dbName);
    }
}
