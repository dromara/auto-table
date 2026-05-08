package org.dromara.autotable.test.core.unit.utils;

import org.dromara.autotable.core.utils.BeanClassUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BeanClassUtil 单元测试
 */
public class BeanClassUtilTest {

    @Test
    void testGetField_fromCurrentClass() {
        Field field = BeanClassUtil.getField(CurrentClass.class, "name");
        assertNotNull(field);
        assertEquals("name", field.getName());
    }

    @Test
    void testGetField_fromParentClass() {
        Field field = BeanClassUtil.getField(ChildClass.class, "id");
        assertNotNull(field);
        assertEquals("id", field.getName());
    }

    @Test
    void testGetField_notFound() {
        assertThrows(RuntimeException.class, () -> BeanClassUtil.getField(CurrentClass.class, "nonExistent"));
    }

    @Test
    void testSortAllFieldForColumn_simpleClass() {
        List<Field> fields = BeanClassUtil.sortAllFieldForColumn(SimpleClass.class);
        assertNotNull(fields);
        assertEquals(2, fields.size());
    }

    @Test
    void testSortAllFieldForColumn_withSortAnnotation() {
        List<Field> fields = BeanClassUtil.sortAllFieldForColumn(SortedClass.class);
        assertNotNull(fields);
        assertEquals(3, fields.size());
        // 第一个字段应该是 sort=1 的 first
        assertEquals("first", fields.get(0).getName());
        // 最后一个字段应该是 sort=-1 的 last
        assertEquals("last", fields.get(fields.size() - 1).getName());
    }

    // 测试实体类
    public static class ParentClass {
        private Long id;
    }

    public static class CurrentClass {
        private String name;
        private Integer age;
    }

    public static class ChildClass extends ParentClass {
        private String name;
    }

    public static class SimpleClass {
        private String field1;
        private Integer field2;
    }

    public static class SortedClass {
        @org.dromara.autotable.annotation.AutoColumn(sort = 1)
        private String first;
        private String middle;
        @org.dromara.autotable.annotation.AutoColumn(sort = -1)
        private String last;
    }
}
