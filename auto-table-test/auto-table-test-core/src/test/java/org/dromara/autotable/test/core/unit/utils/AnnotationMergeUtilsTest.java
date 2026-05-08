package org.dromara.autotable.test.core.unit.utils;

import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.core.utils.AnnotationMergeUtils;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AnnotationMergeUtils 单元测试
 */
public class AnnotationMergeUtilsTest {

    @Test
    void testMerge_withEmptySet() {
        AutoTable result = AnnotationMergeUtils.merge(AutoTable.class, new HashSet<>());
        assertNull(result);
    }

    @Test
    void testMerge_withNullSet() {
        AutoTable result = AnnotationMergeUtils.merge(AutoTable.class, null);
        assertNull(result);
    }

    @Test
    void testMerge_withSingleAnnotation() {
        Set<AutoTable> annotations = new HashSet<>();
        AutoTable anno = createAutoTable("test_table", "测试表", "", "", "");
        annotations.add(anno);

        AutoTable result = AnnotationMergeUtils.merge(AutoTable.class, annotations);

        assertNotNull(result);
        assertEquals("test_table", result.value());
        assertEquals("测试表", result.comment());
    }

    @Test
    void testMerge_multipleAnnotations_withDifferentValues() {
        Set<AutoTable> annotations = new HashSet<>();
        AutoTable anno1 = createAutoTable("table1", "表1", "", "", "");
        AutoTable anno2 = createAutoTable("table2", "表2", "", "", "");
        annotations.add(anno1);
        annotations.add(anno2);

        AutoTable result = AnnotationMergeUtils.merge(AutoTable.class, annotations);

        assertNotNull(result);
        // 取第一个非默认值（HashSet无序，结果可能是table1或table2）
        assertTrue(result.value().equals("table1") || result.value().equals("table2"));
    }

    @Test
    void testMerge_withDefaultValuesOnly() {
        Set<AutoTable> annotations = new HashSet<>();
        AutoTable anno = createAutoTable("", "", "", "", "");
        annotations.add(anno);

        AutoTable result = AnnotationMergeUtils.merge(AutoTable.class, annotations);

        assertNotNull(result);
        // 所有值都是默认值，应该保持默认值
        assertEquals("", result.value());
    }

    private AutoTable createAutoTable(String value, String comment, String schema, String dialect, String initSql) {
        return new AutoTable() {
            @Override
            public String value() { return value; }
            @Override
            public String comment() { return comment; }
            @Override
            public String schema() { return schema; }
            @Override
            public String initSql() { return initSql; }
            @Override
            public String dialect() { return dialect; }
            @Override
            public Class<? extends Annotation> annotationType() { return AutoTable.class; }
        };
    }
}
