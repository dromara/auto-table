package org.dromara.autotable.strategy.sqlserver.builder;

import org.dromara.autotable.annotation.ColumnDefault;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.strategy.sqlserver.data.SqlServerDefaultTypeEnum;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SqlServerColumnMetadataBuilder 单元测试。
 * 验证 getDefaultValue 的方言适配：布尔默认值转 0/1、字符串默认值加引号、日期默认值加引号。
 *
 * <p>测试类与 SqlServerColumnMetadataBuilder 同包，可直接访问其 protected getDefaultValue。</p>
 */
public class SqlServerColumnMetadataBuilderTest {

    /**
     * 测试用实体，带各种 @ColumnDefault 注解
     */
    static class TestEntity {
        @ColumnDefault(value = "1")
        String booleanAsString;
        @ColumnDefault(value = "true")
        String booleanTrueAsString;
        @ColumnDefault(value = "0")
        String booleanFalseZero;
        @ColumnDefault(value = "false")
        String booleanFalseAsString;
        @ColumnDefault(value = "active")
        String stringDefault;
        @ColumnDefault(value = "2026-01-01")
        String dateDefault;
        @ColumnDefault(value = "getdate()")
        String dateFunctionDefault;
    }

    private ColumnDefault columnDefault(String fieldName) throws NoSuchFieldException {
        Field field = TestEntity.class.getDeclaredField(fieldName);
        return field.getAnnotation(ColumnDefault.class);
    }

    private DatabaseTypeAndLength typeOf(SqlServerDefaultTypeEnum typeEnum) {
        return new DatabaseTypeAndLength(typeEnum.getTypeName(), typeEnum.getDefaultLength(), typeEnum.getDefaultDecimalLength(), java.util.Collections.emptyList());
    }

    private String resolve(SqlServerDefaultTypeEnum typeEnum, String fieldName) throws NoSuchFieldException {
        SqlServerColumnMetadataBuilder builder = new SqlServerColumnMetadataBuilder();
        return builder.getDefaultValue(typeOf(typeEnum), columnDefault(fieldName));
    }

    @Test
    void test布尔默认值_数字1转1() throws NoSuchFieldException {
        // BIT 列，默认值 "1" 保持为 "1"
        assertEquals("1", resolve(SqlServerDefaultTypeEnum.BIT, "booleanAsString"));
    }

    @Test
    void test布尔默认值_true转1() throws NoSuchFieldException {
        // BIT 列，"true" 转为 "1"
        assertEquals("1", resolve(SqlServerDefaultTypeEnum.BIT, "booleanTrueAsString"));
    }

    @Test
    void test布尔默认值_0保持0() throws NoSuchFieldException {
        assertEquals("0", resolve(SqlServerDefaultTypeEnum.BIT, "booleanFalseZero"));
    }

    @Test
    void test布尔默认值_false转0() throws NoSuchFieldException {
        assertEquals("0", resolve(SqlServerDefaultTypeEnum.BIT, "booleanFalseAsString"));
    }

    @Test
    void test字符串默认值_自动加单引号() throws NoSuchFieldException {
        // NVARCHAR 列，未带引号的值自动包裹 ''
        assertEquals("'active'", resolve(SqlServerDefaultTypeEnum.VARCHAR, "stringDefault"));
    }

    @Test
    void test日期默认值_字面量自动加单引号() throws NoSuchFieldException {
        // DATETIME2 列，日期字面量（匹配日期正则）自动包裹 ''
        assertEquals("'2026-01-01'", resolve(SqlServerDefaultTypeEnum.DATETIME2, "dateDefault"));
    }

    @Test
    void test日期默认值_函数不加引号() throws NoSuchFieldException {
        // DATETIME2 列，getdate() 是函数不是字面量，不加引号
        assertEquals("getdate()", resolve(SqlServerDefaultTypeEnum.DATETIME2, "dateFunctionDefault"));
    }

    @Test
    void test无默认值_返回null() throws NoSuchFieldException {
        // @ColumnDefault 未指定 value，super.getDefaultValue 返回 null
        Field field = TestEntity.class.getDeclaredField("booleanAsString");
        ColumnDefault emptyDefault = new ColumnDefault() {
            @Override
            public DefaultValueEnum type() {
                return DefaultValueEnum.UNDEFINED;
            }

            @Override
            public String value() {
                return "";
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return ColumnDefault.class;
            }
        };
        SqlServerColumnMetadataBuilder builder = new SqlServerColumnMetadataBuilder();
        String result = builder.getDefaultValue(typeOf(SqlServerDefaultTypeEnum.VARCHAR), emptyDefault);
        // 空字符串 value，super 转为 null，builder 不再处理
        assertNull(result);
    }
}
