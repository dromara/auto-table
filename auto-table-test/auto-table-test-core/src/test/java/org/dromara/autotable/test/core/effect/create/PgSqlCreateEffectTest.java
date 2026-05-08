package org.dromara.autotable.test.core.effect.create;

import org.dromara.autotable.test.core.effect.AbstractEffectTest;
import org.dromara.autotable.test.core.effect.inspector.DbStructureInspector;
import org.dromara.autotable.test.core.entity.common.TestDefineColumn;
import org.dromara.autotable.test.core.entity.common.TestPrimaryKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL 建表效果验证测试
 */
public class PgSqlCreateEffectTest extends AbstractEffectTest {

    @Test
    public void testCreateTableWithColumns() {
        initPgSqlDataSource();

        String tableName = "test_define_column";
        executeCreateTable(TestDefineColumn.class, tableName);

        DbStructureInspector inspector = getInspector();
        assertTrue(inspector.tableExists(tableName), "表应该存在");

        DbStructureInspector.TableSchema schema = inspector.getTableSchema(tableName);
        assertNotNull(schema, "应该能获取到表结构");
        assertEquals(tableName, schema.getTableName());

        assertNotNull(schema.getColumns(), "列信息不应为空");
        assertTrue(schema.getColumns().size() >= 8, "至少应有8个列");
    }

    @Test
    public void testCreateTableWithPrimaryKey() {
        initPgSqlDataSource();

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
}
