package org.dromara.autotable.strategy.sqlserver;

import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.strategy.sqlserver.builder.CreateTableSqlBuilder;
import org.dromara.autotable.strategy.sqlserver.builder.ModifyTableSqlBuilder;
import org.dromara.autotable.strategy.sqlserver.data.SqlServerCompareTableInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQLServer 集成测试：连接真实实例验证生成的 SQL 可执行。
 *
 * <p>默认禁用，避免无库环境构建失败。通过 {@code -DrunIT=true} 启用：
 * {@code mvn test -pl auto-table-strategy/auto-table-strategy-sqlserver -DrunIT=true -Dtest=SqlServerIntegrationTest}</p>
 */
@EnabledIfSystemProperty(named = "runIT", matches = "true")
public class SqlServerIntegrationTest {

    private static final String URL = System.getProperty("it.url",
            "jdbc:sqlserver://43.136.136.34:21433;databaseName=master;encrypt=false;trustServerCertificate=true");
    private static final String USER = System.getProperty("it.user", "SA");
    private static final String PASSWORD = System.getProperty("it.password", "MSSQLP0ss_iyJXeH");
    private static final String SCHEMA = "dbo";

    private static DataSource dataSource;
    private String tableName;

    @BeforeAll
    static void beforeAll() {
        dataSource = new SimpleDataSource();
    }

