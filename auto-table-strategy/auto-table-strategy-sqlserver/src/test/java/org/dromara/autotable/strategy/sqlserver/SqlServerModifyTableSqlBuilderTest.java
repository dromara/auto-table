package org.dromara.autotable.strategy.sqlserver;

import org.dromara.autotable.annotation.enums.IndexSortTypeEnum;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.strategy.sqlserver.builder.ModifyTableSqlBuilder;
import org.dromara.autotable.strategy.sqlserver.data.SqlServerCompareTableInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SqlServer ModifyTableSqlBuilder 单元测试。
 *
 * <p>仅覆盖无注释场景（避免 buildCommentSql 触发 DataSourceManager 查询，那属于集成测试范畴）。
 * 覆盖各改表操作的 SQL 形态：删索引/删主键/删列/重命名/加列/改类型/改非空/改默认值(含约束名drop)/加主键/加索引。</p>
 */
public class SqlServerModifyTableSqlBuilderTest {

    @BeforeEach
    void setUp() {
        IStrategy.setCurrentStrategy(new SqlServerStrategy());
    }

    @AfterEach
    void tearDown() {
        IStrategy.clean();
    }

    private ColumnMetadata column(String name, String type, Integer length, Integer decimalLength, boolean notNull) {
        ColumnMetadata c = new ColumnMetadata();
        c.setName(name);
        c.setType(new DatabaseTypeAndLength(type, length, decimalLength, Collections.emptyList()));
        c.setNotNull(notNull);
        return c;
    }

    private SqlServerCompareTableInfo compareInfo() {
        return new SqlServerCompareTableInfo("user", "dbo");
    }

    @Test
    void test空差异_返回空列表() {
        assertTrue(ModifyTableSqlBuilder.buildSql(compareInfo()).isEmpty());
    }

    @Test
    void test删索引_带ON表名() {
        SqlServerCompareTableInfo ci = compareInfo();
        ci.addDropIndexes(Collections.singleton("idx_name"));
        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        // SQLServer 删索引必须带 ON table
        assertEquals(1, sqls.size());
        assertEquals("DROP INDEX [idx_name] ON [dbo].[user]", sqls.get(0));
    }

    @Test
    void test删主键约束() {
        SqlServerCompareTableInfo ci = compareInfo();
        ci.setDropPrimaryKeyName("PK__user__abc");
        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        assertEquals("ALTER TABLE [dbo].[user] DROP CONSTRAINT [PK__user__abc]", sqls.get(0));
    }

    @Test
    void test删列() {
        SqlServerCompareTableInfo ci = compareInfo();
        ci.addDropColumns(Collections.singleton("old_col"));
        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        assertEquals("ALTER TABLE [dbo].[user] DROP COLUMN [old_col]", sqls.get(0));
    }

    @Test
    void test重命名列_sp_rename() {
        SqlServerCompareTableInfo ci = compareInfo();
        // addRenameColumns 第二参数为前缀，新列名 = 前缀 + 原列名
        ci.addRenameColumns(Collections.singleton("old_col"), "del_");
        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        // EXEC sp_rename N'schema.table.old', N'new', N'COLUMN'
        // 对象名用 schema.table.column 形式，单引号转义
        assertEquals("EXEC sp_rename N'dbo.user.old_col', N'del_old_col', N'COLUMN'", sqls.get(0));
    }

    @Test
    void test加列_含类型和空属性() {
        SqlServerCompareTableInfo ci = compareInfo();
        ci.addNewColumn(column("email", "NVARCHAR", 100, null, false));
        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        assertEquals("ALTER TABLE [dbo].[user] ADD [email] NVARCHAR(100) NULL", sqls.get(0));
    }

    @Test
    void test改列类型_带NOTNULL() {
        SqlServerCompareTableInfo ci = compareInfo();
        ci.addModifyColumn(column("name", "NVARCHAR", 500, null, true), true, false, false, null);
        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        // ALTER COLUMN 必须带上完整类型与 NULL/NOT NULL
        assertEquals("ALTER TABLE [dbo].[user] ALTER COLUMN [name] NVARCHAR(500) NOT NULL", sqls.get(0));
    }

    @Test
    void test仅改非空() {
        SqlServerCompareTableInfo ci = compareInfo();
        ci.addModifyColumn(column("name", "NVARCHAR", 255, null, true), false, true, false, null);
        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        assertEquals("ALTER TABLE [dbo].[user] ALTER COLUMN [name] NVARCHAR(255) NOT NULL", sqls.get(0));
    }

    @Test
    void test改默认值_有旧约束名先drop再add() {
        SqlServerCompareTableInfo ci = compareInfo();
        // 列默认值由 builder 处理，这里 ColumnMetadata 直接带自定义默认值
        ColumnMetadata col = column("status", "INT", null, null, false);
        col.setDefaultValue("1");
        ci.addModifyColumn(col, false, false, true, "DF__user__status__xyz");

        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        // 1. drop 旧默认约束
        assertEquals("ALTER TABLE [dbo].[user] DROP CONSTRAINT [DF__user__status__xyz]", sqls.get(0));
        // 2. add 新默认值（匿名约束）
        assertEquals("ALTER TABLE [dbo].[user] ADD DEFAULT 1 FOR [status]", sqls.get(1));
    }

