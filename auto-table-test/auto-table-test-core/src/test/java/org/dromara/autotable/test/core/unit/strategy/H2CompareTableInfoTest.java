package org.dromara.autotable.strategy.h2.data;

import org.dromara.autotable.core.strategy.IndexMetadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * H2CompareTableInfo 单元测试
 */
public class H2CompareTableInfoTest {

    @Test
    void testAddModifyIndex_shouldNotAutoAddComment() {
        H2CompareTableInfo compareInfo = new H2CompareTableInfo("test_table", "PUBLIC");

        IndexMetadata indexMetadata = new IndexMetadata();
        indexMetadata.setName("idx_name");
        indexMetadata.setComment("测试索引");

        // 添加修改的索引
        compareInfo.addModifyIndex(indexMetadata);

        // 验证索引被添加到 dropIndexList 和 indexMetadataList
        assertTrue(compareInfo.getDropIndexList().contains("idx_name"));
        assertEquals(1, compareInfo.getIndexMetadataList().size());
        assertEquals("idx_name", compareInfo.getIndexMetadataList().get(0).getName());

        // 验证注释没有被自动添加到 indexComment map（修复 #5）
        assertFalse(compareInfo.getIndexComment().containsKey("idx_name"));
    }

    @Test
    void testAddModifyIndex_withoutComment() {
        H2CompareTableInfo compareInfo = new H2CompareTableInfo("test_table", "PUBLIC");

        IndexMetadata indexMetadata = new IndexMetadata();
        indexMetadata.setName("idx_email");
        // 不设置注释

        compareInfo.addModifyIndex(indexMetadata);

        // 验证索引被添加
        assertTrue(compareInfo.getDropIndexList().contains("idx_email"));
        assertEquals(1, compareInfo.getIndexMetadataList().size());

        // 验证注释没有被自动添加
        assertFalse(compareInfo.getIndexComment().containsKey("idx_email"));
    }

    @Test
    void testAddIndexComment_shouldAddComment() {
        H2CompareTableInfo compareInfo = new H2CompareTableInfo("test_table", "PUBLIC");

        // 显式添加索引注释
        compareInfo.addIndexComment("idx_name", "测试索引");

        // 验证注释被添加
        assertTrue(compareInfo.getIndexComment().containsKey("idx_name"));
        assertEquals("测试索引", compareInfo.getIndexComment().get("idx_name"));
    }

    @Test
    void testAddNewIndex_shouldNotAutoAddComment() {
        H2CompareTableInfo compareInfo = new H2CompareTableInfo("test_table", "PUBLIC");

        IndexMetadata indexMetadata = new IndexMetadata();
        indexMetadata.setName("idx_name");
        indexMetadata.setComment("测试索引");

        compareInfo.addNewIndex(indexMetadata);

        // 验证索引被添加
        assertEquals(1, compareInfo.getIndexMetadataList().size());

        // 验证注释没有被自动添加（与 addModifyIndex 行为一致）
        assertFalse(compareInfo.getIndexComment().containsKey("idx_name"));
    }

    @Test
    void testNeedModify_withIndexChanges() {
        H2CompareTableInfo compareInfo = new H2CompareTableInfo("test_table", "PUBLIC");

        // 初始状态不需要修改
        assertFalse(compareInfo.needModify());

        // 添加新索引后需要修改
        IndexMetadata indexMetadata = new IndexMetadata();
        indexMetadata.setName("idx_name");
        compareInfo.addNewIndex(indexMetadata);

        assertTrue(compareInfo.needModify());
    }

    @Test
    void testNeedModify_withDropIndex() {
        H2CompareTableInfo compareInfo = new H2CompareTableInfo("test_table", "PUBLIC");

        // 初始状态不需要修改
        assertFalse(compareInfo.needModify());

        // 添加删除索引后需要修改
        compareInfo.getDropIndexList().add("idx_old");

        assertTrue(compareInfo.needModify());
    }

    @Test
    void testValidateFailedMessage_withIndexChanges() {
        H2CompareTableInfo compareInfo = new H2CompareTableInfo("test_table", "PUBLIC");

        IndexMetadata indexMetadata = new IndexMetadata();
        indexMetadata.setName("idx_name");
        compareInfo.addNewIndex(indexMetadata);

        compareInfo.getDropIndexList().add("idx_old");

        String message = compareInfo.validateFailedMessage();

        assertNotNull(message);
        assertTrue(message.contains("idx_name") || message.contains("idx_old"));
    }
}
