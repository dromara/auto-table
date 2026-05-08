package org.dromara.autotable.test.core.effect.create;

import org.dromara.autotable.test.core.effect.AbstractEffectTest;
import org.dromara.autotable.test.core.effect.inspector.DbStructureInspector;
import org.dromara.autotable.test.core.entity.common.TestDefineColumn;
import org.dromara.autotable.test.core.entity.common.TestPrimaryKey;
import org.dromara.autotable.test.core.entity.common.TestTableIndex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MySQL 建表效果验证测试
 */
public class MySqlCreateEffectTest extends AbstractEffectTest {

    @Test
    public void testCreateTableWithColumns() {
        initMySqlDataSource();

        String tableName = "test_define_column";
        executeCreateTable(TestDefineColumn.class, tableName);

        DbStructureInspector inspector = getInspector();
        assertTrue(inspector.tableExists(tableName), "表应该存在");

        DbStructureInspector.TableSchema schema = inspector.getTableSchema(tableName);
        assertNotNull(schema, "应该能获取到表结构");
        assertEquals(tableName, schema.getTableName());

        // 验证列存在（id, username, age, phone, money, active, description, register_time）
        // extra 字段应该被 @Ignore 忽略
        assertNotNull(schema.getColumns(), "列信息不应为空");
        assertTrue(schema.getColumns().size() >= 8, "至少应有8个列");

        // 验证主键列不存在于列列表中的单独检查，而是通过 getPrimaryKeys 检查
        assertNotNull(schema.getPrimaryKeys(), "主键信息不应为空");
    }

    @Test
    public void testCreateTableWithPrimaryKey() {
        initMySqlDataSource();

        String tableName = "test_primary_key";
        executeCreateTable(TestPrimaryKey.class, tableName);

        DbStructureInspector inspector = getInspector();
        assertTrue(inspector.tableExists(tableName), "表应该存在");

        DbStructureInspector.TableSchema schema = inspector.getTableSchema(tableName);
        assertNotNull(schema);
        assertNotNull(schema.getPrimaryKeys(), "应有主键信息");
        assertEquals(1, schema.getPrimaryKeys().size(), "应有一个主键");

        DbStructureInspector.PrimaryKeySchema pk = schema.getPrimaryKeys().get(0);
        assertNotNull(pk.getColumns(), "主键应有列");
        assertTrue(pk.getColumns().contains("id"), "主键应包含 id 列");
    }

    @Test
    public void testCreateTableWithIndex() {
        initMySqlDataSource();

        String tableName = "test_table_index";
        executeCreateTable(TestTableIndex.class, tableName);

        DbStructureInspector inspector = getInspector();
        assertTrue(inspector.tableExists(tableName), "表应该存在");

        DbStructureInspector.TableSchema schema = inspector.getTableSchema(tableName);
        assertNotNull(schema);
        assertNotNull(schema.getIndexes(), "应有索引信息");
        assertFalse(schema.getIndexes().isEmpty(), "应至少有一个索引");

        // 验证存在包含 table_index1 和 table_index2 的索引
        boolean hasBiz1Index = schema.getIndexes().stream()
                .anyMatch(idx -> idx.getColumns() != null
                        && idx.getColumns().contains("table_index1")
                        && idx.getColumns().contains("table_index2"));
        assertTrue(hasBiz1Index, "应该存在包含 table_index1 和 table_index2 的索引");
    }
}
