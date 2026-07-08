package org.dromara.autotable.adapter.mybatisplus;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * MybatisPlusMetadataAdapter 单元测试（零 Spring，纯 Java 反射）。
 * 验证 adapter 读取 MP 原生注解的正确性。
 *
 * @author auto-table
 */
public class MybatisPlusMetadataAdapterTest {

    private MybatisPlusMetadataAdapter adapter;
    private MybatisPlusAdapterConfig config;

    @Before
    public void setUp() {
        config = new MybatisPlusAdapterConfig();
        config.setMapUnderscoreToCamelCase(true);
        config.setCapitalMode(false);
        adapter = new MybatisPlusMetadataAdapter(config);
    }

    // ===== 测试实体 =====

    @TableName("t_user")
    static class UserEntity {
        @TableId(type = IdType.AUTO)
        private Long id;

        @TableField("user_name")
        private String userName;

        private String email;

        @TableField(exist = false)
        private String transientField;
    }

    @TableName(value = "t_order", schema = "public", keepGlobalPrefix = false)
    static class OrderEntity {
        @TableId(value = "order_id", type = IdType.INPUT)
        private Long id;

        @TableField("amount")
        private String amount;
    }

    static class NoAnnotationEntity {
        private Long id;
        private String userName;
    }

    enum StatusEnum {
        ACTIVE(1),
        INACTIVE(0);

        @EnumValue
        private final int code;

        StatusEnum(int code) { this.code = code; }
    }

    // ===== getTableName 测试 =====

    @Test
    public void testGetTableName_withTableNameAnnotation() {
        String tableName = adapter.getTableName(UserEntity.class);
        assertEquals("表名应读取 @TableName.value()", "t_user", tableName);
    }

    @Test
    public void testGetTableName_withTablePrefix() {
        config.setTablePrefix("app_");
        MybatisPlusMetadataAdapter adapterWithPrefix = new MybatisPlusMetadataAdapter(config);

        String tableName = adapterWithPrefix.getTableName(UserEntity.class);
        assertEquals("keepGlobalPrefix=false 时不加前缀", "t_user", tableName);

        // OrderEntity 有 keepGlobalPrefix=false
        String orderTable = adapterWithPrefix.getTableName(OrderEntity.class);
        assertEquals("keepGlobalPrefix=false 时不加前缀", "t_order", orderTable);
    }

    @Test
    public void testGetTableName_noAnnotation_camelToUnderline() {
        String tableName = adapter.getTableName(NoAnnotationEntity.class);
        assertEquals("无注解时类名驼峰转下划线", "no_annotation_entity", tableName);
    }

    // ===== getTableSchema 测试 =====

    @Test
    public void testGetTableSchema_withSchema() {
        String schema = adapter.getTableSchema(OrderEntity.class);
        assertEquals("schema 应读取 @TableName.schema()", "public", schema);
    }

    @Test
    public void testGetTableSchema_noSchema() {
        String schema = adapter.getTableSchema(UserEntity.class);
        assertNull("无 schema 时应返回 null", schema);
    }

    // ===== getColumnName 测试 =====

    @Test
    public void testGetColumnName_withTableField() throws Exception {
        Field field = UserEntity.class.getDeclaredField("userName");
        String columnName = adapter.getColumnName(UserEntity.class, field);
        assertEquals("@TableField.value() 应作为列名", "user_name", columnName);
    }

    @Test
    public void testGetColumnName_withTableId() throws Exception {
        Field field = OrderEntity.class.getDeclaredField("id");
        String columnName = adapter.getColumnName(OrderEntity.class, field);
        assertEquals("@TableId.value() 应作为列名", "order_id", columnName);
    }

    @Test
    public void testGetColumnName_noAnnotation_camelToUnderline() throws Exception {
        Field field = UserEntity.class.getDeclaredField("email");
        String columnName = adapter.getColumnName(UserEntity.class, field);
        assertEquals("无注解时字段名驼峰转下划线", "email", columnName);
    }

    @Test
    public void testGetColumnName_capitalMode() throws Exception {
        config.setCapitalMode(true);
        MybatisPlusMetadataAdapter adapterCapital = new MybatisPlusMetadataAdapter(config);

        Field field = NoAnnotationEntity.class.getDeclaredField("userName");
        String columnName = adapterCapital.getColumnName(NoAnnotationEntity.class, field);
        assertEquals("大写模式应转大写", "USER_NAME", columnName);
    }

    // ===== isPrimary 测试 =====

    @Test
    public void testIsPrimary_withTableId() throws Exception {
        Field field = UserEntity.class.getDeclaredField("id");
        assertTrue("@TableId 标注的字段应为主键", adapter.isPrimary(field, UserEntity.class));
    }

    @Test
    public void testIsPrimary_idField() throws Exception {
        Field field = NoAnnotationEntity.class.getDeclaredField("id");
        assertTrue("名为 id 的字段默认视为主键", adapter.isPrimary(field, NoAnnotationEntity.class));
    }

    @Test
    public void testIsPrimary_nonPrimaryField() throws Exception {
        Field field = UserEntity.class.getDeclaredField("userName");
        assertFalse("非主键字段不应为主键", adapter.isPrimary(field, UserEntity.class));
    }

    // ===== isAutoIncrement 测试 =====

    @Test
    public void testIsAutoIncrement_autoType() throws Exception {
        Field field = UserEntity.class.getDeclaredField("id");
        assertTrue("IdType.AUTO 应为自增", adapter.isAutoIncrement(field, UserEntity.class));
    }

    @Test
    public void testIsAutoIncrement_inputType() throws Exception {
        Field field = OrderEntity.class.getDeclaredField("id");
        assertFalse("IdType.INPUT 不应为自增", adapter.isAutoIncrement(field, OrderEntity.class));
    }

    // ===== isIgnoreField 测试 =====

    @Test
    public void testIsIgnoreField_existFalse() throws Exception {
        Field field = UserEntity.class.getDeclaredField("transientField");
        assertTrue("@TableField(exist=false) 应被忽略", adapter.isIgnoreField(field, UserEntity.class));
    }

    @Test
    public void testIsIgnoreField_normalField() throws Exception {
        Field field = UserEntity.class.getDeclaredField("userName");
        assertFalse("正常字段不应被忽略", adapter.isIgnoreField(field, UserEntity.class));
    }

    // ===== getColumnEnumValues 测试 =====

    @Test
    public void testGetColumnEnumValues_withEnumValue() {
        List<String> values = adapter.getColumnEnumValues(StatusEnum.class);
        assertEquals("应返回 2 个枚举值", 2, values.size());
        assertEquals("第一个枚举值的 @EnumValue 字段值", "1", values.get(0));
        assertEquals("第二个枚举值的 @EnumValue 字段值", "0", values.get(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetColumnEnumValues_notEnum() {
        adapter.getColumnEnumValues(String.class);
    }

    // ===== getColumnDefaultValue 测试 =====

    @Test
    public void testGetColumnDefaultValue_logicDeleteField() throws Exception {
        config.setLogicDeleteField("deleted");
        config.setLogicNotDeleteValue("0");
        MybatisPlusMetadataAdapter adapterLogic = new MybatisPlusMetadataAdapter(config);

        // 创建一个带逻辑删除字段的实体
        Field field = LogicDeleteEntity.class.getDeclaredField("deleted");
        assertNotNull("逻辑删除字段应有默认值", adapterLogic.getColumnDefaultValue(field, LogicDeleteEntity.class));
    }

    static class LogicDeleteEntity {
        private Long id;
        private Integer deleted;
    }
}
