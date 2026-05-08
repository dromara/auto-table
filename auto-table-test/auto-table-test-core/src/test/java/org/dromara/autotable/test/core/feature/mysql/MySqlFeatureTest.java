package org.dromara.autotable.test.core.feature.mysql;

import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.test.core.base.AbstractIntegrationTest;
import org.dromara.autotable.test.core.effect.inspector.DbStructureInspector;
import org.dromara.autotable.test.core.entity.mysql.TestTableEngine;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MySQL 数据库特性测试
 */
public class MySqlFeatureTest extends AbstractIntegrationTest {

    @Test
    public void testEngineFeature() throws Exception {
        initMySqlDataSource();

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{TestTableEngine.class});
        AutoTableBootstrap.start();

        // 验证 ENGINE = MyISAM
        DataSource dataSource = org.dromara.autotable.core.dynamicds.DataSourceManager.getDataSource();
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE test_table_engine")) {
            if (rs.next()) {
                String createSql = rs.getString(2);
                assertTrue(createSql.contains("MyISAM"), "表应该使用 MyISAM 引擎");
            }
        }
    }

    @Test
    public void testTableExists() {
        initMySqlDataSource();

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{TestTableEngine.class});
        AutoTableBootstrap.start();

        // 使用 inspector 验证表存在
        // 这里简化验证，直接查询
        DataSource dataSource = org.dromara.autotable.core.dynamicds.DataSourceManager.getDataSource();
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 FROM information_schema.tables WHERE table_name = 'test_table_engine' AND table_schema = DATABASE()")) {
            assertTrue(rs.next(), "表应该存在");
        } catch (Exception e) {
            fail("验证失败: " + e.getMessage());
        }
    }
}
