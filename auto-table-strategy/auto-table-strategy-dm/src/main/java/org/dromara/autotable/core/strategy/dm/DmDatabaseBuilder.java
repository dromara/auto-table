package org.dromara.autotable.core.strategy.dm;

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

@Slf4j
public class DmDatabaseBuilder implements DatabaseBuilder {

    @Override
    public boolean support(String jdbcUrl, String dialectOnEntity) {
        return jdbcUrl != null && jdbcUrl.startsWith("jdbc:dm:") && (StringUtils.noText(dialectOnEntity) || Objects.equals(dialectOnEntity, DatabaseDialect.DM));
    }

    @Override
    public boolean build(String jdbcUrl, String targetUser, String targetPwd, Consumer<Boolean> dbStatusCallback) {
        // 决定使用哪个账号连接
        PropertyConfig.DMConfig dmConfig = AutoTableGlobalConfig.instance().getAutoTableProperties().getDm();
        String execUser = StringUtils.hasText(dmConfig.getAdminUser())
                ? dmConfig.getAdminUser()
                : targetUser;

        String execPwd = StringUtils.hasText(dmConfig.getAdminPassword())
                ? dmConfig.getAdminPassword()
                : targetPwd;

        log.info("达梦建库使用账号：{}", execUser);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, execUser, execPwd)) {
            if (!hasCreateUserPrivilege(conn)) {
                log.warn("用户 [{}] 无权限创建用户，跳过达梦建库", execUser);
                return false;
            }

            boolean userExists = userExists(conn, targetUser);
            // 回调数据库状态
            dbStatusCallback.accept(userExists);
            if (!userExists) {
                return createUser(conn, targetUser, targetPwd);
            } else {
                log.info("达梦用户已存在：{}", targetUser);
            }

        } catch (SQLException e) {
            log.error("达梦建库失败", e);
        }
        return false;
    }

    private boolean hasCreateUserPrivilege(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM USER_SYS_PRIVS WHERE PRIVILEGE = 'CREATE USER'")) {
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            log.warn("无法判断是否具备 CREATE USER 权限", e);
            return false; // 保守处理
        }
    }

    private boolean userExists(Connection conn, String username) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM SYSUSERS WHERE NAME = ?")) {
            ps.setString(1, username.toUpperCase());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            log.error("查询用户是否存在失败", e);
            return false;
        }
    }

    private boolean createUser(Connection conn, String username, String password) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE USER " + username + " IDENTIFIED BY \"" + password + "\"");
            stmt.executeUpdate("GRANT RESOURCE, PUBLIC TO " + username);
            log.info("达梦用户创建成功：{}", username);
            return true;
        } catch (Exception e) {
            log.error("创建达梦用户失败", e);
        }
        return false;
    }
}
