package org.dromara.autotable.test.core.unit.builder;

import org.dromara.autotable.annotation.enums.IndexSortTypeEnum;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.strategy.pgsql.PgsqlStrategy;
import org.dromara.autotable.strategy.pgsql.builder.CreateTableSqlBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PgSQL CreateTableSqlBuilder 单元测试
 */
public class PgsqlCreateTableSqlBuilderTest {

    @BeforeEach
    void setUp() {
        IStrategy.setCurrentStrategy(new PgsqlStrategy());
    }

    @AfterEach
    void tearDown() {
        IStrategy.clean();
    }

    private DefaultTableMetadata makeTableMetadata(String tableName) {
        DefaultTableMetadata metadata = new DefaultTableMetadata(null, tableName, "public", "");
        metadata.setColumnMetadataList(new ArrayList<>());
        metadata.setIndexMetadataList(new ArrayList<>());
        return metadata;
    }

    private ColumnMetadata makeColumn(String name, String type, Integer length) {
        ColumnMetadata column = new ColumnMetadata();
        column.setName(name);
        column.setType(new DatabaseTypeAndLength(type, length, null, Collections.emptyList()));
        return column;
    }

    @Test
    void testBuildSql_withSimpleTable() {
        DefaultTableMetadata metadata = makeTableMetadata("test_user");
        metadata.setComment("用户表");

        ColumnMetadata idCol = makeColumn("id", "int8", null);
        idCol.setPrimary(true);
        idCol.setNotNull(true);
        idCol.setComment("主键");

        ColumnMetadata nameCol = makeColumn("name", "varchar", 100);
        nameCol.setNotNull(true);
        nameCol.setComment("用户名");

        metadata.setColumnMetadataList(Arrays.asList(idCol, nameCol));

        String sql = CreateTableSqlBuilder.buildSql(metadata);

        // CREATE TABLE
        assertTrue(sql.contains("CREATE TABLE \"public\".\"test_user\""), "应包含 schema 限定的表名");
        assertTrue(sql.contains("\"id\""), "应包含 id 列");
        assertTrue(sql.contains("\"name\""), "应包含 name 列");
        assertTrue(sql.contains("PRIMARY KEY"), "应包含主键");

        // COMMENT ON
        assertTrue(sql.contains("COMMENT ON TABLE \"public\".\"test_user\" IS '用户表'"), "应包含表注释");
        assertTrue(sql.contains("COMMENT ON COLUMN"), "应包含列注释");
    }

    @Test
    void testBuildSql_withIndex() {
        DefaultTableMetadata metadata = makeTableMetadata("test_index");

        ColumnMetadata idCol = makeColumn("id", "int8", null);
        idCol.setPrimary(true);
        metadata.setColumnMetadataList(Arrays.asList(idCol));

        IndexMetadata indexMetadata = new IndexMetadata();
        indexMetadata.setName("idx_name");
        indexMetadata.setType(IndexTypeEnum.NORMAL);
        indexMetadata.setColumns(Arrays.asList(
                IndexMetadata.IndexColumnParam.newInstance("name", null)
        ));
        metadata.setIndexMetadataList(Arrays.asList(indexMetadata));

        String sql = CreateTableSqlBuilder.buildSql(metadata);

        assertTrue(sql.contains("CREATE INDEX \"idx_name\""), "应包含索引名");
        assertTrue(sql.contains("USING btree"), "默认应使用 btree");
        assertTrue(sql.contains("\"public\".\"test_index\""), "应包含 schema 限定的表名");
    }

    @Test
    void testBuildSql_withUniqueIndex() {
        DefaultTableMetadata metadata = makeTableMetadata("test_unique");
        metadata.setColumnMetadataList(new ArrayList<>());

        IndexMetadata indexMetadata = new IndexMetadata();
        indexMetadata.setName("uk_email");
        indexMetadata.setType(IndexTypeEnum.UNIQUE);
        indexMetadata.setColumns(Arrays.asList(
                IndexMetadata.IndexColumnParam.newInstance("email", null)
        ));
        metadata.setIndexMetadataList(Arrays.asList(indexMetadata));

        String sql = CreateTableSqlBuilder.buildSql(metadata);

        assertTrue(sql.contains("CREATE UNIQUE INDEX \"uk_email\""), "应包含 UNIQUE 关键字");
    }

