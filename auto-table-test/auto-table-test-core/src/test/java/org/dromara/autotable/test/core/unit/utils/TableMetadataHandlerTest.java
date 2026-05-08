package org.dromara.autotable.test.core.unit.utils;

import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.ColumnComment;
import org.dromara.autotable.annotation.PrimaryKey;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.utils.TableMetadataHandler;
import org.dromara.autotable.strategy.mysql.MysqlStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TableMetadataHandler 单元测试
 */
public class TableMetadataHandlerTest {

    @BeforeEach
    void setUp() {
        IStrategy.setCurrentStrategy(new MysqlStrategy());
    }

    @AfterEach
    void tearDown() {
        IStrategy.clean();
    }

    @Test
    void testGetTableName_withAnnotationValue() {
        assertEquals("custom_table", TableMetadataHandler.getTableName(WithTableName.class));
    }

    @Test
    void testGetTableName_withoutAnnotation() {
        assertEquals("without_table_name", TableMetadataHandler.getTableName(WithoutTableName.class));
    }

    @Test
    void testGetTableComment_withAnnotation() {
        assertEquals("测试表", TableMetadataHandler.getTableComment(WithTableComment.class));
    }

    @Test
    void testGetTableComment_withoutAnnotation() {
        assertNull(TableMetadataHandler.getTableComment(WithoutTableComment.class));
    }

    @Test
    void testIsPrimary_withPrimaryKeyAnnotation() {
        java.lang.reflect.Field field;
        try {
            field = PrimaryKeyField.class.getDeclaredField("id");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        assertTrue(TableMetadataHandler.isPrimary(field, PrimaryKeyField.class));
    }

    @Test
    void testIsPrimary_withoutAnnotation() {
        java.lang.reflect.Field field;
        try {
            field = NormalField.class.getDeclaredField("name");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        assertFalse(TableMetadataHandler.isPrimary(field, NormalField.class));
    }

    @Test
    void testGetColumnComment_withColumnCommentAnnotation() {
        java.lang.reflect.Field field;
        try {
            field = ColumnCommentField.class.getDeclaredField("name");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        assertEquals("用户名", TableMetadataHandler.getColumnComment(field, ColumnCommentField.class));
    }

    @Test
    void testGetColumnComment_withoutAnnotation() {
        java.lang.reflect.Field field;
        try {
            field = NormalField.class.getDeclaredField("name");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        assertEquals("", TableMetadataHandler.getColumnComment(field, NormalField.class));
    }

    @Test
    void testGetColumnName_withColumnNameAnnotation() {
        java.lang.reflect.Field field;
        try {
            field = ColumnNameField.class.getDeclaredField("userName");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        assertEquals("user_name", TableMetadataHandler.getColumnName(ColumnNameField.class, field));
    }

    @Test
    void testGetColumnName_withoutAnnotation() {
        java.lang.reflect.Field field;
        try {
            field = NormalField.class.getDeclaredField("userName");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        assertEquals("user_name", TableMetadataHandler.getColumnName(NormalField.class, field));
    }

    // 测试实体类
    @AutoTable(value = "custom_table")
    public static class WithTableName {
    }

    public static class WithoutTableName {
    }

    @AutoTable(comment = "测试表")
    public static class WithTableComment {
    }

    public static class WithoutTableComment {
    }

    public static class PrimaryKeyField {
        @PrimaryKey
        private Long id;
    }

    public static class NormalField {
        private String name;
        private String userName;
    }

    public static class ColumnCommentField {
        @ColumnComment("用户名")
        private String name;
    }

    public static class ColumnNameField {
        @org.dromara.autotable.annotation.ColumnName("user_name")
        private String userName;
    }
}
