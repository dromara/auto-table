package org.dromara.autotable.strategy.sqlserver;

import org.dromara.autotable.annotation.enums.IndexSortTypeEnum;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.strategy.sqlserver.builder.CreateTableSqlBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SqlServer CreateTableSqlBuilder 单元测试
 */
public class SqlServerCreateTableSqlBuilderTest {

    @BeforeEach
    void setUp() {
        IStrategy.setCurrentStrategy(new SqlServerStrategy());
    }

    @AfterEach
    void tearDown() {
        IStrategy.clean();
    }

    private DefaultTableMetadata buildUserTable() {
        DefaultTableMetadata metadata = new DefaultTableMetadata(null, "user", "dbo", "用户表");

        ColumnMetadata idColumn = new ColumnMetadata();
        idColumn.setName("id");
        idColumn.setType(new DatabaseTypeAndLength("BIGINT", null, null, Collections.emptyList()));
        idColumn.setPrimary(true);
        idColumn.setAutoIncrement(true);
        idColumn.setNotNull(true);

        ColumnMetadata nameColumn = new ColumnMetadata();
        nameColumn.setName("name");
        nameColumn.setType(new DatabaseTypeAndLength("NVARCHAR", 255, null, Collections.emptyList()));
        nameColumn.setComment("姓名");

        metadata.setColumnMetadataList(Arrays.asList(idColumn, nameColumn));
        metadata.setIndexMetadataList(Collections.emptyList());
        return metadata;
    }

    @Test
    void testBuildSql_包含CREATE_TABLE和IDENTITY() {
        List<String> sqls = CreateTableSqlBuilder.buildSql(buildUserTable());

        assertNotNull(sqls);
        assertFalse(sqls.isEmpty());

        // 第一条是 CREATE TABLE
        String createTableSql = sqls.get(0);
        assertTrue(createTableSql.contains("CREATE TABLE [dbo].[user]"), "应包含建表语句: " + createTableSql);
        // 自增主键 IDENTITY(1,1)
        assertTrue(createTableSql.contains("[id] BIGINT IDENTITY(1,1) NOT NULL"), "应包含 IDENTITY 自增: " + createTableSql);
        // 主键约束
        assertTrue(createTableSql.contains("PRIMARY KEY ([id])"), "应包含主键: " + createTableSql);
        // 普通列默认 NULL
        assertTrue(createTableSql.contains("[name] NVARCHAR(255) NULL"), "普通列应为 NULL: " + createTableSql);
    }

    @Test
    void testBuildSql_注释用sp_addextendedproperty() {
        List<String> sqls = CreateTableSqlBuilder.buildSql(buildUserTable());

        // 表注释
        String tableCommentSql = sqls.stream()
                .filter(sql -> sql.contains("sp_addextendedproperty") && sql.contains("@level1name=N'user'") && !sql.contains("@level2"))
                .findFirst().orElse(null);
        assertNotNull(tableCommentSql, "应包含表注释");
        assertTrue(tableCommentSql.contains("N'用户表'"), "表注释内容应正确: " + tableCommentSql);

        // 列注释
        String columnCommentSql = sqls.stream()
                .filter(sql -> sql.contains("@level2type=N'COLUMN'") && sql.contains("@level2name=N'name'"))
                .findFirst().orElse(null);
        assertNotNull(columnCommentSql, "应包含列注释");
        assertTrue(columnCommentSql.contains("N'姓名'"), "列注释内容应正确: " + columnCommentSql);
    }

    @Test
    void testBuildSql_注释单引号转义() {
        DefaultTableMetadata metadata = new DefaultTableMetadata(null, "test", "dbo", "用户's 表");
        ColumnMetadata col = new ColumnMetadata();
        col.setName("name");
        col.setType(new DatabaseTypeAndLength("NVARCHAR", 255, null, Collections.emptyList()));
        col.setComment("用户's name");
        metadata.setColumnMetadataList(Collections.singletonList(col));
        metadata.setIndexMetadataList(Collections.emptyList());

        List<String> sqls = CreateTableSqlBuilder.buildSql(metadata);

        String tableComment = sqls.stream()
                .filter(sql -> sql.contains("sp_addextendedproperty") && !sql.contains("@level2"))
                .findFirst().orElse(null);
        assertNotNull(tableComment);
        // 单引号应转义为两个单引号
        assertTrue(tableComment.contains("N'用户''s 表'"), "表注释单引号应转义: " + tableComment);
    }

    @Test
    void testGetCreateIndexSql_唯一索引DESC() {
        IndexMetadata index = new IndexMetadata();
        index.setName("uk_phone");
        index.setType(IndexTypeEnum.UNIQUE);
        index.setColumns(Collections.singletonList(
                IndexMetadata.IndexColumnParam.newInstance("phone", IndexSortTypeEnum.DESC)));

        String sql = CreateTableSqlBuilder.getCreateIndexSql("dbo", "user", index);
        assertEquals("CREATE UNIQUE INDEX [uk_phone] ON [dbo].[user] ([phone] DESC)", sql);
    }

    @Test
    void testGetCreateIndexSql_普通索引ASC省略() {
        IndexMetadata index = new IndexMetadata();
        index.setName("idx_name");
        index.setType(IndexTypeEnum.NORMAL);
        index.setColumns(Collections.singletonList(
                IndexMetadata.IndexColumnParam.newInstance("name", IndexSortTypeEnum.ASC)));

        String sql = CreateTableSqlBuilder.getCreateIndexSql("dbo", "user", index);
        // ASC 为默认，应省略
        assertEquals("CREATE INDEX [idx_name] ON [dbo].[user] ([name])", sql);
    }

    @Test
    void testGetCreateTableSql_DECIMAL带精度() {
        DefaultTableMetadata metadata = new DefaultTableMetadata(null, "test", "dbo", null);
        ColumnMetadata col = new ColumnMetadata();
        col.setName("amount");
        col.setType(new DatabaseTypeAndLength("DECIMAL", 19, 4, Collections.emptyList()));
        col.setNotNull(true);
        metadata.setColumnMetadataList(Collections.singletonList(col));
        metadata.setIndexMetadataList(Collections.emptyList());

        List<String> sqls = CreateTableSqlBuilder.buildSql(metadata);
        assertTrue(sqls.get(0).contains("[amount] DECIMAL(19,4) NOT NULL"), sqls.get(0));
    }

    @Test
    void testBuildSql_多语句分离() {
        // SQLServer JDBC 单次 execute 只执行一条语句，验证每条 SQL 独立
        List<String> sqls = CreateTableSqlBuilder.buildSql(buildUserTable());
        // 每条 SQL 不应含多个分号分隔的语句（仅可能末尾无分号，wrapSql 会补）
        for (String sql : sqls) {
            long semiCount = sql.chars().filter(c -> c == ';').count();
            assertTrue(semiCount <= 1, "每条 SQL 应为独立单语句，发现多个分号: " + sql);
        }
    }
}
