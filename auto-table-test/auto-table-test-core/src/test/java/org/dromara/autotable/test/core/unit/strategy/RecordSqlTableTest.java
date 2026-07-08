package org.dromara.autotable.test.core.unit.strategy;

import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.recordsql.AutoTableExecuteSqlLog;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.strategy.mysql.MysqlStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AutoTableExecuteSqlLog 建表 SQL 验证。
 * <p>
 * 回归测试：确保 SQL 记录表包含主键（issue #9），
 * 在 Percona XtraDB Cluster / MySQL Group Replication / sql_require_primary_key 等
 * 要求所有表必须有主键的环境下不会报错。
 */
public class RecordSqlTableTest {

    private MysqlStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new MysqlStrategy();
        IStrategy.setCurrentStrategy(strategy);
        // 注册类型映射（createTable 需要完整的类型映射）
        AutoTableGlobalConfig.instance().addStrategy(strategy);
    }

    @AfterEach
    void tearDown() {
        IStrategy.clean();
    }

    @Test
    void testCreateTableSql_hasPrimaryKey() {
        List<String> sqlList = strategy.createTable(AutoTableExecuteSqlLog.class, tableMetadata -> tableMetadata);
        assertFalse(sqlList.isEmpty(), "建表 SQL 不应为空");

        String createSql = String.join("\n", sqlList);
        // 验证包含 id 列
        assertTrue(createSql.contains("id"), "建表 SQL 应包含 id 列，实际 SQL:\n" + createSql);
        // 验证包含主键定义
        assertTrue(createSql.toUpperCase().contains("PRIMARY KEY"),
                "建表 SQL 应包含 PRIMARY KEY 定义，实际 SQL:\n" + createSql);
        // 验证包含自增
        assertTrue(createSql.toUpperCase().contains("AUTO_INCREMENT"),
                "建表 SQL 应包含 AUTO_INCREMENT，实际 SQL:\n" + createSql);
    }

    @Test
    void testCreateTableSql_hasExpectedColumns() {
        List<String> sqlList = strategy.createTable(AutoTableExecuteSqlLog.class, tableMetadata -> tableMetadata);
        String createSql = String.join("\n", sqlList).toLowerCase();

        // 验证业务字段都存在
        assertTrue(createSql.contains("table_schema"), "应包含 table_schema 列");
        assertTrue(createSql.contains("table_name"), "应包含 table_name 列");
        assertTrue(createSql.contains("sql_statement"), "应包含 sql_statement 列");
        assertTrue(createSql.contains("version"), "应包含 version 列");
        assertTrue(createSql.contains("execution_time"), "应包含 execution_time 列");
        assertTrue(createSql.contains("execution_end_time"), "应包含 execution_end_time 列");
    }

    @Test
    void testCreateTableSql_entityClassIgnored() {
        List<String> sqlList = strategy.createTable(AutoTableExecuteSqlLog.class, tableMetadata -> tableMetadata);
        String createSql = String.join("\n", sqlList).toLowerCase();

        // entityClass 字段标注了 @Ignore，不应出现在建表 SQL 中
        assertFalse(createSql.contains("entity_class"),
                "entityClass（@Ignore）不应出现在建表 SQL 中，实际 SQL:\n" + createSql);
    }
}
