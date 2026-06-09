package org.dromara.autotable.test.core.unit.builder;

import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.strategy.mysql.MysqlStrategy;
import org.dromara.autotable.strategy.mysql.builder.ModifyTableSqlBuilder;
import org.dromara.autotable.strategy.mysql.data.MysqlColumnMetadata;
import org.dromara.autotable.strategy.mysql.data.MysqlCompareTableInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 ModifyTableSqlBuilder 生成的主键字段修改 SQL
 */
public class ModifyTableSqlBuilderTest {

    @BeforeEach
    void setUp() {
        IStrategy.setCurrentStrategy(new MysqlStrategy());
    }

    @AfterEach
    void tearDown() {
        IStrategy.clean();
        AutoTableGlobalConfig.clear();
    }

    @Test
    void testModifyPrimaryKeyColumn() {
        MysqlCompareTableInfo compareTableInfo = new MysqlCompareTableInfo("test_table", "");

        // 构造一个需要修改的主键字段
        MysqlColumnMetadata columnMetadata = new MysqlColumnMetadata();
        columnMetadata.setName("event_id");
        columnMetadata.setComment("事件id");
        columnMetadata.setType(new DatabaseTypeAndLength("varchar", 128, null, Collections.emptyList()));
        columnMetadata.setNotNull(true);
        columnMetadata.setPrimary(true);
        columnMetadata.setPosition(1);

        compareTableInfo.addEditColumnMetadata(columnMetadata);

        // 生成 SQL
        List<String> sqlList = ModifyTableSqlBuilder.buildSql(compareTableInfo);

        System.out.println("Generated SQL:");
        for (String sql : sqlList) {
            System.out.println(sql);
        }

        assertFalse(sqlList.isEmpty(), "应该生成 SQL");
        String sql = sqlList.get(0);
        assertTrue(sql.contains("MODIFY COLUMN"), "应该包含 MODIFY COLUMN");
        assertTrue(sql.contains("event_id"), "应该包含 event_id");
        assertTrue(sql.contains("varchar(128)"), "应该包含 varchar(128)");
    }

    @Test
    void testAlterTableSeparateDrop() {
        // 开启分离模式
        AutoTableGlobalConfig.instance().getAutoTableProperties().getMysql().setAlterTableSeparateDrop(true);

        MysqlCompareTableInfo compareTableInfo = new MysqlCompareTableInfo("test_table", "");

        // 添加删除列
        compareTableInfo.getDropColumnList().add("old_column");

        // 添加新列（使用 addNewColumnMetadata）
        MysqlColumnMetadata newColumn = new MysqlColumnMetadata();
        newColumn.setName("new_column");
        newColumn.setType(new DatabaseTypeAndLength("varchar", 100, null, Collections.emptyList()));
        newColumn.setPosition(1);
        compareTableInfo.addNewColumnMetadata(newColumn);

        List<String> sqlList = ModifyTableSqlBuilder.buildSql(compareTableInfo);

        // 应该生成两条 SQL：一条 DROP，一条 ADD
        assertEquals(2, sqlList.size(), "分离模式下应该生成两条 SQL");
        assertTrue(sqlList.get(0).contains("DROP COLUMN"), "第一条应该是 DROP");
        assertTrue(sqlList.get(1).contains("ADD COLUMN"), "第二条应该是 ADD COLUMN");
    }

    @Test
    void testAlterTableDefaultMode() {
        // 默认模式（alterTableSeparateDrop=false）
        AutoTableGlobalConfig.instance().getAutoTableProperties().getMysql().setAlterTableSeparateDrop(false);

        MysqlCompareTableInfo compareTableInfo = new MysqlCompareTableInfo("test_table", "");

        // 添加删除列
        compareTableInfo.getDropColumnList().add("old_column");

        // 添加新列
        MysqlColumnMetadata newColumn = new MysqlColumnMetadata();
        newColumn.setName("new_column");
        newColumn.setType(new DatabaseTypeAndLength("varchar", 100, null, Collections.emptyList()));
        newColumn.setPosition(1);
        compareTableInfo.addNewColumnMetadata(newColumn);

        List<String> sqlList = ModifyTableSqlBuilder.buildSql(compareTableInfo);

        // 默认模式下应该合并为一条 SQL
        assertEquals(1, sqlList.size(), "默认模式下应该生成一条 SQL");
        assertTrue(sqlList.get(0).contains("DROP COLUMN"), "应该包含 DROP COLUMN");
        assertTrue(sqlList.get(0).contains("ADD COLUMN"), "应该包含 ADD COLUMN");
    }

