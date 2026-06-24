package org.dromara.autotable.strategy.sqlserver;

import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.strategy.IStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SqlServerStrategy 单元测试
 */
public class SqlServerStrategyUnitTest {

    @Test
    void testDatabaseDialect_对齐JDBCProductName() {
        SqlServerStrategy strategy = new SqlServerStrategy();
        // 必须与 mssql-jdbc getDatabaseProductName() 返回值逐字相等
        // 不使用 DatabaseDialect.SQLServer 常量做断言，避免常量内联受 core 编译时序影响
        assertEquals("Microsoft SQL Server", strategy.databaseDialect());
    }

    @Test
    void testWrapIdentifier_方括号包裹() {
        SqlServerStrategy strategy = new SqlServerStrategy();
        IStrategy.setCurrentStrategy(strategy);
        try {
            assertEquals("[test]", strategy.wrapIdentifier("test"));
            // 已包裹的不再重复包裹
            assertEquals("[test]", strategy.wrapIdentifier("[test]"));
            // schema.table 连接
            assertEquals("[dbo].[user]", strategy.concatWrapName("dbo", "user"));
            assertEquals("[user]", strategy.concatWrapName(null, "user"));
        } finally {
            IStrategy.clean();
        }
    }

    @Test
    void testDropTable_含IFEXISTS() {
        SqlServerStrategy strategy = new SqlServerStrategy();
        IStrategy.setCurrentStrategy(strategy);
        try {
            String sql = strategy.dropTable("dbo", "test_table");
            assertEquals("DROP TABLE IF EXISTS [dbo].[test_table]", sql);
        } finally {
            IStrategy.clean();
        }
    }

    @Test
    void testDropTable_无schema() {
        SqlServerStrategy strategy = new SqlServerStrategy();
        IStrategy.setCurrentStrategy(strategy);
        try {
            String sql = strategy.dropTable(null, "test_table");
            assertEquals("DROP TABLE IF EXISTS [test_table]", sql);
        } finally {
            IStrategy.clean();
        }
    }

    @Test
    void testIndexNameMaxLength_128() {
        SqlServerStrategy strategy = new SqlServerStrategy();
        assertEquals(128, strategy.indexNameMaxLength());
    }

    @Test
    void testTypeMapping_非空且含常见类型() {
        SqlServerStrategy strategy = new SqlServerStrategy();
        assertFalse(strategy.typeMapping().isEmpty());
        assertTrue(strategy.typeMapping().containsKey(String.class));
        assertTrue(strategy.typeMapping().containsKey(Integer.class));
        assertTrue(strategy.typeMapping().containsKey(Long.class));
        assertTrue(strategy.typeMapping().containsKey(Boolean.class));
    }

    @Test
    void testTypeMapping_不可变() {
        SqlServerStrategy strategy = new SqlServerStrategy();
        assertThrows(UnsupportedOperationException.class, () ->
                strategy.typeMapping().put(String.class, null));
    }

    @Test
    void testTypeMapping_核心类型映射() {
        SqlServerStrategy strategy = new SqlServerStrategy();
        // 字符串默认 NVARCHAR，避免中文乱码
        assertEquals("NVARCHAR", strategy.typeMapping().get(String.class).getTypeName());
        // Character 映射到定长 NCHAR
        assertEquals("NCHAR", strategy.typeMapping().get(Character.class).getTypeName());
        assertEquals("NCHAR", strategy.typeMapping().get(char.class).getTypeName());
        assertEquals("INT", strategy.typeMapping().get(Integer.class).getTypeName());
        assertEquals("BIGINT", strategy.typeMapping().get(Long.class).getTypeName());
        assertEquals("BIT", strategy.typeMapping().get(Boolean.class).getTypeName());
        assertEquals("DECIMAL", strategy.typeMapping().get(java.math.BigDecimal.class).getTypeName());
    }

    @Test
    void testTypeMapping_NVARCHAR默认长度255() {
        SqlServerStrategy strategy = new SqlServerStrategy();
        assertEquals(Integer.valueOf(255), strategy.typeMapping().get(String.class).getDefaultLength());
    }

    @Test
    void testTypeMapping_DECIMAL精度19位标4() {
        SqlServerStrategy strategy = new SqlServerStrategy();
        assertEquals(Integer.valueOf(19), strategy.typeMapping().get(java.math.BigDecimal.class).getDefaultLength());
        assertEquals(Integer.valueOf(4), strategy.typeMapping().get(java.math.BigDecimal.class).getDefaultDecimalLength());
    }

    @Test
    void testTypeMapping_时间类型映射() {
        SqlServerStrategy strategy = new SqlServerStrategy();
        // LocalDateTime 映射为 DATETIME2（高精度）
        assertEquals("DATETIME2", strategy.typeMapping().get(java.time.LocalDateTime.class).getTypeName());
        assertEquals("DATETIME2", strategy.typeMapping().get(java.util.Date.class).getTypeName());
        assertEquals("DATE", strategy.typeMapping().get(java.time.LocalDate.class).getTypeName());
        assertEquals("TIME", strategy.typeMapping().get(java.time.LocalTime.class).getTypeName());
    }

    @Test
    void testTypeMapping_浮点类型映射() {
        SqlServerStrategy strategy = new SqlServerStrategy();
        assertEquals("REAL", strategy.typeMapping().get(Float.class).getTypeName());
        assertEquals("FLOAT", strategy.typeMapping().get(Double.class).getTypeName());
    }
}
