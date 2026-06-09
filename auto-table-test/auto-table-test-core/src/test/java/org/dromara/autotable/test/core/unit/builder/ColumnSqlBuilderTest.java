package org.dromara.autotable.test.core.unit.builder;

import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.strategy.mysql.builder.ColumnSqlBuilder;
import org.dromara.autotable.strategy.mysql.data.MysqlColumnMetadata;
import org.junit.jupiter.api.Test;

import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.strategy.mysql.MysqlStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ColumnSqlBuilder 单元测试
 */
public class ColumnSqlBuilderTest {

    @BeforeEach
    void setUp() {
        IStrategy.setCurrentStrategy(new MysqlStrategy());
    }

    @AfterEach
    void tearDown() {
        IStrategy.clean();
    }

    @Test
    void testBuildSql_simpleColumn() {
        MysqlColumnMetadata column = new MysqlColumnMetadata();
        column.setName("name");
        column.setType(new DatabaseTypeAndLength("varchar", 255, null, Collections.emptyList()));
        column.setNotNull(false);
        column.setComment("用户名");

        String sql = ColumnSqlBuilder.buildSql(column);

        assertTrue(sql.contains("`name`"));
        assertTrue(sql.contains("varchar(255)"));
        assertTrue(sql.contains("NULL"));
        assertTrue(sql.contains("COMMENT '用户名'"));
    }

    @Test
    void testBuildSql_notNullColumn() {
        MysqlColumnMetadata column = new MysqlColumnMetadata();
        column.setName("age");
        column.setType(new DatabaseTypeAndLength("int", 11, null, Collections.emptyList()));
        column.setNotNull(true);

        String sql = ColumnSqlBuilder.buildSql(column);

        assertTrue(sql.contains("NOT NULL"));
        assertFalse(sql.contains("COMMENT"));
    }

    @Test
    void testBuildSql_primaryKeyAutoIncrement() {
        MysqlColumnMetadata column = new MysqlColumnMetadata();
        column.setName("id");
        column.setType(new DatabaseTypeAndLength("bigint", 20, null, Collections.emptyList()));
        column.setNotNull(true);
        column.setAutoIncrement(true);
        column.setComment("主键");

        String sql = ColumnSqlBuilder.buildSql(column);

        assertTrue(sql.contains("`id`"));
        assertTrue(sql.contains("bigint(20)"));
        assertTrue(sql.contains("NOT NULL"));
        assertTrue(sql.contains("AUTO_INCREMENT"));
        assertTrue(sql.contains("COMMENT '主键'"));
    }

    @Test
    void testBuildSql_withDefaultValue() {
        MysqlColumnMetadata column = new MysqlColumnMetadata();
        column.setName("status");
        column.setType(new DatabaseTypeAndLength("varchar", 20, null, Collections.emptyList()));
        column.setDefaultValueType(null);
        column.setDefaultValue("'active'");

        String sql = ColumnSqlBuilder.buildSql(column);

        assertTrue(sql.contains("DEFAULT 'active'"));
    }

    @Test
    void testBuildSql_withDefaultNull() {
        MysqlColumnMetadata column = new MysqlColumnMetadata();
        column.setName("deleted");
        column.setType(new DatabaseTypeAndLength("tinyint", 1, null, Collections.emptyList()));
        column.setDefaultValueType(DefaultValueEnum.NULL);

        String sql = ColumnSqlBuilder.buildSql(column);

        assertTrue(sql.contains("DEFAULT NULL"));
    }

    @Test
    void testBuildSql_withDefaultEmptyString() {
        MysqlColumnMetadata column = new MysqlColumnMetadata();
        column.setName("remark");
        column.setType(new DatabaseTypeAndLength("varchar", 255, null, Collections.emptyList()));
        column.setDefaultValueType(DefaultValueEnum.EMPTY_STRING);

        String sql = ColumnSqlBuilder.buildSql(column);

        assertTrue(sql.contains("DEFAULT ''"));
    }

    @Test
    void testBuildSql_unsigned() {
        MysqlColumnMetadata column = new MysqlColumnMetadata();
        column.setName("amount");
        column.setType(new DatabaseTypeAndLength("int", 10, null, Collections.emptyList()));
        column.setUnsigned(true);

        String sql = ColumnSqlBuilder.buildSql(column);

        assertTrue(sql.contains("UNSIGNED"));
    }

    @Test
    void testBuildSql_zerofill() {
        MysqlColumnMetadata column = new MysqlColumnMetadata();
        column.setName("code");
        column.setType(new DatabaseTypeAndLength("int", 8, null, Collections.emptyList()));
        column.setZerofill(true);

        String sql = ColumnSqlBuilder.buildSql(column);

        assertTrue(sql.contains("UNSIGNED"));
        assertTrue(sql.contains("ZEROFILL"));
    }

    @Test
    void testBuildSql_withCharacterSetAndCollate() {
        MysqlColumnMetadata column = new MysqlColumnMetadata();
        column.setName("name");
        column.setType(new DatabaseTypeAndLength("varchar", 100, null, Collections.emptyList()));
        column.setCharacterSet("utf8mb4");
        column.setCollate("utf8mb4_unicode_ci");

        String sql = ColumnSqlBuilder.buildSql(column);

        assertTrue(sql.contains("CHARACTER SET utf8mb4"));
        assertTrue(sql.contains("COLLATE utf8mb4_unicode_ci"));
    }

    @Test
    void testBuildSql_withPosition() {
        MysqlColumnMetadata column = new MysqlColumnMetadata();
        column.setName("email");
        column.setType(new DatabaseTypeAndLength("varchar", 255, null, Collections.emptyList()));
        column.setNewPreColumn("name");

        String sql = ColumnSqlBuilder.buildSql(column);

        assertTrue(sql.contains("AFTER `name`"));
    }

    @Test
    void testBuildSql_firstPosition() {
        MysqlColumnMetadata column = new MysqlColumnMetadata();
        column.setName("id");
        column.setType(new DatabaseTypeAndLength("bigint", 20, null, Collections.emptyList()));
        column.setNewPreColumn("");

        String sql = ColumnSqlBuilder.buildSql(column);

        assertTrue(sql.contains("FIRST"));
    }

    @Test
    void testBuildSql_commentWithSingleQuote() {
        MysqlColumnMetadata column = new MysqlColumnMetadata();
        column.setName("name");
        column.setType(new DatabaseTypeAndLength("varchar", 100, null, Collections.emptyList()));
        column.setComment("用户's name");

        String sql = ColumnSqlBuilder.buildSql(column);

        // 单引号应该被转义为 ''
        assertTrue(sql.contains("COMMENT '用户''s name'"));
    }

    @Test
    void testBuildSql_charsetOnlyWithoutCollate() {
        MysqlColumnMetadata column = new MysqlColumnMetadata();
        column.setName("name");
        column.setType(new DatabaseTypeAndLength("varchar", 100, null, Collections.emptyList()));
        column.setCharacterSet("utf8mb4");
        // collate 不设置

        String sql = ColumnSqlBuilder.buildSql(column);

        // charset 应该独立生效
        assertTrue(sql.contains("CHARACTER SET utf8mb4"));
        assertFalse(sql.contains("COLLATE"));
    }
}