    @Test
    void testAddAndModifyColumns() {
        MysqlCompareTableInfo compareTableInfo = new MysqlCompareTableInfo("test_table", "");

        // 添加新列
        MysqlColumnMetadata addColumn = new MysqlColumnMetadata();
        addColumn.setName("new_col");
        addColumn.setType(new DatabaseTypeAndLength("varchar", 50, null, Collections.emptyList()));
        addColumn.setPosition(2);
        compareTableInfo.addNewColumnMetadata(addColumn);

        // 修改已有列
        MysqlColumnMetadata modifyColumn = new MysqlColumnMetadata();
        modifyColumn.setName("existing_col");
        modifyColumn.setType(new DatabaseTypeAndLength("varchar", 200, null, Collections.emptyList()));
        modifyColumn.setPosition(1);
        compareTableInfo.addEditColumnMetadata(modifyColumn);

        List<String> sqlList = ModifyTableSqlBuilder.buildSql(compareTableInfo);

        assertFalse(sqlList.isEmpty());
        String sql = sqlList.get(0);
        assertTrue(sql.contains("ADD COLUMN `new_col`"), "应该包含 ADD COLUMN");
        assertTrue(sql.contains("MODIFY COLUMN `existing_col`"), "应该包含 MODIFY COLUMN");
    }

    @Test
    void testDropColumn() {
        MysqlCompareTableInfo compareTableInfo = new MysqlCompareTableInfo("test_table", "");
        compareTableInfo.getDropColumnList().add("obsolete_col");

        List<String> sqlList = ModifyTableSqlBuilder.buildSql(compareTableInfo);

        assertFalse(sqlList.isEmpty());
        assertTrue(sqlList.get(0).contains("DROP COLUMN `obsolete_col`"));
    }

    @Test
    void testDropAndAddIndex() {
        MysqlCompareTableInfo compareTableInfo = new MysqlCompareTableInfo("test_table", "");

        // 删除索引
        compareTableInfo.getDropIndexList().add("old_index");

        // 添加新索引
        IndexMetadata newIndex = new IndexMetadata();
        newIndex.setName("new_index");
        newIndex.setType(IndexTypeEnum.NORMAL);
        newIndex.setColumns(Arrays.asList(
            IndexMetadata.IndexColumnParam.newInstance("col1", null)
        ));
        compareTableInfo.getIndexMetadataList().add(newIndex);

        List<String> sqlList = ModifyTableSqlBuilder.buildSql(compareTableInfo);

        assertFalse(sqlList.isEmpty());
        String sql = sqlList.get(0);
        // DROP INDEX 和 ADD INDEX 在同一条 ALTER TABLE 中
        assertTrue(sql.contains("DROP INDEX"), "应该包含 DROP INDEX");
        assertTrue(sql.contains("`old_index`"), "应该包含旧索引名");
        assertTrue(sql.contains("`new_index`"), "应该包含新索引名");
    }

    @Test
    void testResetPrimary() {
        MysqlCompareTableInfo compareTableInfo = new MysqlCompareTableInfo("test_table", "");

        // 重置主键
        MysqlColumnMetadata pk1 = new MysqlColumnMetadata();
        pk1.setName("id");
        pk1.setType(new DatabaseTypeAndLength("bigint", 20, null, Collections.emptyList()));
        pk1.setPrimary(true);

        MysqlColumnMetadata pk2 = new MysqlColumnMetadata();
        pk2.setName("tenant_id");
        pk2.setType(new DatabaseTypeAndLength("bigint", 20, null, Collections.emptyList()));
        pk2.setPrimary(true);

        compareTableInfo.resetPrimary(Arrays.asList(pk1, pk2));

        List<String> sqlList = ModifyTableSqlBuilder.buildSql(compareTableInfo);

        assertFalse(sqlList.isEmpty());
        String sql = sqlList.get(0);
        assertTrue(sql.contains("DROP PRIMARY KEY"), "应该包含 DROP PRIMARY KEY");
        assertTrue(sql.contains("ADD PRIMARY KEY"), "应该包含 ADD PRIMARY KEY");
        assertTrue(sql.contains("`id`"), "应该包含新主键列 id");
        assertTrue(sql.contains("`tenant_id`"), "应该包含新主键列 tenant_id");
    }

    @Test
    void testTablePropertiesChange() {
        MysqlCompareTableInfo compareTableInfo = new MysqlCompareTableInfo("test_table", "");
        compareTableInfo.setEngine("MyISAM");
        compareTableInfo.setCharacterSet("utf8mb4");
        compareTableInfo.setCollate("utf8mb4_unicode_ci");
        compareTableInfo.setComment("新注释");

        List<String> sqlList = ModifyTableSqlBuilder.buildSql(compareTableInfo);

        assertFalse(sqlList.isEmpty());
        String sql = sqlList.get(0);
        assertTrue(sql.contains("ENGINE = MyISAM"), "应该包含引擎变更");
        assertTrue(sql.contains("CHARACTER SET = utf8mb4"), "应该包含字符集变更");
        assertTrue(sql.contains("COLLATE = utf8mb4_unicode_ci"), "应该包含排序规则变更");
        assertTrue(sql.contains("COMMENT = '新注释'"), "应该包含注释变更");
    }
}
