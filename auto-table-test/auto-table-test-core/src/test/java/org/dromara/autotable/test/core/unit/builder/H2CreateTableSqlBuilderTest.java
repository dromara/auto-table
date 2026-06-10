package org.dromara.autotable.test.core.unit.builder;

import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.strategy.h2.H2Strategy;
import org.dromara.autotable.strategy.h2.builder.CreateTableSqlBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * H2 CreateTableSqlBuilder 单元测试
 */
public class H2CreateTableSqlBuilderTest {

    @BeforeEach
    void setUp() {
        IStrategy.setCurrentStrategy(new H2Strategy());
    }

    @AfterEach
    void tearDown() {
        IStrategy.clean();
    }

    @Test
    void testBuildTableSql_simpleTable() {
        DefaultTableMetadata metadata = new DefaultTableMetadata(null, "test_table", "PUBLIC", "测试表");

        ColumnMetadata idColumn = new ColumnMetadata();
        idColumn.setName("id");
        idColumn.setType(new DatabaseTypeAndLength("BIGINT", null, null, Collections.emptyList()));
        idColumn.setPrimary(true);
        idColumn.setAutoIncrement(true);

        ColumnMetadata nameColumn = new ColumnMetadata();
        nameColumn.setName("name");
        nameColumn.setType(new DatabaseTypeAndLength("VARCHAR", 100, null, Collections.emptyList()));
        nameColumn.setNotNull(false);

        metadata.setColumnMetadataList(Arrays.asList(idColumn, nameColumn));
        metadata.setIndexMetadataList(Collections.emptyList());

        List<String> sqls = CreateTableSqlBuilder.buildTableSql(metadata);

        assertNotNull(sqls);
        assertFalse(sqls.isEmpty());

        // 当有 schema 时，前两个 SQL 是 CREATE SCHEMA 和 SET SCHEMA，第三个才是 CREATE TABLE
        String createTableSql = sqls.stream()
                .filter(sql -> sql.contains("CREATE TABLE"))
                .findFirst()
                .orElse(null);

        assertNotNull(createTableSql, "应该包含 CREATE TABLE 语句");
        assertTrue(createTableSql.contains("\"PUBLIC\".\"test_table\""));
        assertTrue(createTableSql.contains("\"id\""));
        assertTrue(createTableSql.contains("\"name\""));
    }

    @Test
    void testGetAllCommentSql_withSingleQuote() {
        String schema = "PUBLIC";
        String tableName = "test_table";
        String tableComment = "用户's 表"; // 包含单引号

        java.util.Map<String, String> columnComments = new java.util.HashMap<>();
        columnComments.put("name", "用户's name"); // 包含单引号
        columnComments.put("email", "邮箱地址");

        java.util.Map<String, String> indexComments = new java.util.HashMap<>();
        indexComments.put("idx_name", "用户's index"); // 包含单引号

        List<String> commentSqls = CreateTableSqlBuilder.getAllCommentSql(
                schema, tableName, tableComment, columnComments, indexComments);

        assertNotNull(commentSqls);
        assertEquals(4, commentSqls.size()); // 1 table + 2 columns + 1 index

        // 验证表注释单引号被转义
        String tableCommentSql = commentSqls.get(0);
        assertTrue(tableCommentSql.contains("COMMENT ON TABLE"));
        assertTrue(tableCommentSql.contains("\"PUBLIC\".\"test_table\""));
        assertTrue(tableCommentSql.contains("'用户''s 表'")); // 单引号被转义为两个单引号
        assertFalse(tableCommentSql.contains("'用户's 表'")); // 不能有未转义的单引号

        // 验证列注释单引号被转义
        String nameCommentSql = commentSqls.get(1);
        assertTrue(nameCommentSql.contains("COMMENT ON COLUMN"));
        assertTrue(nameCommentSql.contains("'用户''s name'")); // 单引号被转义

        // 验证索引注释单引号被转义
        String indexCommentSql = commentSqls.get(3);
        assertTrue(indexCommentSql.contains("COMMENT ON INDEX"));
        assertTrue(indexCommentSql.contains("'用户''s index'")); // 单引号被转义
    }

    @Test
    void testGetAllCommentSql_withNullComment() {
        String schema = "PUBLIC";
        String tableName = "test_table";
        String tableComment = null;

        java.util.Map<String, String> columnComments = new java.util.HashMap<>();
        columnComments.put("name", null);

        java.util.Map<String, String> indexComments = new java.util.HashMap<>();
        indexComments.put("idx_name", null);

        List<String> commentSqls = CreateTableSqlBuilder.getAllCommentSql(
                schema, tableName, tableComment, columnComments, indexComments);

        assertNotNull(commentSqls);
        // 当 tableComment 为 null 时，不生成表注释 SQL
        // 列和索引注释为 null 时，生成 IS NULL 的 SQL
        assertEquals(2, commentSqls.size()); // 1 column + 1 index (table comment is null, not generated)

        // 验证列注释为 NULL
        String nameCommentSql = commentSqls.get(0);
        assertTrue(nameCommentSql.contains("COMMENT ON COLUMN"));
        assertTrue(nameCommentSql.contains("IS null"));

        // 验证索引注释为 NULL
        String indexCommentSql = commentSqls.get(1);
        assertTrue(indexCommentSql.contains("COMMENT ON INDEX"));
        assertTrue(indexCommentSql.contains("IS null"));
    }

