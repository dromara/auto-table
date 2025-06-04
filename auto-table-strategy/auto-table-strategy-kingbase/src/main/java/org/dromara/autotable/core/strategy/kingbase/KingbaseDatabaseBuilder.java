package org.dromara.autotable.core.strategy.kingbase;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.strategy.DatabaseBuilder;
import org.dromara.autotable.core.utils.StringUtils;

import java.sql.*;
import java.util.Set;

@Slf4j
public class KingbaseDatabaseBuilder implements DatabaseBuilder {

    @Override
    public boolean support(String jdbcUrl, Set<Class<?>> classes) {
        return jdbcUrl != null && jdbcUrl.startsWith("jdbc:kingbase8:");
    }

    @Override
    public boolean buildIfAbsent(String jdbcUrl, String targetUser, String targetPassword) {
        // 决定使用哪个账号连接
        PropertyConfig.KingbaseConfig kingbaseConfig = AutoTableGlobalConfig.instance().getAutoTableProperties().getKingbase();
        String execUser = StringUtils.hasText(kingbaseConfig.getAdminUser())
                ? kingbaseConfig.getAdminUser()
                : targetUser;

        String execPwd = StringUtils.hasText(kingbaseConfig.getAdminPassword())
                ? kingbaseConfig.getAdminPassword()
                : targetPassword;

        try (Connection conn = DriverManager.getConnection(jdbcUrl, execUser, execPwd)) {

            if (!hasCreateUserPrivilege(conn)) {
                log.warn("用户 [{}] 无权限创建用户，跳过 Kingbase 建库", execUser);
                return false;
            }

            if (!userExists(conn, targetUser)) {
                return createUser(conn, targetUser, targetPassword);
            } else {
                log.info("Kingbase 用户已存在：{}", targetUser);
            }

        } catch (SQLException e) {
            log.error("Kingbase 建库失败", e);
        }
        return false;
    }

    private boolean hasCreateUserPrivilege(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM information_schema.role_table_grants WHERE grantee = CURRENT_USER"
        )) {
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            log.warn("判断 CREATE USER 权限失败，跳过权限校验", e);
            return true; // 某些场景权限无法查时，放行继续尝试
        }
    }

    private boolean userExists(Connection conn, String username) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM pg_roles WHERE rolname = ?")) {
            ps.setString(1, username.toLowerCase());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            log.warn("判断 Kingbase 用户是否存在失败", e);
            return false;
        }
    }

    private boolean createUser(Connection conn, String username, String password) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE USER \"" + username + "\" WITH PASSWORD '" + password + "'");
            stmt.executeUpdate("GRANT CONNECT ON DATABASE CURRENT_DATABASE() TO \"" + username + "\"");
            stmt.executeUpdate("GRANT ALL PRIVILEGES ON SCHEMA public TO \"" + username + "\"");
            log.info("Kingbase 用户创建成功：{}", username);
            return true;
        } catch (Exception e) {
            log.error("创建 Kingbase 用户失败", e);
        }
        return false;
    }
}
