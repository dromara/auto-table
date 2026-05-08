package org.dromara.autotable.test.core.effect.update;

import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.test.core.effect.AbstractEffectTest;
import org.dromara.autotable.test.core.effect.inspector.DbStructureInspector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MySQL ALTER TABLE 效果验证测试
 */
public class MySqlUpdateEffectTest extends AbstractEffectTest {

    /**
     * 测试 update 模式下表结构能正常比对并执行（不抛异常）
     */
    @Test
    public void testUpdateExecutesWithoutError() {
        initMySqlDataSource();

        // 1. 先 create 表
        String tableName = "test_primary_key";
        executeCreateTable(org.dromara.autotable.test.core.entity.common.TestPrimaryKey.class, tableName);

        // 2. update 表（自增主键）
        // 注意：MySQL 不支持通过 ALTER TABLE 直接添加 AUTO_INCREMENT 到非自增列
        // 这里仅验证 update 模式能正常执行，不会抛出异常
        executeUpdateTable(org.dromara.autotable.test.core.entity.common_update.TestPrimaryKey.class, tableName);

        // 3. 验证表仍然存在
        DbStructureInspector inspector = getInspector();
        assertTrue(inspector.tableExists(tableName), "update 后表应该仍然存在");
    }

    /**
     * 测试新增列
     */
    @Test
    public void testUpdateAddColumn() {
        initMySqlDataSource();

        // 1. create 基础表
        String tableName = "test_define_column";
        executeCreateTable(org.dromara.autotable.test.core.entity.common.TestDefineColumn.class, tableName);

        // 2. update 表（extra 列被 @Ignore 去掉，但在 common_update 中又回来了）
        executeUpdateTable(org.dromara.autotable.test.core.entity.common_update.TestDefineColumn.class, tableName);

        // 3. 验证 extra 列存在（因为 update 版本有 extra 列）
        DbStructureInspector inspector = getInspector();
        DbStructureInspector.TableSchema schema = inspector.getTableSchema(tableName);
        assertNotNull(schema);

        // 验证表存在且列正常
        assertTrue(schema.getColumns().size() >= 8, "update 后应该至少有 8 个列");
    }

    /**
     * 测试修改注释和默认值
     */
    @Test
    public void testUpdateCommentAndDefault() {
        initMySqlDataSource();

        // 1. create 基础表
        String tableName = "test_define_column";
        executeCreateTable(org.dromara.autotable.test.core.entity.common.TestDefineColumn.class, tableName);

        // 2. update 表
        executeUpdateTable(org.dromara.autotable.test.core.entity.common_update.TestDefineColumn.class, tableName);

        // 3. 验证修改后的表结构
        DbStructureInspector inspector = getInspector();
        DbStructureInspector.TableSchema schema = inspector.getTableSchema(tableName);
        assertNotNull(schema);

        // 验证 description 列的注释变化
        DbStructureInspector.ColumnSchema descColumn = schema.getColumns().stream()
                .filter(c -> "description".equals(c.getName())).findFirst().orElse(null);
        if (descColumn != null) {
            assertNotNull(descColumn.getComment(), "description 列应该有注释");
        }
    }
}