    @Test
    void test改默认值_无旧约束名直接add() {
        SqlServerCompareTableInfo ci = compareInfo();
        ColumnMetadata col = column("status", "INT", null, null, false);
        col.setDefaultValue("0");
        ci.addModifyColumn(col, false, false, true, null);

        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        assertEquals(1, sqls.size());
        assertEquals("ALTER TABLE [dbo].[user] ADD DEFAULT 0 FOR [status]", sqls.get(0));
    }

    @Test
    void test改默认值_改为无默认值仅drop旧约束() {
        SqlServerCompareTableInfo ci = compareInfo();
        ColumnMetadata col = column("status", "INT", null, null, false);
        // 无新默认值（defaultValueType 为自定义且 defaultValue 空 → resolveDefaultValue 返回 null）
        ci.addModifyColumn(col, false, false, true, "DF__user__status__xyz");

        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        // 仅 drop 旧约束，不 add 新默认值
        assertEquals(1, sqls.size());
        assertEquals("ALTER TABLE [dbo].[user] DROP CONSTRAINT [DF__user__status__xyz]", sqls.get(0));
    }

    @Test
    void test加主键_无名约束() {
        SqlServerCompareTableInfo ci = compareInfo();
        ci.addNewPrimary(Collections.singletonList(column("id", "BIGINT", null, null, true)));
        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        assertEquals("ALTER TABLE [dbo].[user] ADD PRIMARY KEY ([id])", sqls.get(0));
    }

    @Test
    void test加主键_多列() {
        SqlServerCompareTableInfo ci = compareInfo();
        ci.addNewPrimary(java.util.Arrays.asList(
                column("id1", "BIGINT", null, null, true),
                column("id2", "BIGINT", null, null, true)));
        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        assertEquals("ALTER TABLE [dbo].[user] ADD PRIMARY KEY ([id1], [id2])", sqls.get(0));
    }

    @Test
    void test加索引_新建() {
        SqlServerCompareTableInfo ci = compareInfo();
        IndexMetadata idx = new IndexMetadata();
        idx.setName("idx_name");
        idx.setType(IndexTypeEnum.NORMAL);
        idx.setColumns(Collections.singletonList(
                IndexMetadata.IndexColumnParam.newInstance("name", IndexSortTypeEnum.ASC)));
        ci.addNewIndex(idx);
        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        assertEquals("CREATE INDEX [idx_name] ON [dbo].[user] ([name])", sqls.get(0));
    }

    @Test
    void test加索引_修改索引先drop再create() {
        SqlServerCompareTableInfo ci = compareInfo();
        IndexMetadata idx = new IndexMetadata();
        idx.setName("idx_name");
        idx.setType(IndexTypeEnum.UNIQUE);
        idx.setColumns(Collections.singletonList(
                IndexMetadata.IndexColumnParam.newInstance("name", null)));
        ci.addModifyIndex(idx);
        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        // 先 drop 旧索引，再 create 新索引
        assertEquals("DROP INDEX [idx_name] ON [dbo].[user]", sqls.get(0));
        assertEquals("CREATE UNIQUE INDEX [idx_name] ON [dbo].[user] ([name])", sqls.get(1));
    }

    @Test
    void test完整改表_顺序正确() {
        // 验证 SQL 生成顺序：删索引 → 删主键 → 删列 → 重命名 → 加列 → 改列 → 加主键 → 加索引
        SqlServerCompareTableInfo ci = compareInfo();
        ci.addDropIndexes(Collections.singleton("old_idx"));
        ci.setDropPrimaryKeyName("PK_old");
        ci.addDropColumns(Collections.singleton("old_col"));
        ci.addRenameColumns(Collections.singleton("rename_col"), "del_");
        ci.addNewColumn(column("new_col", "NVARCHAR", 50, null, false));
        ci.addNewPrimary(Collections.singletonList(column("id", "BIGINT", null, null, true)));

        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        assertEquals("DROP INDEX [old_idx] ON [dbo].[user]", sqls.get(0));
        assertEquals("ALTER TABLE [dbo].[user] DROP CONSTRAINT [PK_old]", sqls.get(1));
        assertEquals("ALTER TABLE [dbo].[user] DROP COLUMN [old_col]", sqls.get(2));
        assertEquals("EXEC sp_rename N'dbo.user.rename_col', N'del_rename_col', N'COLUMN'", sqls.get(3));
        assertEquals("ALTER TABLE [dbo].[user] ADD [new_col] NVARCHAR(50) NULL", sqls.get(4));
        assertEquals("ALTER TABLE [dbo].[user] ADD PRIMARY KEY ([id])", sqls.get(5));
    }

    @Test
    void test每条SQL独立单语句() {
        // SQLServer JDBC 单次 execute 只执行一条语句，验证每条 SQL 不含多个分号语句
        SqlServerCompareTableInfo ci = compareInfo();
        ci.addDropColumns(Collections.singleton("c1"));
        ci.addNewColumn(column("c2", "NVARCHAR", 50, null, false));
        ci.addNewPrimary(Collections.singletonList(column("id", "BIGINT", null, null, true)));

        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        for (String sql : sqls) {
            long semiCount = sql.chars().filter(c -> c == ';').count();
            assertTrue(semiCount <= 1, "每条 SQL 应为独立单语句: " + sql);
        }
    }
}
