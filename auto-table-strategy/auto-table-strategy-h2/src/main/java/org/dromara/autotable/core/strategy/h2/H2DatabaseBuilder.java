package org.dromara.autotable.core.strategy.h2;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.strategy.DatabaseBuilder;
import org.dromara.autotable.core.utils.StringUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class H2DatabaseBuilder implements DatabaseBuilder {

    @Override
    public boolean support(String jdbcUrl, Set<Class<?>> classes) {
        return jdbcUrl.startsWith("jdbc:h2:");
    }

    @Override
    public boolean buildIfAbsent(String jdbcUrl, String username, String password) {
        String filePath = extractFilePathFromUrl(jdbcUrl);
        if (filePath == null) {
            log.warn("无法从URL中提取H2数据库文件路径：{}", jdbcUrl);
            return false;
        }

        // 跳过内存模式
        if (filePath.startsWith("mem:") || filePath.equalsIgnoreCase("mem")) {
            // log.info("H2 内存数据库，无需建库：{}", jdbcUrl);
            return false;
        }

        File dbFile = new File(filePath + ".mv.db");
        if (dbFile.exists()) {
            // log.info("H2 数据库已存在：{}", dbFile.getAbsolutePath());
            return false;
        }

        // 使用 admin 配置优先，否则 fallback 到 username/password
        // 决定使用哪个账号连接
        PropertyConfig.H2Config h2Config = AutoTableGlobalConfig.instance().getAutoTableProperties().getH2();
        String execUser = StringUtils.hasText(h2Config.getAdminUser())
                ? h2Config.getAdminUser()
                : username;

        String execPwd = StringUtils.hasText(h2Config.getAdminPassword())
                ? h2Config.getAdminPassword()
                : password;

        try {
            return createH2Database(jdbcUrl, execUser, execPwd);
        } catch (SQLException e) {
            log.error("创建 H2 数据库失败", e);
        }
        return false;
    }

    private String extractFilePathFromUrl(String jdbcUrl) {
        // 例：jdbc:h2:file:/data/db/test  => 提取出 file:/data/db/test
        Matcher matcher = Pattern.compile("jdbc:h2:(file:)?([^;]+)").matcher(jdbcUrl);
        if (matcher.find()) {
            String path = matcher.group(2);
            // 去掉前缀file: 如果有
            return path.startsWith("file:") ? path.substring(5) : path;
        }
        return null;
    }

    private boolean createH2Database(String jdbcUrl, String username, String password) throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            // 自动创建数据库文件
            log.info("创建 H2 数据库：{}", jdbcUrl);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
