package org.dromara.autotable.strategy.sqlserver;

import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.strategy.sqlserver.data.SqlServerCompareTableInfo;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SqlServerCompareTableInfo 单元测试。
 * 验证 needModify 触发条件、validateFailedMessage 输出、addModifyColumn 携带默认约束名。
 */
public class SqlServerCompareTableInfoTest {

    private SqlServerCompareTableInfo newInfo() {
        return new SqlServerCompareTableInfo("user", "dbo");
    }

    private ColumnMetadata column(String name) {
        ColumnMetadata c = new ColumnMetadata();
        c.setName(name);
        c.setType(new DatabaseTypeAndLength("NVARCHAR", 255, null, Collections.emptyList()));
        return c;
    }

    @Test
    void test空差异_needModify为false() {
        assertFalse(newInfo().needModify());
    }

    @Test
    void test表注释变更_触发needModify() {
        SqlServerCompareTableInfo ci = newInfo();
        ci.setComment("新注释");
        assertTrue(ci.needModify());
    }

    @Test
    void test各差异字段独立触发needModify() {
        // 主键删除
        SqlServerCompareTableInfo ci1 = newInfo();
        ci1.setDropPrimaryKeyName("PK_user");
        assertTrue(ci1.needModify());

        // 新增主键
        SqlServerCompareTableInfo ci2 = newInfo();
        ci2.addNewPrimary(Collections.singletonList(column("id")));
        assertTrue(ci2.needModify());

        // 列注释
        SqlServerCompareTableInfo ci3 = newInfo();
        ci3.addColumnComment("name", "姓名");
        assertTrue(ci3.needModify());

        // 索引注释
        SqlServerCompareTableInfo ci4 = newInfo();
        ci4.addIndexComment("idx_name", "索引注释");
        assertTrue(ci4.needModify());

        // 删除列
        SqlServerCompareTableInfo ci5 = newInfo();
        ci5.addDropColumns(Collections.singleton("old_col"));
        assertTrue(ci5.needModify());

        // 重命名列
        SqlServerCompareTableInfo ci6 = newInfo();
        ci6.addRenameColumns(Collections.singleton("old_col"), "del_");
        assertTrue(ci6.needModify());

        // 修改列
        SqlServerCompareTableInfo ci7 = newInfo();
        ci7.addModifyColumn(column("name"), true, false, false, null);
        assertTrue(ci7.needModify());

        // 新增列
        SqlServerCompareTableInfo ci8 = newInfo();
        ci8.addNewColumn(column("email"));
        assertTrue(ci8.needModify());

        // 删除索引
        SqlServerCompareTableInfo ci9 = newInfo();
        ci9.addDropIndexes(Collections.singleton("old_idx"));
        assertTrue(ci9.needModify());

        // 新增索引
        SqlServerCompareTableInfo ci10 = newInfo();
        IndexMetadata idx = new IndexMetadata();
        idx.setName("idx_name");
        ci10.addNewIndex(idx);
        assertTrue(ci10.needModify());
    }

    @Test
    void testaddModifyColumn_携带默认约束名() {
        SqlServerCompareTableInfo ci = newInfo();
        ci.addModifyColumn(column("status"), false, false, true, "DF__user__status");
        assertEquals(1, ci.getModifyColumnMetadataList().size());
        SqlServerCompareTableInfo.SqlServerModifyColumnMetadata m = ci.getModifyColumnMetadataList().get(0);
        assertEquals("status", m.getColumnMetadata().getName());
        assertFalse(m.isTypeChanged());
        assertFalse(m.isNotNullChanged());
        assertTrue(m.isDefaultChanged());
        assertEquals("DF__user__status", m.getDefaultConstraintName());
    }

    @Test
    void testaddModifyColumn_约束名可为null() {
        SqlServerCompareTableInfo ci = newInfo();
        ci.addModifyColumn(column("status"), true, false, false, null);
        assertNull(ci.getModifyColumnMetadataList().get(0).getDefaultConstraintName());
    }

    @Test
    void testaddModifyIndex_同时记录drop和新增() {
        SqlServerCompareTableInfo ci = newInfo();
        IndexMetadata idx = new IndexMetadata();
        idx.setName("idx_name");
        ci.addModifyIndex(idx);
        // 修改索引 = 先 drop 旧 + 重新 create
        assertEquals(1, ci.getDropIndexList().size());
        assertEquals("idx_name", ci.getDropIndexList().get(0));
        assertEquals(1, ci.getIndexMetadataList().size());
    }

    @Test
    void testvalidateFailedMessage_空差异返回空串() {
        // 无差异时 message 应为空（不触发 validate 抛异常）
        assertTrue(newInfo().validateFailedMessage().isEmpty());
    }

    @Test
    void testvalidateFailedMessage_包含各差异描述() {
        SqlServerCompareTableInfo ci = newInfo();
        ci.setComment("新表注释");
        ci.setDropPrimaryKeyName("PK_old");
        ci.addDropColumns(Collections.singleton("old_col"));
        ci.addNewColumn(column("new_col"));
        ci.addDropIndexes(Collections.singleton("old_idx"));

        String msg = ci.validateFailedMessage();
        assertTrue(msg.contains("表注释变更: 新表注释"), msg);
        assertTrue(msg.contains("删除主键: PK_old"), msg);
        assertTrue(msg.contains("删除列: old_col"), msg);
        assertTrue(msg.contains("新增列: new_col"), msg);
        assertTrue(msg.contains("删除索引: old_idx"), msg);
    }

    @Test
    void testvalidateFailedMessage_重命名列展示映射() {
        SqlServerCompareTableInfo ci = newInfo();
        ci.addRenameColumns(Collections.singleton("old_col"), "del_");
        String msg = ci.validateFailedMessage();
        // old_col -> del_old_col（前缀 + 原列名）
        assertTrue(msg.contains("old_col -> del_old_col"), msg);
    }

    @Test
    void test构造器_name和schema正确赋值() {
        SqlServerCompareTableInfo ci = new SqlServerCompareTableInfo("user", "dbo");
        assertEquals("user", ci.getName());
        assertEquals("dbo", ci.getSchema());
    }

    @Test
    void test各列表字段默认空集合() {
        SqlServerCompareTableInfo ci = newInfo();
        assertNotNull(ci.getNewPrimaries());
        assertNotNull(ci.getDropColumnList());
        assertNotNull(ci.getModifyColumnMetadataList());
        assertNotNull(ci.getNewColumnMetadataList());
        assertNotNull(ci.getDropIndexList());
        assertNotNull(ci.getIndexMetadataList());
        assertNotNull(ci.getColumnComment());
        assertNotNull(ci.getIndexComment());
        assertNotNull(ci.getRenameColumnMap());
        assertTrue(ci.getNewPrimaries().isEmpty());
    }
}