    @Test
    void testBuildSql_withIndexSortDesc() {
        DefaultTableMetadata metadata = makeTableMetadata("test_sort");
        metadata.setColumnMetadataList(new ArrayList<>());

        IndexMetadata indexMetadata = new IndexMetadata();
        indexMetadata.setName("idx_sort");
        indexMetadata.setType(IndexTypeEnum.NORMAL);
        indexMetadata.setColumns(Arrays.asList(
                IndexMetadata.IndexColumnParam.newInstance("col1", IndexSortTypeEnum.DESC)
        ));
        metadata.setIndexMetadataList(Arrays.asList(indexMetadata));

        String sql = CreateTableSqlBuilder.buildSql(metadata);

        assertTrue(sql.contains("DESC"), "应包含 DESC 排序");
    }

    @Test
    void testBuildSql_withCustomIndexMethod() {
        DefaultTableMetadata metadata = makeTableMetadata("test_method");
        metadata.setColumnMetadataList(new ArrayList<>());

        IndexMetadata indexMetadata = new IndexMetadata();
        indexMetadata.setName("idx_gin");
        indexMetadata.setType(IndexTypeEnum.NORMAL);
        indexMetadata.setMethod("gin");
        indexMetadata.setColumns(Arrays.asList(
                IndexMetadata.IndexColumnParam.newInstance("data", null)
        ));
        metadata.setIndexMetadataList(Arrays.asList(indexMetadata));

        String sql = CreateTableSqlBuilder.buildSql(metadata);

        assertTrue(sql.contains("USING gin"), "应使用自定义索引方法");
    }

    @Test
    void testGetAddColumnCommentSql_commentWithSingleQuote() {
        Map<String, String> columnComments = new HashMap<>();
        columnComments.put("name", "用户's name");

        Map<String, String> indexComments = new HashMap<>();
        indexComments.put("idx_test", "索引's comment");

        String sql = CreateTableSqlBuilder.getAddColumnCommentSql("public", "test_table",
                "表's comment", columnComments, indexComments);

        // 单引号应被转义为 ''
        assertTrue(sql.contains("表''s comment"), "表注释单引号应转义");
        assertTrue(sql.contains("用户''s name"), "列注释单引号应转义");
        assertTrue(sql.contains("索引''s comment"), "索引注释单引号应转义");
    }

    @Test
    void testGetAddColumnCommentSql_withNullComment() {
        Map<String, String> columnComments = new HashMap<>();
        columnComments.put("name", null);

        Map<String, String> indexComments = new HashMap<>();

        String sql = CreateTableSqlBuilder.getAddColumnCommentSql("public", "test_table",
                null, columnComments, indexComments);

        // 表注释为 null，不应生成 COMMENT ON TABLE
        assertFalse(sql.contains("COMMENT ON TABLE"), "null 表注释不应生成 SQL");
        // 列注释为 null，仍会生成 COMMENT ON COLUMN（调用方负责过滤）
        assertTrue(sql.contains("COMMENT ON COLUMN"), "null 列注释仍应生成 COMMENT ON COLUMN（调用方负责过滤）");
    }

    @Test
    void testGetPrimaryKeySql() {
        DefaultTableMetadata metadata = makeTableMetadata("test_pk");

        ColumnMetadata idCol = makeColumn("id", "int8", null);
        idCol.setPrimary(true);
        ColumnMetadata tenantCol = makeColumn("tenant_id", "int8", null);
        tenantCol.setPrimary(true);

        metadata.setColumnMetadataList(Arrays.asList(idCol, tenantCol));
        metadata.setIndexMetadataList(new ArrayList<>());

        String sql = CreateTableSqlBuilder.buildSql(metadata);

        assertTrue(sql.contains("PRIMARY KEY (\"id\",\"tenant_id\")"), "应包含复合主键");
    }
}
