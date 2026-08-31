package org.dromara.autotable.strategy.yashandb;

import org.dromara.autotable.annotation.enums.IndexSortTypeEnum;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.strategy.yashandb.data.YashanCompareTableInfo;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional integration tests against a real YashanDB JDBC endpoint.
 */
class YashanStrategyIntegrationTest {
    /**
     * Exercises create, metadata discovery, case-insensitive DML, and update flows.
     */
    @Test
    void createsReadsAndUpdatesIsolatedTable() throws Exception {
        // Integration credentials are supplied by the test environment, never by source code.
        String url = System.getenv("YASHAN_TEST_URL");
        String user = System.getenv("YASHAN_TEST_USER");
        String password = System.getenv("YASHAN_TEST_PASSWORD");
        Assumptions.assumeTrue(hasText(url) && hasText(user) && password != null,
                "YashanDB integration environment is not configured");

        Class.forName("com.yashandb.jdbc.Driver");
        String schema = valueOrDefault(System.getenv("YASHAN_TEST_SCHEMA"), user).toUpperCase(Locale.ROOT);
        String tableName = "at_yas_it_" + Long.toUnsignedString(System.nanoTime(), 36);
        DriverDataSource dataSource = new DriverDataSource(url, user, password);
        boolean created = false;
        try (Connection connection = dataSource.getConnection()) {
            assertFalse(tableExists(connection, schema, tableName), "Integration test table already exists");
        }

        YashanStrategy strategy = new YashanStrategy();
        IStrategy.setCurrentStrategy(strategy);
        DataSourceManager.setDataSource(dataSource);
        try {
            // Use a unique table so the integration test cannot modify application tables.
            DefaultTableMetadata table = metadata(schema, tableName, 64, "Yashan AutoTable integration");
            strategy.executeSql(table, strategy.createTable(table));
            created = true;

            assertFalse(strategy.checkTableNotExist(schema, tableName));
            assertTrue(strategy.listAllTables(schema).stream().anyMatch(tableName::equalsIgnoreCase));
            assertCaseInsensitiveDml(dataSource, schema, tableName);

            YashanCompareTableInfo unchanged = strategy.compareTable(table);
            assertFalse(unchanged.needModify(), unchanged.validateFailedMessage());

            DefaultTableMetadata updated = metadata(schema, tableName, 128, "Yashan AutoTable updated");
            ColumnMetadata enabled = column("enabled", "TINYINT", null, null).setDefaultValue("0");
            updated.getColumnMetadataList().add(enabled);
            YashanCompareTableInfo changes = strategy.compareTable(updated);
            assertTrue(changes.needModify());
            strategy.executeSql(updated, strategy.modifyTable(changes));
            assertFalse(strategy.compareTable(updated).needModify(),
                    strategy.compareTable(updated).validateFailedMessage());
        } finally {
            // Remove only the unique test table and restore AutoTable's global data-source state.
            if (created) {
                try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                    statement.execute("DROP TABLE " + schema + "." + tableName + " CASCADE CONSTRAINTS");
                }
            }
            DataSourceManager.cleanDataSource();
            IStrategy.clean();
        }
    }

    /**
     * Builds a fixture containing native and MySQL-compatible YashanDB types.
     */
    private static DefaultTableMetadata metadata(String schema, String tableName, int codeLength, String comment) {
        DefaultTableMetadata table = new DefaultTableMetadata(null, tableName, schema, comment);
        ColumnMetadata id = column("id", "BIGINT", null, null).setPrimary(true).setAutoIncrement(true);
        ColumnMetadata code = column("case_code", "VARCHAR", codeLength, null).setNotNull(true)
                .setComment("case-insensitive code");
        ColumnMetadata amount = column("amount", "DECIMAL", 18, 2);
        ColumnMetadata createdAt = column("created_at", "TIMESTAMP", null, null);
        ColumnMetadata mysqlDateTime = column("mysql_datetime", "DATETIME", null, null);
        ColumnMetadata mysqlText = column("mysql_text", "TEXT", null, null);
        ColumnMetadata mysqlJson = column("mysql_json", "JSON", null, null);
        ColumnMetadata mysqlLongBlob = column("mysql_longblob", "LONGBLOB", null, null);
        ColumnMetadata mysqlInt = column("mysql_int", "INT", null, null);
        table.setColumnMetadataList(new java.util.ArrayList<>(Arrays.asList(
                id, code, amount, createdAt, mysqlDateTime, mysqlText, mysqlJson, mysqlLongBlob, mysqlInt)));
        IndexMetadata index = new IndexMetadata().setName("auto_idx_" + tableName + "_code")
                .setType(IndexTypeEnum.UNIQUE)
                .setColumns(Collections.singletonList(
                        IndexMetadata.IndexColumnParam.newInstance("case_code", IndexSortTypeEnum.ASC)));
        table.setIndexMetadataList(Collections.singletonList(index));
        return table;
    }

    /**
     * Confirms DML works with lower- and upper-case unquoted object references.
     */
    private static void assertCaseInsensitiveDml(DataSource dataSource, String schema, String table) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO " + schema.toLowerCase(Locale.ROOT) + "." + table.toLowerCase(Locale.ROOT)
                    + " (case_code, amount) VALUES ('lower', 12.34)");
            try (ResultSet resultSet = statement.executeQuery("SELECT ID, CASE_CODE FROM " + schema + "."
                    + table.toUpperCase(Locale.ROOT) + " WHERE CASE_CODE = 'lower'")) {
                assertTrue(resultSet.next());
                assertEquals(1L, resultSet.getLong(1));
                assertEquals("lower", resultSet.getString(2));
            }
        }
    }

    /**
     * Checks table existence through the same YashanDB catalog view used by the strategy.
     */
    private static boolean tableExists(Connection connection, String schema, String table) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM ALL_TABLES WHERE UPPER(OWNER)=UPPER(?) AND UPPER(TABLE_NAME)=UPPER(?)")) {
            statement.setString(1, schema);
            statement.setString(2, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    /**
     * Creates a compact column metadata object for integration fixtures.
     */
    private static ColumnMetadata column(String name, String type, Integer length, Integer scale) {
        return new ColumnMetadata().setName(name)
                .setType(new DatabaseTypeAndLength(type, length, scale, Collections.emptyList()));
    }

    /**
     * Tests whether an environment value contains a usable non-blank string.
     */
    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Uses a configured value or a supplied fallback when the value is blank.
     */
    private static String valueOrDefault(String value, String defaultValue) {
        return hasText(value) ? value : defaultValue;
    }

    /**
     * Minimal JDBC data source wrapper used to inject the configured YashanDB driver.
     */
    private static class DriverDataSource implements DataSource {
        private final String url;
        private final String user;
        private final String password;

        private DriverDataSource(String url, String user, String password) {
            this.url = url;
            this.user = user;
            this.password = password;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, user, password);
        }

        @Override
        public Connection getConnection(String username, String pwd) throws SQLException {
            return DriverManager.getConnection(url, username, pwd);
        }

        @Override
        public PrintWriter getLogWriter() {
            return DriverManager.getLogWriter();
        }

        @Override
        public void setLogWriter(PrintWriter out) {
            DriverManager.setLogWriter(out);
        }

        @Override
        public void setLoginTimeout(int seconds) {
            DriverManager.setLoginTimeout(seconds);
        }

        @Override
        public int getLoginTimeout() {
            return DriverManager.getLoginTimeout();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) {
                return iface.cast(this);
            }
            throw new SQLException("Not a wrapper for " + iface.getName());
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return iface.isInstance(this);
        }
    }
}
