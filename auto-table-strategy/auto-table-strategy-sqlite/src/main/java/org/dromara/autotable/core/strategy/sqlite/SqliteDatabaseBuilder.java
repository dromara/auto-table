package org.dromara.autotable.core.strategy.sqlite;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.strategy.DatabaseBuilder;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SqliteDatabaseBuilder implements DatabaseBuilder {

    @Override
    public boolean support(String jdbcUrl, Set<Class<?>> classes) {
        return jdbcUrl.startsWith("jdbc:sqlite:");
    }

    @Override
    public boolean buildIfAbsent(String jdbcUrl, String username, String password) {
        String filePath = extractFilePathFromUrl(jdbcUrl);
        if (filePath == null) {
            log.warn("无法从URL中提取SQLite文件路径：{}", jdbcUrl);
            return false;
        }

        if ("memory".equalsIgnoreCase(filePath) || filePath.startsWith(":memory:")) {
            log.info("内存 SQLite 数据库，无需建库：{}", jdbcUrl);
            return false;
        }

        File dbFile = new File(filePath);
        if (dbFile.exists()) {
            log.info("SQLite 数据库已存在：{}", filePath);
            return false;
        }

        try {
            return createSqliteDatabase(filePath);
        } catch (SQLException e) {
            log.error("创建 SQLite 数据库失败", e);
        }
        return false;
    }

    private String extractFilePathFromUrl(String jdbcUrl) {
        Matcher matcher = Pattern.compile("jdbc:sqlite:(.+)").matcher(jdbcUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private boolean createSqliteDatabase(String filePath) throws SQLException {
        // 连接即创建
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + filePath)) {
            // 无需执行额外SQL，文件会被创建
            log.info("创建 SQLite 数据库文件：{}", filePath);
            return true;
        } catch (Exception e) {
            log.error("创建 SQLite 数据库失败", e);
            return false;
        }
    }
}