    @AfterAll
    static void afterAll() throws SQLException {
        // 清理可能残留的测试表（按前缀）
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sys.tables WHERE name LIKE 'at_it_%'")) {
            while (rs.next()) {
                try (Statement s = conn.createStatement()) {
                    s.executeUpdate("DROP TABLE [" + SCHEMA + "].[" + rs.getString(1) + "]");
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        // 每个测试用唯一表名
        tableName = "at_it_" + System.nanoTime();
        IStrategy.setCurrentStrategy(new SqlServerStrategy());
        DataSourceManager.setDataSource(dataSource);
        // 确保表不存在
        dropTableIfExists();
    }

    @AfterEach
    void tearDown() throws SQLException {
        dropTableIfExists();
        DataSourceManager.cleanDataSource();
        IStrategy.clean();
    }

    @Test
    void test_建表与注释可执行() throws SQLException {
        DefaultTableMetadata metadata = buildUserTable("用户表");

        List<String> sqls = CreateTableSqlBuilder.buildSql(metadata);
        executeAll(sqls);

        assertTrue(tableExists(), "表应创建成功");
        assertEquals("姓名", queryExtendedProperty("name"), "列注释应写入");
        assertEquals("用户表", queryExtendedProperty(null), "表注释应写入");
        assertTrue(indexExists("uk_name"), "唯一索引应创建");
        // 验证 IDENTITY 自增
        assertEquals("bigint", queryColumnType("id"), "自增列类型应为 bigint");
    }

    @Test
    void test_改表加列与列注释add可执行() throws SQLException {
        // 先建基础表
        executeAll(CreateTableSqlBuilder.buildSql(buildBaseTable()));

        SqlServerCompareTableInfo compareInfo = new SqlServerCompareTableInfo(tableName, SCHEMA);
        ColumnMetadata newCol = new ColumnMetadata();
        newCol.setName("email");
        newCol.setType(new DatabaseTypeAndLength("NVARCHAR", 100, null, Collections.emptyList()));
        newCol.setComment("邮箱");
        compareInfo.addNewColumn(newCol);
        compareInfo.addColumnComment("email", "邮箱");

        executeAll(ModifyTableSqlBuilder.buildSql(compareInfo));

        assertTrue(columnExists("email"), "新列应添加");
        assertEquals("邮箱", queryExtendedProperty("email"), "新列注释应写入");
    }

    @Test
    void test_改表修改列类型可执行() throws SQLException {
        executeAll(CreateTableSqlBuilder.buildSql(buildBaseTable()));

        SqlServerCompareTableInfo compareInfo = new SqlServerCompareTableInfo(tableName, SCHEMA);
        ColumnMetadata name = new ColumnMetadata();
        name.setName("name");
        name.setType(new DatabaseTypeAndLength("NVARCHAR", 500, null, Collections.emptyList()));
        name.setNotNull(false);
        compareInfo.addModifyColumn(name, true, false, false, null);

        executeAll(ModifyTableSqlBuilder.buildSql(compareInfo));

        assertEquals("nvarchar(500)", queryColumnType("name"), "列类型应变更为 nvarchar(500)");
    }

    @Test
    void test_改表重命名列_sp_rename可执行() throws SQLException {
        executeAll(CreateTableSqlBuilder.buildSql(buildBaseTable()));

        // 逻辑删除：将 name 重命名为 del_name
        SqlServerCompareTableInfo compareInfo = new SqlServerCompareTableInfo(tableName, SCHEMA);
        compareInfo.addRenameColumns(Collections.singleton("name"), "del_");
        executeAll(ModifyTableSqlBuilder.buildSql(compareInfo));

        // 验证旧列已重命名（不存在 name），新列 del_name 存在
        assertFalse(columnExists("name"), "旧列应被重命名");
        assertTrue(columnExists("del_name"), "新列名应存在");
    }

    @Test
    void test_改表修改表注释update可执行() throws SQLException {
        executeAll(CreateTableSqlBuilder.buildSql(buildUserTable("旧注释")));

        // 库已有注释，应走 sp_updateextendedproperty
        SqlServerCompareTableInfo compareInfo = new SqlServerCompareTableInfo(tableName, SCHEMA);
        compareInfo.setComment("新注释");
        executeAll(ModifyTableSqlBuilder.buildSql(compareInfo));

        assertEquals("新注释", queryExtendedProperty(null), "表注释应更新");
    }

    @Test
    void test_dropTable语法可执行() throws SQLException {
        executeAll(CreateTableSqlBuilder.buildSql(buildBaseTable()));
        assertTrue(tableExists());

        String sql = new SqlServerStrategy().dropTable(SCHEMA, tableName);
        // 直接通过数据源执行
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
        assertFalse(tableExists(), "表应被删除");
    }

    // ==================== 辅助方法 ====================

    private DefaultTableMetadata buildUserTable(String tableComment) {
        DefaultTableMetadata metadata = new DefaultTableMetadata(null, tableName, SCHEMA, tableComment);
        metadata.setColumnMetadataList(Arrays.asList(buildIdColumn(), buildNameColumn(), buildAmountColumn()));
        metadata.setIndexMetadataList(Collections.singletonList(buildUniqueIndex()));
        return metadata;
    }

    private DefaultTableMetadata buildBaseTable() {
        DefaultTableMetadata metadata = new DefaultTableMetadata(null, tableName, SCHEMA, null);
        metadata.setColumnMetadataList(Arrays.asList(buildIdColumn(), buildNameColumn()));
        metadata.setIndexMetadataList(Collections.emptyList());
        return metadata;
    }

    private ColumnMetadata buildIdColumn() {
        ColumnMetadata id = new ColumnMetadata();
        id.setName("id");
        id.setType(new DatabaseTypeAndLength("BIGINT", null, null, Collections.emptyList()));
        id.setPrimary(true);
        id.setAutoIncrement(true);
        id.setNotNull(true);
        return id;
    }

    private ColumnMetadata buildNameColumn() {
        ColumnMetadata name = new ColumnMetadata();
        name.setName("name");
        name.setType(new DatabaseTypeAndLength("NVARCHAR", 255, null, Collections.emptyList()));
        name.setComment("姓名");
        name.setNotNull(false);
        return name;
    }

    private ColumnMetadata buildAmountColumn() {
        ColumnMetadata amount = new ColumnMetadata();
        amount.setName("amount");
        amount.setType(new DatabaseTypeAndLength("DECIMAL", 19, 4, Collections.emptyList()));
        amount.setNotNull(true);
        return amount;
    }

    private IndexMetadata buildUniqueIndex() {
        IndexMetadata index = new IndexMetadata();
        index.setName("uk_name");
        index.setType(IndexTypeEnum.UNIQUE);
        index.setColumns(Collections.singletonList(
                IndexMetadata.IndexColumnParam.newInstance("name", null)));
        return index;
    }

    private void executeAll(List<String> sqls) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : sqls) {
                stmt.execute(sql);
            }
        }
    }

    private void dropTableIfExists() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TABLE IF EXISTS [" + SCHEMA + "].[" + tableName + "]");
        } catch (SQLException ignored) {
        }
    }

    private boolean tableExists() throws SQLException {
        return DataSourceManager.useConnection(connection -> {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT 1 FROM sys.tables t JOIN sys.schemas s ON t.schema_id=s.schema_id "
                                 + "WHERE s.name='" + SCHEMA + "' AND t.name='" + tableName + "'")) {
                return rs.next();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private boolean columnExists(String columnName) throws SQLException {
        return DataSourceManager.useConnection(connection -> {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT 1 FROM sys.columns c JOIN sys.tables t ON c.object_id=t.object_id "
                                 + "WHERE t.name='" + tableName + "' AND c.name='" + columnName + "'")) {
                return rs.next();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private boolean indexExists(String indexName) throws SQLException {
        return DataSourceManager.useConnection(connection -> {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT 1 FROM sys.indexes i JOIN sys.tables t ON i.object_id=t.object_id "
                                 + "WHERE t.name='" + tableName + "' AND i.name='" + indexName + "'")) {
                return rs.next();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String queryExtendedProperty(String columnName) throws SQLException {
        String sql;
        if (columnName == null) {
            sql = "SELECT CAST(ep.value AS NVARCHAR(MAX)) FROM sys.extended_properties ep "
                    + "JOIN sys.tables t ON ep.major_id=t.object_id "
                    + "JOIN sys.schemas s ON t.schema_id=s.schema_id "
                    + "WHERE s.name='" + SCHEMA + "' AND t.name='" + tableName + "' AND ep.minor_id=0 AND ep.name='MS_Description'";
        } else {
            sql = "SELECT CAST(ep.value AS NVARCHAR(MAX)) FROM sys.extended_properties ep "
                    + "JOIN sys.columns c ON ep.major_id=c.object_id AND ep.minor_id=c.column_id "
                    + "JOIN sys.tables t ON c.object_id=t.object_id "
                    + "JOIN sys.schemas s ON t.schema_id=s.schema_id "
                    + "WHERE s.name='" + SCHEMA + "' AND t.name='" + tableName + "' AND c.name='" + columnName + "' AND ep.name='MS_Description'";
        }
        return DataSourceManager.useConnection(connection -> {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                return rs.next() ? rs.getString(1) : null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String queryColumnType(String columnName) throws SQLException {
        String sql = "SELECT t.name, c.max_length, c.precision, c.scale FROM sys.columns c "
                + "JOIN sys.types t ON c.user_type_id=t.user_type_id "
                + "JOIN sys.tables tb ON c.object_id=tb.object_id "
                + "WHERE tb.name='" + tableName + "' AND c.name='" + columnName + "'";
        return DataSourceManager.useConnection(connection -> {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    String type = rs.getString(1).toLowerCase();
                    int maxLen = rs.getInt(2);
                    int precision = rs.getInt(3);
                    int scale = rs.getInt(4);
                    if ("nvarchar".equals(type) || "nchar".equals(type)) {
                        return type + "(" + (maxLen / 2) + ")";
                    }
                    if ("decimal".equals(type) || "numeric".equals(type)) {
                        return type + "(" + precision + "," + scale + ")";
                    }
                    return type;
                }
                return null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 最简 DataSource：每次返回新连接。
     */
    private static class SimpleDataSource implements DataSource {
        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(URL, username, password);
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return Logger.getLogger("SimpleDataSource");
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("Not a wrapper");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }
    }
}
