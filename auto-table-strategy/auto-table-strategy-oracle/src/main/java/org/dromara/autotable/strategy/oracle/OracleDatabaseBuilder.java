package org.dromara.autotable.strategy.oracle;

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
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
public class OracleDatabaseBuilder implements DatabaseBuilder {

    @Override
    public boolean support(String jdbcUrl, String dialectOnEntity) {
        return jdbcUrl != null && jdbcUrl.contains(":oracle://") && (StringUtils.noText(dialectOnEntity) || Objects.equals(dialectOnEntity, DatabaseDialect.Oracle));
    }

    @Override
    public BuildResult build(String jdbcUrl, String targetUsername, String targetPassword, Set<Class<?>> entityClasses, Consumer<Boolean> dbStatusCallback) {
        // 决定使用哪个账号连接
        PropertyConfig.OracleConfig oracleConfig = AutoTableGlobalConfig.instance().getAutoTableProperties().getOracle();
        String execUsername = StringUtils.hasText(oracleConfig.getAdminUser())
                ? oracleConfig.getAdminUser()
                : targetUsername;

        String execPassword = StringUtils.hasText(oracleConfig.getAdminPassword())
                ? oracleConfig.getAdminPassword()
                : targetPassword;

        try (Connection conn = DriverManager.getConnection(jdbcUrl, execUsername, execPassword)) {

            if (!hasCreateUserPrivilege(conn)) {
                log.warn("用户 [{}] 无 CREATE USER 权限，跳过 Oracle 建库操作", execUsername);
                return BuildResult.of(false, targetUsername);
            }

            boolean userExists = userExists(conn, targetUsername);
            // 用户状态回调
            dbStatusCallback.accept(userExists);
            if (!userExists) {
                return createUser(conn, targetUsername, targetPassword);
            }

        } catch (SQLException e) {
            log.error("Oracle 建库失败，连接或执行异常", e);
        }
        return BuildResult.of(false, targetUsername);
    }

    private boolean hasCreateUserPrivilege(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM USER_SYS_PRIVS WHERE PRIVILEGE = 'CREATE USER'"
        )) {
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            log.warn("检查 CREATE USER 权限失败", e);
            return false;
        }
    }

    private boolean userExists(Connection conn, String username) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT USERNAME FROM DBA_USERS WHERE USERNAME = ?"
        )) {
            ps.setString(1, username.toUpperCase());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            log.warn("无 DBA_USERS 权限，尝试使用 ALL_USERS 检查用户是否存在");

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT USERNAME FROM ALL_USERS WHERE USERNAME = ?"
            )) {
                ps.setString(1, username.toUpperCase());
                ResultSet rs = ps.executeQuery();
                return rs.next();
            } catch (SQLException ex) {
                log.error("ALL_USERS 查询也失败，无法判断用户是否存在", ex);
                return false;
            }
        }
    }

    private BuildResult createUser(Connection conn, String username, String password) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE USER " + username + " IDENTIFIED BY " + password);
            stmt.executeUpdate("GRANT CONNECT, RESOURCE TO " + username);
            log.info("Oracle 用户创建成功：{}", username);
            return BuildResult.of(true, username);
        } catch (Exception e) {
            return BuildResult.of(false, username);
        }
    }
}
