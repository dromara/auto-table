package org.dromara.autotable.test.core.unit.builder;

import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.strategy.pgsql.PgsqlStrategy;
import org.dromara.autotable.strategy.pgsql.builder.ModifyTableSqlBuilder;
import org.dromara.autotable.strategy.pgsql.data.PgsqlCompareTableInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PgSQL ModifyTableSqlBuilder 单元测试
 */
public class PgsqlModifyTableSqlBuilderTest {

    @BeforeEach
    void setUp() {
        IStrategy.setCurrentStrategy(new PgsqlStrategy());
    }

    @AfterEach
    void tearDown() {
        IStrategy.clean();
    }

    private ColumnMetadata makeColumn(String name, String type, Integer length) {
        ColumnMetadata column = new ColumnMetadata();
        column.setName(name);
        column.setType(new DatabaseTypeAndLength(type, length, null, Collections.emptyList()));
        return column;
    }

    @Test
    void testModifyColumn_onlyTypeChanged() {
        PgsqlCompareTableInfo compareInfo = new PgsqlCompareTableInfo("test_table", "public");

        ColumnMetadata column = makeColumn("name", "varchar", 500);
        column.setNotNull(false);
        compareInfo.addModifyColumn(column, true, false, false);

        String sql = ModifyTableSqlBuilder.buildSql(compareInfo);

        assertTrue(sql.contains("ALTER COLUMN \"name\" TYPE varchar(500)"), "应包含 TYPE 变更");
        assertTrue(sql.contains("USING \"name\"::varchar(500)"), "应包含 USING 类型转换");
        assertFalse(sql.contains("SET NOT NULL"), "不应包含 NOT NULL 变更");
        assertFalse(sql.contains("DROP NOT NULL"), "不应包含 NOT NULL 变更");
        assertFalse(sql.contains("SET DEFAULT"), "不应包含 DEFAULT 变更");
        assertFalse(sql.contains("DROP DEFAULT"), "不应包含 DEFAULT 变更");
    }

    @Test
    void testModifyColumn_onlyNotNullChanged() {
        PgsqlCompareTableInfo compareInfo = new PgsqlCompareTableInfo("test_table", "public");

        ColumnMetadata column = makeColumn("name", "varchar", 100);
        column.setNotNull(true);
        compareInfo.addModifyColumn(column, false, true, false);

        String sql = ModifyTableSqlBuilder.buildSql(compareInfo);

        assertTrue(sql.contains("ALTER COLUMN \"name\" SET NOT NULL"), "应包含 SET NOT NULL");
        assertFalse(sql.contains("ALTER COLUMN \"name\" TYPE"), "不应包含 TYPE 变更");
        assertFalse(sql.contains("SET DEFAULT"), "不应包含 DEFAULT 变更");
    }

    @Test
    void testModifyColumn_dropNotNull() {
        PgsqlCompareTableInfo compareInfo = new PgsqlCompareTableInfo("test_table", "public");

        ColumnMetadata column = makeColumn("name", "varchar", 100);
        column.setNotNull(false);
        compareInfo.addModifyColumn(column, false, true, false);

        String sql = ModifyTableSqlBuilder.buildSql(compareInfo);

        assertTrue(sql.contains("ALTER COLUMN \"name\" DROP NOT NULL"), "应包含 DROP NOT NULL");
    }

    @Test
    void testModifyColumn_onlyDefaultChanged() {
        PgsqlCompareTableInfo compareInfo = new PgsqlCompareTableInfo("test_table", "public");

        ColumnMetadata column = makeColumn("status", "varchar", 20);
        column.setDefaultValue("'active'");
        compareInfo.addModifyColumn(column, false, false, true);

        String sql = ModifyTableSqlBuilder.buildSql(compareInfo);

        assertTrue(sql.contains("ALTER COLUMN \"status\" SET DEFAULT 'active'"), "应包含 SET DEFAULT");
        assertFalse(sql.contains("TYPE"), "不应包含 TYPE 变更");
        assertFalse(sql.contains("NOT NULL"), "不应包含 NOT NULL 变更");
    }

    @Test
    void testModifyColumn_dropDefault() {
        PgsqlCompareTableInfo compareInfo = new PgsqlCompareTableInfo("test_table", "public");

        ColumnMetadata column = makeColumn("status", "varchar", 20);
        // defaultValue 为 null，表示删除默认值
        compareInfo.addModifyColumn(column, false, false, true);

        String sql = ModifyTableSqlBuilder.buildSql(compareInfo);

        assertTrue(sql.contains("ALTER COLUMN \"status\" DROP DEFAULT"), "应包含 DROP DEFAULT");
    }

    @Test
    void testModifyColumn_allChanged() {
        PgsqlCompareTableInfo compareInfo = new PgsqlCompareTableInfo("test_table", "public");

        ColumnMetadata column = makeColumn("age", "int8", null);
        column.setNotNull(true);
        column.setDefaultValue("0");
        compareInfo.addModifyColumn(column, true, true, true);

        String sql = ModifyTableSqlBuilder.buildSql(compareInfo);

        assertTrue(sql.contains("ALTER COLUMN \"age\" TYPE int8"), "应包含 TYPE 变更");
        assertTrue(sql.contains("ALTER COLUMN \"age\" SET NOT NULL"), "应包含 NOT NULL 变更");
        assertTrue(sql.contains("ALTER COLUMN \"age\" SET DEFAULT 0"), "应包含 DEFAULT 变更");
    }