    @Test
    void testGetAllCommentSql_withEmptyComment() {
        String schema = "PUBLIC";
        String tableName = "test_table";
        String tableComment = "";

        java.util.Map<String, String> columnComments = new java.util.HashMap<>();
        columnComments.put("name", "");

        java.util.Map<String, String> indexComments = new java.util.HashMap<>();
        indexComments.put("idx_name", "");

        List<String> commentSqls = CreateTableSqlBuilder.getAllCommentSql(
                schema, tableName, tableComment, columnComments, indexComments);

        assertNotNull(commentSqls);
        // 当 tableComment 为空字符串时，不生成表注释 SQL
        assertEquals(2, commentSqls.size()); // 1 column + 1 index (table comment is empty, not generated)

        // 验证列注释为 NULL（空字符串被视为 null）
        String nameCommentSql = commentSqls.get(0);
        assertTrue(nameCommentSql.contains("COMMENT ON COLUMN"));
        assertTrue(nameCommentSql.contains("IS null"));

        // 验证索引注释为 NULL（空字符串被视为 null）
        String indexCommentSql = commentSqls.get(1);
        assertTrue(indexCommentSql.contains("COMMENT ON INDEX"));
        assertTrue(indexCommentSql.contains("IS null"));
    }

    @Test
    void testGetAllCommentSql_withNormalComment() {
        String schema = "PUBLIC";
        String tableName = "test_table";
        String tableComment = "测试表";

        // 使用 LinkedHashMap 保证顺序
        java.util.Map<String, String> columnComments = new java.util.LinkedHashMap<>();
        columnComments.put("id", "主键ID");
        columnComments.put("name", "用户名称");

        java.util.Map<String, String> indexComments = new java.util.LinkedHashMap<>();
        indexComments.put("idx_name", "名称索引");

        List<String> commentSqls = CreateTableSqlBuilder.getAllCommentSql(
                schema, tableName, tableComment, columnComments, indexComments);

        assertNotNull(commentSqls);
        assertEquals(4, commentSqls.size()); // 1 table + 2 columns + 1 index

        // 验证表注释
        String tableCommentSql = commentSqls.get(0);
        assertTrue(tableCommentSql.contains("'测试表'"));

        // 验证列注释（按插入顺序）
        String idCommentSql = commentSqls.get(1);
        assertTrue(idCommentSql.contains("'主键ID'"));

        String nameCommentSql = commentSqls.get(2);
        assertTrue(nameCommentSql.contains("'用户名称'"));

        // 验证索引注释
        String indexCommentSql = commentSqls.get(3);
        assertTrue(indexCommentSql.contains("'名称索引'"));
    }

    @Test
    void testBuildTableSql_withIndex() {
        DefaultTableMetadata metadata = new DefaultTableMetadata(null, "test_table", "PUBLIC", null);

        ColumnMetadata idColumn = new ColumnMetadata();
        idColumn.setName("id");
        idColumn.setType(new DatabaseTypeAndLength("BIGINT", null, null, Collections.emptyList()));
        idColumn.setPrimary(true);

        ColumnMetadata nameColumn = new ColumnMetadata();
        nameColumn.setName("name");
        nameColumn.setType(new DatabaseTypeAndLength("VARCHAR", 100, null, Collections.emptyList()));

        metadata.setColumnMetadataList(Arrays.asList(idColumn, nameColumn));

        IndexMetadata indexMetadata = new IndexMetadata();
        indexMetadata.setName("idx_name");
        indexMetadata.setType(IndexTypeEnum.NORMAL); // 设置索引类型
        indexMetadata.setColumns(Collections.singletonList(IndexMetadata.IndexColumnParam.newInstance("name", null)));
        metadata.setIndexMetadataList(Collections.singletonList(indexMetadata));

        List<String> sqls = CreateTableSqlBuilder.buildTableSql(metadata);

        assertNotNull(sqls);
        assertTrue(sqls.size() >= 4); // CREATE SCHEMA + SET SCHEMA + CREATE TABLE + CREATE INDEX

        // 查找 CREATE INDEX 语句
        String createIndexSql = sqls.stream()
                .filter(sql -> sql.contains("INDEX") && sql.contains("\"idx_name\""))
                .findFirst()
                .orElse(null);

        assertNotNull(createIndexSql, "应该包含 CREATE INDEX 语句");
        assertTrue(createIndexSql.contains("\"idx_name\""));
        assertTrue(createIndexSql.contains("\"name\""));
    }
}
