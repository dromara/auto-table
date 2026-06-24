package org.dromara.autotable.strategy.sqlserver;

import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.strategy.sqlserver.SqlServerDatabaseBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SqlServerDatabaseBuilder 单元测试。
 * 验证 support 判断、URL 数据库名解析、master 库 URL 替换。
 * 不测试 build（涉及真实建库，属于集成测试范畴）。
 */
public class SqlServerDatabaseBuilderTest {

    private final SqlServerDatabaseBuilder builder = new SqlServerDatabaseBuilder();

    private String extractDbName(String url) throws Exception {
        Method method = SqlServerDatabaseBuilder.class.getDeclaredMethod("extractDbNameFromUrl", String.class);
        method.setAccessible(true);
        return (String) method.invoke(builder, url);
    }

    private String toMasterUrl(String url) throws Exception {
        Method method = SqlServerDatabaseBuilder.class.getDeclaredMethod("toMasterUrl", String.class);
        method.setAccessible(true);
        return (String) method.invoke(builder, url);
    }

    // ==================== support ====================

    @Test
    void test支持_sqlserverUrl且无方言() {
        assertTrue(builder.support("jdbc:sqlserver://localhost:1433;databaseName=test", null));
        assertTrue(builder.support("jdbc:sqlserver://localhost:1433;databaseName=test", ""));
    }

    @Test
    void test支持_sqlserverUrl且方言匹配() {
        assertTrue(builder.support("jdbc:sqlserver://localhost:1433;databaseName=test",
                DatabaseDialect.SQLServer));
    }

    @Test
    void test不支持_非sqlserverUrl() {
        assertFalse(builder.support("jdbc:postgresql://localhost/test", null));
        assertFalse(builder.support("jdbc:mysql://localhost/test", null));
    }

    @Test
    void test不支持_sqlserverUrl但方言不匹配() {
        assertFalse(builder.support("jdbc:sqlserver://localhost:1433;databaseName=test",
                DatabaseDialect.PostgreSQL));
    }

    // ==================== extractDbNameFromUrl ====================

    @Test
    void test解析库名_databaseName形式() throws Exception {
        assertEquals("mydb", extractDbName("jdbc:sqlserver://localhost:1433;databaseName=mydb;encrypt=false"));
    }

    @Test
    void test解析库名_database简写形式() throws Exception {
        assertEquals("mydb", extractDbName("jdbc:sqlserver://localhost:1433;database=mydb"));
    }

    @Test
    void test解析库名_大小写不敏感() throws Exception {
        assertEquals("mydb", extractDbName("jdbc:sqlserver://localhost:1433;DatabaseName=mydb"));
        assertEquals("mydb", extractDbName("jdbc:sqlserver://localhost:1433;DATABASE=mydb"));
    }

    @Test
    void test解析库名_无库名返回null() throws Exception {
        assertNull(extractDbName("jdbc:sqlserver://localhost:1433;encrypt=false"));
    }

    @Test
    void test解析库名_带额外参数() throws Exception {
        // databaseName 后有其他参数，确保只取到分号前
        assertEquals("test_db", extractDbName(
                "jdbc:sqlserver://43.136.136.34:21433;databaseName=test_db;encrypt=false;trustServerCertificate=true"));
    }

    // ==================== toMasterUrl ====================

    @Test
    void test转masterUrl_databaseName形式() throws Exception {
        assertEquals("jdbc:sqlserver://localhost:1433;databaseName=master;encrypt=false",
                toMasterUrl("jdbc:sqlserver://localhost:1433;databaseName=mydb;encrypt=false"));
    }

    @Test
    void test转masterUrl_database简写形式() throws Exception {
        assertEquals("jdbc:sqlserver://localhost:1433;database=master",
                toMasterUrl("jdbc:sqlserver://localhost:1433;database=mydb"));
    }

    @Test
    void test转masterUrl_无库名追加master() throws Exception {
        assertEquals("jdbc:sqlserver://localhost:1433;encrypt=false;databaseName=master",
                toMasterUrl("jdbc:sqlserver://localhost:1433;encrypt=false"));
    }

    @Test
    void test转masterUrl_大小写不敏感() throws Exception {
        // (?i) 不敏感匹配，替换后统一为小写 databaseName=master
        assertEquals("jdbc:sqlserver://localhost:1433;databaseName=master",
                toMasterUrl("jdbc:sqlserver://localhost:1433;DatabaseName=mydb"));
    }

    @Test
    void test转masterUrl_保留其他参数() throws Exception {
        String result = toMasterUrl("jdbc:sqlserver://h:1433;databaseName=app;encrypt=false;trustServerCertificate=true");
        assertTrue(result.contains("databaseName=master"), result);
        assertTrue(result.contains("encrypt=false"), result);
        assertTrue(result.contains("trustServerCertificate=true"), result);
        assertFalse(result.contains("databaseName=app"), "原库名应被替换: " + result);
    }
}