    @Test
    void testAddColumn() {
        PgsqlCompareTableInfo compareInfo = new PgsqlCompareTableInfo("test_table", "public");

        ColumnMetadata newCol = makeColumn("email", "varchar", 255);
        newCol.setNotNull(true);
        compareInfo.addNewColumn(newCol);

        String sql = ModifyTableSqlBuilder.buildSql(compareInfo);

        assertTrue(sql.contains("ADD COLUMN \"email\""), "应包含 ADD COLUMN");
        assertTrue(sql.contains("NOT NULL"), "应包含 NOT NULL");
    }

    @Test
    void testDropColumn() {
        PgsqlCompareTableInfo compareInfo = new PgsqlCompareTableInfo("test_table", "public");
        compareInfo.getDropColumnList().add("obsolete_col");

        String sql = ModifyTableSqlBuilder.buildSql(compareInfo);

        assertTrue(sql.contains("DROP COLUMN \"obsolete_col\""), "应包含 DROP COLUMN");
    }

    @Test
    void testRenameColumn() {
        PgsqlCompareTableInfo compareInfo = new PgsqlCompareTableInfo("test_table", "public");
        compareInfo.getRenameColumnMap().put("old_name", "_del_old_name");

        String sql = ModifyTableSqlBuilder.buildSql(compareInfo);

        assertTrue(sql.contains("RENAME COLUMN \"old_name\" TO \"_del_old_name\""), "应包含 RENAME COLUMN");
    }

    @Test
    void testDropAndAddIndex() {
        PgsqlCompareTableInfo compareInfo = new PgsqlCompareTableInfo("test_table", "public");
        compareInfo.getDropIndexList().add("old_index");

        IndexMetadata newIndex = new IndexMetadata();
        newIndex.setName("new_index");
        newIndex.setType(IndexTypeEnum.NORMAL);
        newIndex.setColumns(Arrays.asList(
                IndexMetadata.IndexColumnParam.newInstance("col1", null)
        ));
        compareInfo.getIndexMetadataList().add(newIndex);

        String sql = ModifyTableSqlBuilder.buildSql(compareInfo);

        assertTrue(sql.contains("DROP INDEX \"public\".\"old_index\""), "应包含 DROP INDEX");
        assertTrue(sql.contains("CREATE INDEX \"new_index\""), "应包含 CREATE INDEX");
    }

    @Test
    void testResetPrimary() {
        PgsqlCompareTableInfo compareInfo = new PgsqlCompareTableInfo("test_table", "public");

        // 先删除旧主键
        compareInfo.setDropPrimaryKeyName("test_table_pkey");

        // 再添加新主键
        ColumnMetadata pk1 = makeColumn("id", "int8", null);
        pk1.setPrimary(true);
        ColumnMetadata pk2 = makeColumn("tenant_id", "int8", null);
        pk2.setPrimary(true);
        compareInfo.addNewPrimary(Arrays.asList(pk1, pk2));

        String sql = ModifyTableSqlBuilder.buildSql(compareInfo);

        assertTrue(sql.contains("DROP CONSTRAINT \"test_table_pkey\""), "应包含 DROP CONSTRAINT");
        assertTrue(sql.contains("ADD CONSTRAINT \"test_table_pkey\" PRIMARY KEY"), "应复用约束名");
        assertTrue(sql.contains("\"id\""), "应包含新主键列 id");
        assertTrue(sql.contains("\"tenant_id\""), "应包含新主键列 tenant_id");
    }

    @Test
    void testAddPrimaryWithoutDrop() {
        PgsqlCompareTableInfo compareInfo = new PgsqlCompareTableInfo("test_table", "public");

        ColumnMetadata pk = makeColumn("id", "int8", null);
        pk.setPrimary(true);
        compareInfo.addNewPrimary(Arrays.asList(pk));

        String sql = ModifyTableSqlBuilder.buildSql(compareInfo);

        assertTrue(sql.contains("ADD PRIMARY KEY"), "应包含 ADD PRIMARY KEY（不带约束名）");
        assertFalse(sql.contains("DROP CONSTRAINT"), "不应包含 DROP CONSTRAINT");
    }

    @Test
    void testTableComment() {
        PgsqlCompareTableInfo compareInfo = new PgsqlCompareTableInfo("test_table", "public");
        compareInfo.setComment("新注释");

        String sql = ModifyTableSqlBuilder.buildSql(compareInfo);

        assertTrue(sql.contains("COMMENT ON TABLE \"public\".\"test_table\" IS '新注释'"), "应包含表注释更新");
    }

    @Test
    void testEmptyCompareTableInfo() {
        PgsqlCompareTableInfo compareInfo = new PgsqlCompareTableInfo("test_table", "public");

        String sql = ModifyTableSqlBuilder.buildSql(compareInfo);

        // 无任何变更，不应生成 ALTER TABLE
        assertFalse(sql.contains("ALTER TABLE"), "无变更时不应生成 ALTER TABLE");
    }
}
