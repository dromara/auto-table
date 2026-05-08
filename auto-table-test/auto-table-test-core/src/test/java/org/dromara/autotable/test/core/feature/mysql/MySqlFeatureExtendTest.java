package org.dromara.autotable.test.core.feature.mysql;

import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.test.core.base.AbstractIntegrationTest;
import org.dromara.autotable.test.core.entity.mysql.*;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MySQL 扩展特性测试（Unsigned、Zerofill、Charset、FullText 等）
 */
public class MySqlFeatureExtendTest extends AbstractIntegrationTest {

    @Test
    public void testUnsignedColumn() throws Exception {
        initMySqlDataSource();

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{TestColumnUnsigned.class});
        AutoTableBootstrap.start();

        DataSource dataSource = org.dromara.autotable.core.dynamicds.DataSourceManager.getDataSource();
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE test_column_unsigned")) {
            if (rs.next()) {
                String createSql = rs.getString(2);
                assertTrue(createSql.contains("unsigned"), "age 列应该包含 unsigned");
            }
        }
    }

    @Test
    public void testZerofillColumn() throws Exception {
        initMySqlDataSource();

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{TestColumnZerofill.class});
        AutoTableBootstrap.start();

        DataSource dataSource = org.dromara.autotable.core.dynamicds.DataSourceManager.getDataSource();
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE test_column_zerofill")) {
            if (rs.next()) {
                String createSql = rs.getString(2);
                assertTrue(createSql.contains("zerofill"), "phone 列应该包含 zerofill");
            }
        }
    }

    @Test
    public void testTableCharsetAndCollate() throws Exception {
        initMySqlDataSource();

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{TestTableCharset.class});
        AutoTableBootstrap.start();

        DataSource dataSource = org.dromara.autotable.core.dynamicds.DataSourceManager.getDataSource();
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE test_table_charset")) {
            if (rs.next()) {
                String createSql = rs.getString(2);
                assertTrue(createSql.contains("utf8mb4"), "表应该包含 utf8mb4 charset");
                assertTrue(createSql.contains("utf8mb4_general_ci"), "表应该包含 utf8mb4_general_ci collate");
            }
        }
    }

    @Test
    public void testFullTextIndex() throws Exception {
        initMySqlDataSource();

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{TestFullTextIndex.class});
        AutoTableBootstrap.start();

        DataSource dataSource = org.dromara.autotable.core.dynamicds.DataSourceManager.getDataSource();
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE test_full_text_index")) {
            if (rs.next()) {
                String createSql = rs.getString(2);
                assertTrue(createSql.contains("FULLTEXT"), "表应该包含 FULLTEXT 索引");
            }
        }
    }

    @Test
    public void testTableFullTextIndex() throws Exception {
        initMySqlDataSource();

        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setModelClass(new Class[]{TestTableFullTextIndex.class});
        AutoTableBootstrap.start();

        DataSource dataSource = org.dromara.autotable.core.dynamicds.DataSourceManager.getDataSource();
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE test_table_full_text_index")) {
            if (rs.next()) {
                String createSql = rs.getString(2);
                assertTrue(createSql.contains("FULLTEXT"), "表应该包含类级别的 FULLTEXT 索引");
            }
        }
    }
}
