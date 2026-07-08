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
 * <p>buildCommentSql 复用 compare 阶段的 dbExists 标志，不再触发 DataSourceManager 查询，故注释场景也可单元测试。
 * 覆盖各改表操作的 SQL 形态：删索引/删主键/删列/重命名/加列/改类型/改非空/改默认值(含约束名drop)/加主键/加索引/注释add与update。</p>
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
        // EXEC sp_rename N'[schema].[table].[old]', N'new', N'COLUMN'
        // 对象名用 [schema].[table].[column] 方括号形式，单引号转义
        assertEquals("EXEC sp_rename N'[dbo].[user].[old_col]', N'del_old_col', N'COLUMN'", sqls.get(0));
    }

    @Test
    void test重命名列_schema为null时省略schema前缀() {
        // schema 未配置时为 null（getTableSchema 默认实现返回 null），sp_rename 省略 schema 前缀走默认 schema
        SqlServerCompareTableInfo ci = new SqlServerCompareTableInfo("user", null);
        ci.addRenameColumns(Collections.singleton("old_col"), "del_");
        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        assertEquals("EXEC sp_rename N'[user].[old_col]', N'del_old_col', N'COLUMN'", sqls.get(0));
    }

    @Test
    void test重命名列_schema为空串时省略schema前缀() {
        // schema 为空串时同样省略 schema 前缀（与 null 行为一致，均由 hasText 过滤）
        SqlServerCompareTableInfo ci = new SqlServerCompareTableInfo("user", "");
        ci.addRenameColumns(Collections.singleton("old_col"), "del_");
        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        assertEquals("EXEC sp_rename N'[user].[old_col]', N'del_old_col', N'COLUMN'", sqls.get(0));
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
        assertEquals("EXEC sp_rename N'[dbo].[user].[rename_col]', N'del_rename_col', N'COLUMN'", sqls.get(3));
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

    @Test
    void test改类型与默认值_先drop约束再ALTER再add默认() {
        // 同时改类型与默认值且列有旧默认约束时，顺序应为：drop 旧约束 → ALTER COLUMN → add 新默认值
        // （列存在默认约束时直接 ALTER COLUMN 可能失败，故先解除约束）
        SqlServerCompareTableInfo ci = compareInfo();
        ColumnMetadata col = column("status", "INT", null, null, false);
        col.setDefaultValue("1");
        ci.addModifyColumn(col, true, false, true, "DF__user__status__xyz");

        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        assertEquals("ALTER TABLE [dbo].[user] DROP CONSTRAINT [DF__user__status__xyz]", sqls.get(0));
        assertEquals("ALTER TABLE [dbo].[user] ALTER COLUMN [status] INT NULL", sqls.get(1));
        assertEquals("ALTER TABLE [dbo].[user] ADD DEFAULT 1 FOR [status]", sqls.get(2));
    }

    @Test
    void test注释_按dbExists区分add与update() {
        // buildCommentSql 复用 compare 阶段的 dbExists 标志，不触发 DataSourceManager 查询
        SqlServerCompareTableInfo ci = compareInfo();
        // 表注释：DB 已存在 → sp_updateextendedproperty（无 level2）
        ci.setComment("用户表");
        ci.setTableCommentExists(true);
        // 列注释：DB 无 → sp_addextendedproperty
        ci.addColumnComment("name", "姓名", false);
        // 列注释：DB 已存在 → sp_updateextendedproperty
        ci.addColumnComment("age", "年龄", true);

        List<String> sqls = ModifyTableSqlBuilder.buildSql(ci);
        // 表注释 update：含 sp_updateextendedproperty 且 level1name=user、无 level2type
        assertTrue(sqls.stream().anyMatch(s -> s.contains("sp_updateextendedproperty")
                && s.contains("@level1name=N'user'") && !s.contains("level2type")), "表注释应 update");
        // name 列注释 add
        assertTrue(sqls.stream().anyMatch(s -> s.contains("sp_addextendedproperty")
                && s.contains("@level2name=N'name'")), "name 列注释应 add");
        // age 列注释 update
        assertTrue(sqls.stream().anyMatch(s -> s.contains("sp_updateextendedproperty")
                && s.contains("@level2name=N'age'")), "age 列注释应 update");
    }
}
