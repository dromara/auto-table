package org.dromara.autotable.adapter.mybatisplus;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.UnknownTypeHandler;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

/**
 * MybatisPlusJavaTypeToDatabaseTypeConverter 单元测试。
 * 验证枚举类型转换和自定义 TypeHandler 处理逻辑。
 *
 * @author auto-table
 */
public class MybatisPlusJavaTypeToDatabaseTypeConverterTest {

    private final MybatisPlusJavaTypeToDatabaseTypeConverter converter = new MybatisPlusJavaTypeToDatabaseTypeConverter();

    // ===== 测试实体 =====

    enum StatusWithEnumValue {
        ACTIVE(1),
        INACTIVE(0);

        @EnumValue
        private final int code;

        StatusWithEnumValue(int code) { this.code = code; }
    }

    enum SimpleEnum {
        A, B, C
    }

    enum EnumWithIEnum implements IEnum<String> {
        ACTIVE("active"),
        INACTIVE("inactive");

        private final String value;

        EnumWithIEnum(String value) { this.value = value; }

        @Override
        public String getValue() { return value; }
    }

    /**
     * 测试用 TypeHandler（非 UnknownTypeHandler）
     */
    public static class TestTypeHandler extends BaseTypeHandler<String> {
        @Override public void setNonNullParameter(java.sql.PreparedStatement ps, int i, String parameter, JdbcType jdbcType) {}
        @Override public String getNullableResult(java.sql.ResultSet rs, String columnName) { return null; }
        @Override public String getNullableResult(java.sql.ResultSet rs, int columnIndex) { return null; }
        @Override public String getNullableResult(java.sql.CallableStatement cs, int columnIndex) { return null; }
    }

    static class EntityWithTypeHandler {
        @TableField(typeHandler = TestTypeHandler.class)
        private String jsonField;

        @TableField
        private String normalField;
    }

    static class NormalEntity {
        private String name;
        private Integer age;
    }

    // ===== 测试 =====

    @Test
    public void testGetFieldType_normalField() throws Exception {
        Field field = NormalEntity.class.getDeclaredField("name");
        assertEquals("普通字段应返回原始类型", String.class, converter.getFieldType(NormalEntity.class, field));
    }

    @Test
    public void testGetFieldType_integerField() throws Exception {
        Field field = NormalEntity.class.getDeclaredField("age");
        assertEquals("Integer 字段应返回 Integer", Integer.class, converter.getFieldType(NormalEntity.class, field));
    }

    @Test
    public void testGetFieldType_enumWithEnumValue() throws Exception {
        Field field = EntityWithEnum.class.getDeclaredField("status");
        Class<?> resultType = converter.getFieldType(EntityWithEnum.class, field);
        assertEquals("@EnumValue(int) 枚举应返回 int 类型", int.class, resultType);
    }

    static class EntityWithEnum {
        private StatusWithEnumValue status;
    }

    @Test
    public void testGetFieldType_simpleEnum() throws Exception {
        Field field = EntityWithSimpleEnum.class.getDeclaredField("simple");
        Class<?> resultType = converter.getFieldType(EntityWithSimpleEnum.class, field);
        assertEquals("无 @EnumValue 的枚举应返回 String", String.class, resultType);
    }

    static class EntityWithSimpleEnum {
        private SimpleEnum simple;
    }

    @Test
    public void testGetFieldType_enumWithIEnum() throws Exception {
        Field field = EntityWithIEnumField.class.getDeclaredField("status");
        Class<?> resultType = converter.getFieldType(EntityWithIEnumField.class, field);
        assertEquals("IEnum<String> 枚举应返回 String", String.class, resultType);
    }

    static class EntityWithIEnumField {
        private EnumWithIEnum status;
    }

    @Test
    public void testGetFieldType_customTypeHandler() throws Exception {
        Field field = EntityWithTypeHandler.class.getDeclaredField("jsonField");
        Class<?> resultType = converter.getFieldType(EntityWithTypeHandler.class, field);
        assertEquals("自定义 TypeHandler 应返回 String", String.class, resultType);
    }

    @Test
    public void testGetFieldType_defaultTypeHandler() throws Exception {
        Field field = EntityWithTypeHandler.class.getDeclaredField("normalField");
        Class<?> resultType = converter.getFieldType(EntityWithTypeHandler.class, field);
        assertEquals("默认 TypeHandler 应返回原始类型", String.class, resultType);
    }
}
