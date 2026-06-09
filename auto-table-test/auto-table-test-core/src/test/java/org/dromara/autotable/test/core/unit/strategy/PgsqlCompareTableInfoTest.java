package org.dromara.autotable.test.core.unit.strategy;

import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.strategy.pgsql.data.PgsqlCompareTableInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PgsqlCompareTableInfo 单元测试
 */
public class PgsqlCompareTableInfoTest {

    @Test
    void testNeedModify_initialState() {
        PgsqlCompareTableInfo info = new PgsqlCompareTableInfo("test_table", "public");
        assertFalse(info.needModify(), "初始状态不应需要修改");
    }

    @Test
    void testNeedModify_withComment() {
        PgsqlCompareTableInfo info = new PgsqlCompareTableInfo("test_table", "public");
        info.setComment("新注释");
        assertTrue(info.needModify(), "有注释变更时应需要修改");
    }

    @Test
    void testNeedModify_withDropPrimaryKey() {
        PgsqlCompareTableInfo info = new PgsqlCompareTableInfo("test_table", "public");
        info.setDropPrimaryKeyName("test_pkey");
        assertTrue(info.needModify(), "有主键删除时应需要修改");
    }

    @Test
    void testNeedModify_withNewPrimary() {
        PgsqlCompareTableInfo info = new PgsqlCompareTableInfo("test_table", "public");
        ColumnMetadata pk = new ColumnMetadata();
        pk.setName("id");
        info.addNewPrimary(Arrays.asList(pk));
        assertTrue(info.needModify(), "有新主键时应需要修改");
    }

    @Test
    void testNeedModify_withDropColumn() {
        PgsqlCompareTableInfo info = new PgsqlCompareTableInfo("test_table", "public");
        info.getDropColumnList().add("old_col");
        assertTrue(info.needModify(), "有列删除时应需要修改");
    }

    @Test
    void testNeedModify_withRenameColumn() {
        PgsqlCompareTableInfo info = new PgsqlCompareTableInfo("test_table", "public");
        info.getRenameColumnMap().put("old", "_del_old");
        assertTrue(info.needModify(), "有列重命名时应需要修改");
    }

    @Test
    void testNeedModify_withNewColumn() {
        PgsqlCompareTableInfo info = new PgsqlCompareTableInfo("test_table", "public");
        ColumnMetadata col = new ColumnMetadata();
        col.setName("new_col");
        info.addNewColumn(col);
        assertTrue(info.needModify(), "有新列时应需要修改");
    }

    @Test
    void testNeedModify_withModifyColumn() {
        PgsqlCompareTableInfo info = new PgsqlCompareTableInfo("test_table", "public");
        ColumnMetadata col = new ColumnMetadata();
        col.setName("col");
        col.setType(new DatabaseTypeAndLength("varchar", 255, null, Collections.emptyList()));
        info.addModifyColumn(col, true, false, false);
        assertTrue(info.needModify(), "有列修改时应需要修改");
    }

    @Test
    void testNeedModify_withDropIndex() {
        PgsqlCompareTableInfo info = new PgsqlCompareTableInfo("test_table", "public");
        info.getDropIndexList().add("old_idx");
        assertTrue(info.needModify(), "有索引删除时应需要修改");
    }

    @Test
    void testNeedModify_withNewIndex() {
        PgsqlCompareTableInfo info = new PgsqlCompareTableInfo("test_table", "public");
        IndexMetadata idx = new IndexMetadata();
        idx.setName("new_idx");
        info.addNewIndex(idx);
        assertTrue(info.needModify(), "有新索引时应需要修改");
    }

    @Test
    void testValidateFailedMessage_comprehensive() {
        PgsqlCompareTableInfo info = new PgsqlCompareTableInfo("test_table", "public");
        info.setComment("新注释");
        info.setDropPrimaryKeyName("test_pkey");

        ColumnMetadata col = new ColumnMetadata();
        col.setName("col");
        col.setType(new DatabaseTypeAndLength("varchar", 255, null, Collections.emptyList()));
        info.addModifyColumn(col, true, false, false);
        info.getDropColumnList().add("old_col");
        info.getRenameColumnMap().put("del_col", "_del_del_col");

        String msg = info.validateFailedMessage();

        assertTrue(msg.contains("表注释变更"), "应包含表注释变更");
        assertTrue(msg.contains("删除主键"), "应包含主键删除");
        assertTrue(msg.contains("修改列"), "应包含列修改");
        assertTrue(msg.contains("删除列"), "应包含列删除");
        assertTrue(msg.contains("重命名列"), "应包含列重命名");
    }

    @Test
    void testAddRenameColumns() {
        PgsqlCompareTableInfo info = new PgsqlCompareTableInfo("test_table", "public");
        info.addRenameColumns(new HashSet<>(Arrays.asList("col1", "col2")), "_del_");

        assertEquals(2, info.getRenameColumnMap().size());
        assertEquals("_del_col1", info.getRenameColumnMap().get("col1"));
        assertEquals("_del_col2", info.getRenameColumnMap().get("col2"));
    }

    @Test
    void testAddModifyIndex() {
        PgsqlCompareTableInfo info = new PgsqlCompareTableInfo("test_table", "public");
        IndexMetadata idx = new IndexMetadata();
        idx.setName("idx_test");
        info.addModifyIndex(idx);

        // addModifyIndex 应同时添加删除旧索引和新增新索引
        assertTrue(info.getDropIndexList().contains("idx_test"), "应标记删除旧索引");
        assertEquals(1, info.getIndexMetadataList().size(), "应标记新增索引");
    }

    @Test
    void testColumnComment() {
        PgsqlCompareTableInfo info = new PgsqlCompareTableInfo("test_table", "public");
        info.addColumnComment("name", "新名称注释");

        assertEquals(1, info.getColumnComment().size());
        assertEquals("新名称注释", info.getColumnComment().get("name"));
    }

    @Test
    void testIndexComment() {
        PgsqlCompareTableInfo info = new PgsqlCompareTableInfo("test_table", "public");
        info.addIndexComment("idx_test", "索引注释");

        assertEquals(1, info.getIndexComment().size());
        assertEquals("索引注释", info.getIndexComment().get("idx_test"));
    }
}
