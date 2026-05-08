package org.dromara.autotable.test.core.effect;

import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.test.core.base.AbstractIntegrationTest;
import org.dromara.autotable.test.core.effect.inspector.DbStructureInspector;
import org.dromara.autotable.test.core.effect.inspector.MySqlStructureInspector;
import org.dromara.autotable.test.core.effect.inspector.PgSqlStructureInspector;
import org.junit.jupiter.api.AfterEach;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 效果测试基类
 * <p>
 * 提供数据库 inspector 初始化和自动清理能力，
 * 子类可通过 {@link #getInspector()} 获取数据库结构查询器，
 * 验证真实数据库表结构是否与预期一致。
 */
public abstract class AbstractEffectTest extends AbstractIntegrationTest {

    /**
     * 记录测试中创建的表，测试结束后自动清理
     */
    private final List<String> createdTables = new ArrayList<>();

    /**
     * 获取数据库结构查询器
     *
     * @return DbStructureInspector 实例
     */
    protected DbStructureInspector getInspector() {
        // 获取当前数据源对应的数据库类型和连接信息
        DataSource dataSource = DataSourceManager.getDataSource();
        try {
            Connection connection = dataSource.getConnection();
            String url = connection.getMetaData().getURL();
            String databaseName = connection.getCatalog();
            if (databaseName == null) {
                databaseName = connection.getSchema();
            }

            if (url.contains("mysql")) {
                return new MySqlStructureInspector(connection, databaseName);
            } else if (url.contains("postgresql") || url.contains("pgsql")) {
                String schema = connection.getSchema();
                if (schema == null) {
                    schema = "public";
                }
                return new PgSqlStructureInspector(connection, schema);
            }
            throw new UnsupportedOperationException("不支持的数据库类型: " + url);
        } catch (SQLException e) {
            throw new RuntimeException("获取 inspector 失败", e);
        }
    }

    /**
     * 执行建表并记录表名用于自动清理
     *
     * @param entityClass 实体类
     * @param tableName   表名（用于后续清理）
     */
    protected void executeCreateTable(Class<?> entityClass, String tableName) {
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{entityClass});
        AutoTableBootstrap.start();
        createdTables.add(tableName);
    }

    /**
     * 执行改表
     *
     * @param entityClass 实体类
     * @param tableName   表名
     */
    protected void executeUpdateTable(Class<?> entityClass, String tableName) {
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.update);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{entityClass});
        AutoTableBootstrap.start();
    }

    /**
     * 直接执行 SQL（如 DROP TABLE）
     *
     * @param sql SQL 语句
     */
    protected void executeSql(String sql) {
        DataSourceManager.useConnection(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            } catch (SQLException e) {
                throw new RuntimeException("执行 SQL 失败: " + sql, e);
            }
            return null;
        });
    }

    /**
     * 注册需要自动清理的表
     *
     * @param tableName 表名
     */
    protected void registerTableForCleanup(String tableName) {
        createdTables.add(tableName);
    }

    @AfterEach
    public void cleanupTables() {
        for (String tableName : createdTables) {
            try {
                executeSql("DROP TABLE IF EXISTS " + tableName);
            } catch (Exception e) {
                // 忽略清理异常
            }
        }
        createdTables.clear();
    }
}
