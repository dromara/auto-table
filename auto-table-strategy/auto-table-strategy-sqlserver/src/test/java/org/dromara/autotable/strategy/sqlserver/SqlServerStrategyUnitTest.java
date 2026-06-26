package org.dromara.autotable.strategy.sqlserver;

import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.annotation.enums.IndexSortTypeEnum;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.strategy.sqlserver.data.dbdata.SqlServerDbIndex;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

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

    // ==================== isIndexSame（索引列顺序 + 排序方向）====================

    @Test
    void testIsIndexSame_列序排序一致_相同() throws Exception {
        assertTrue(invokeIsIndexSame(
                dbIndex("NO", "a,b", "ASC,DESC"),
                indexColumns(IndexTypeEnum.NORMAL,
                        IndexMetadata.IndexColumnParam.newInstance("a", IndexSortTypeEnum.ASC),
                        IndexMetadata.IndexColumnParam.newInstance("b", IndexSortTypeEnum.DESC))));
    }

    @Test
    void testIsIndexSame_列顺序不同_不一致() throws Exception {
        // DB 为 (a,b)，实体为 (b,a) → 顺序不同，判变更
        assertFalse(invokeIsIndexSame(
                dbIndex("NO", "a,b", "ASC,ASC"),
                indexColumns(IndexTypeEnum.NORMAL,
                        IndexMetadata.IndexColumnParam.newInstance("b", null),
                        IndexMetadata.IndexColumnParam.newInstance("a", null))));
    }

    @Test
    void testIsIndexSame_列数不同_不一致() throws Exception {
        assertFalse(invokeIsIndexSame(
                dbIndex("NO", "a,b", "ASC,ASC"),
                indexColumns(IndexTypeEnum.NORMAL,
                        IndexMetadata.IndexColumnParam.newInstance("a", null))));
    }

    @Test
    void testIsIndexSame_实体指定排序且与DB不同_不一致() throws Exception {
        // 实体指定 b 为 DESC，DB 为 ASC → 不一致
        assertFalse(invokeIsIndexSame(
                dbIndex("NO", "a,b", "ASC,ASC"),
                indexColumns(IndexTypeEnum.NORMAL,
                        IndexMetadata.IndexColumnParam.newInstance("a", null),
                        IndexMetadata.IndexColumnParam.newInstance("b", IndexSortTypeEnum.DESC))));
    }

    @Test
    void testIsIndexSame_实体未指定排序_接受DB任意() throws Exception {
        // 实体未指定 sort（null），DB 为 DESC → 视为一致（对齐 mysql）
        assertTrue(invokeIsIndexSame(
                dbIndex("NO", "a", "DESC"),
                indexColumns(IndexTypeEnum.NORMAL,
                        IndexMetadata.IndexColumnParam.newInstance("a", null))));
    }

    @Test
    void testIsIndexSame_DB无排序信息_实体未指定_一致() throws Exception {
        // DB indexColumnSorts 为空，实体未指定 sort → 一致
        assertTrue(invokeIsIndexSame(
                dbIndex("NO", "a", null),
                indexColumns(IndexTypeEnum.NORMAL,
                        IndexMetadata.IndexColumnParam.newInstance("a", null))));
    }

    @Test
    void testIsIndexSame_唯一性不同_不一致() throws Exception {
        assertFalse(invokeIsIndexSame(
                dbIndex("YES", "a", "ASC"),
                indexColumns(IndexTypeEnum.NORMAL,
                        IndexMetadata.IndexColumnParam.newInstance("a", null))));
    }

    private boolean invokeIsIndexSame(SqlServerDbIndex dbIndex, IndexMetadata indexMetadata) throws Exception {
        SqlServerStrategy strategy = new SqlServerStrategy();
        java.lang.reflect.Method m = SqlServerStrategy.class
                .getDeclaredMethod("isIndexSame", SqlServerDbIndex.class, IndexMetadata.class);
        m.setAccessible(true);
        return (boolean) m.invoke(strategy, dbIndex, indexMetadata);
    }

    private SqlServerDbIndex dbIndex(String isUnique, String indexColumns, String indexColumnSorts) {
        SqlServerDbIndex dbIndex = new SqlServerDbIndex();
        dbIndex.setIsUnique(isUnique);
        dbIndex.setIndexColumns(indexColumns);
        dbIndex.setIndexColumnSorts(indexColumnSorts);
        return dbIndex;
    }

    private IndexMetadata indexColumns(IndexTypeEnum type, IndexMetadata.IndexColumnParam... cols) {
        IndexMetadata idx = new IndexMetadata();
        idx.setType(type);
        idx.setColumns(Arrays.asList(cols));
        return idx;
    }
}
