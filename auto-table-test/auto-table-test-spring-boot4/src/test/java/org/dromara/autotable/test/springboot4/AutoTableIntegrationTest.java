package org.dromara.autotable.test.springboot4;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AutoTable Spring Boot 4 集成测试
 * 验证 AutoTable 在 Spring Boot 4 环境下的核心功能
 */
@SpringBootTest
@DisplayName("AutoTable Spring Boot 4 集成测试")
class AutoTableIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("1. 验证 Spring Boot 4 上下文正常启动")
    void testApplicationContextLoads() {
        assertNotNull(dataSource, "DataSource 应该被正确注入");
        assertNotNull(jdbcTemplate, "JdbcTemplate 应该被正确注入");
    }

    @Test
    @DisplayName("2. 验证数据库连接正常")
    void testDatabaseConnection() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "数据库连接不应为空");
            assertFalse(connection.isClosed(), "数据库连接应该是打开的");

            DatabaseMetaData metaData = connection.getMetaData();
            System.out.println("数据库: " + metaData.getDatabaseProductName());
            System.out.println("版本: " + metaData.getDatabaseProductVersion());
            System.out.println("驱动: " + metaData.getDriverName());
        }
    }

    @Test
    @DisplayName("3. 验证 test_user 表自动创建成功")
    void testUserTableCreated() throws Exception {
        // 检查表是否存在（H2 PostgreSQL 模式下表名区分大小写）
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE LOWER(TABLE_NAME) = 'test_user'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);

        assertNotNull(count, "表查询结果不应为空");
        assertTrue(count > 0, "test_user 表应该被自动创建");

        // 验证表结构
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE LOWER(TABLE_NAME) = 'test_user' ORDER BY ORDINAL_POSITION");

        System.out.println("test_user 表结构:");
        columns.forEach(col -> System.out.println("  " + col.get("COLUMN_NAME") + " - " + col.get("DATA_TYPE")));

        // 验证关键字段存在
        List<String> columnNames = columns.stream()
                .map(col -> col.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        assertTrue(columnNames.contains("id"), "应该包含 id 字段");
        assertTrue(columnNames.contains("username"), "应该包含 username 字段");
        assertTrue(columnNames.contains("email"), "应该包含 email 字段");
        assertTrue(columnNames.contains("age"), "应该包含 age 字段");
        assertTrue(columnNames.contains("status"), "应该包含 status 字段");
        assertTrue(columnNames.contains("create_time"), "应该包含 create_time 字段");
        assertTrue(columnNames.contains("update_time"), "应该包含 update_time 字段");
    }

    @Test
    @DisplayName("4. 验证 test_order 表自动创建成功")
    void testOrderTableCreated() throws Exception {
        // 检查表是否存在
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE LOWER(TABLE_NAME) = 'test_order'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);

        assertNotNull(count, "表查询结果不应为空");
        assertTrue(count > 0, "test_order 表应该被自动创建");

        // 验证表结构
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE LOWER(TABLE_NAME) = 'test_order' ORDER BY ORDINAL_POSITION");

        System.out.println("test_order 表结构:");
        columns.forEach(col -> System.out.println("  " + col.get("COLUMN_NAME") + " - " + col.get("DATA_TYPE")));

        // 验证关键字段存在
        List<String> columnNames = columns.stream()
                .map(col -> col.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        assertTrue(columnNames.contains("id"), "应该包含 id 字段");
        assertTrue(columnNames.contains("order_no"), "应该包含 order_no 字段");
        assertTrue(columnNames.contains("user_id"), "应该包含 user_id 字段");
        assertTrue(columnNames.contains("amount"), "应该包含 amount 字段");
        assertTrue(columnNames.contains("order_status"), "应该包含 order_status 字段");
        assertTrue(columnNames.contains("remark"), "应该包含 remark 字段");
        assertTrue(columnNames.contains("order_date"), "应该包含 order_date 字段");
        assertTrue(columnNames.contains("create_time"), "应该包含 create_time 字段");
    }

    @Test
    @DisplayName("5. 验证主键约束正确创建")
    void testPrimaryKeyConstraints() throws Exception {
        // 验证 test_user 表的主键
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                "WHERE LOWER(TABLE_NAME) = 'test_user' AND CONSTRAINT_TYPE = 'PRIMARY KEY'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);

        assertNotNull(count, "主键查询结果不应为空");
        assertTrue(count > 0, "test_user 表应该有主键约束");

        // 验证 test_order 表的主键
        sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                "WHERE LOWER(TABLE_NAME) = 'test_order' AND CONSTRAINT_TYPE = 'PRIMARY KEY'";
        count = jdbcTemplate.queryForObject(sql, Integer.class);

        assertNotNull(count, "主键查询结果不应为空");
        assertTrue(count > 0, "test_order 表应该有主键约束");
    }

    @Test
    @DisplayName("6. 验证表可以正常执行 CRUD 操作")
    void testCrudOperations() {
        // 插入测试数据（H2 PostgreSQL 模式下所有标识符都需要双引号且区分大小写）
        jdbcTemplate.update(
                "INSERT INTO \"test_user\" (\"id\", \"username\", \"email\", \"age\", \"status\", \"create_time\", \"update_time\") VALUES (?, ?, ?, ?, ?, ?, ?)",
                1L, "testuser", "test@example.com", 25, 1,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());

        // 查询验证
        String username = jdbcTemplate.queryForObject(
                "SELECT \"username\" FROM \"test_user\" WHERE \"id\" = ?", String.class, 1L);
        assertEquals("testuser", username, "插入的数据应该能正确查询");

        // 更新验证
        jdbcTemplate.update("UPDATE \"test_user\" SET \"email\" = ? WHERE \"id\" = ?", "new@example.com", 1L);
        String newEmail = jdbcTemplate.queryForObject(
                "SELECT \"email\" FROM \"test_user\" WHERE \"id\" = ?", String.class, 1L);
        assertEquals("new@example.com", newEmail, "更新的数据应该正确");

        // 删除验证
        int rows = jdbcTemplate.update("DELETE FROM \"test_user\" WHERE \"id\" = ?", 1L);
        assertEquals(1, rows, "删除操作应该影响一行");

        // 验证删除成功
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM \"test_user\" WHERE \"id\" = ?", Integer.class, 1L);
        assertEquals(0, count, "删除后应该查询不到数据");
    }

    @Test
    @DisplayName("7. 验证 BigDecimal 类型字段正确创建")
    void testBigDecimalField() {
        // 插入包含 BigDecimal 的测试数据
        jdbcTemplate.update(
                "INSERT INTO \"test_order\" (\"id\", \"order_no\", \"user_id\", \"amount\", \"order_status\", \"remark\", \"order_date\", \"create_time\") VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                1L, "ORD-20250101-001", 1L, new java.math.BigDecimal("99.99"), 1,
                "测试订单", java.time.LocalDate.now(), java.time.LocalDateTime.now());

        // 查询验证金额字段
        java.math.BigDecimal amount = jdbcTemplate.queryForObject(
                "SELECT \"amount\" FROM \"test_order\" WHERE \"id\" = ?", java.math.BigDecimal.class, 1L);
        assertNotNull(amount, "金额字段不应为空");
        assertEquals(0, new java.math.BigDecimal("99.99").compareTo(amount), "金额应该正确存储");

        // 清理数据
        jdbcTemplate.update("DELETE FROM \"test_order\" WHERE \"id\" = ?", 1L);
    }

    @Test
    @DisplayName("8. 验证 AutoTable 配置正确加载")
    void testAutoTableConfiguration() {
        // 验证 AutoTable 相关表是否创建（如果有版本记录表）
        try {
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'");

            System.out.println("数据库中的所有表:");
            tables.forEach(table -> System.out.println("  " + table.get("TABLE_NAME")));

            // 至少应该有我们创建的两个表
            assertTrue(tables.size() >= 2, "应该至少创建两个测试表");
        } catch (Exception e) {
            fail("查询表列表失败: " + e.getMessage());
        }
    }
}
