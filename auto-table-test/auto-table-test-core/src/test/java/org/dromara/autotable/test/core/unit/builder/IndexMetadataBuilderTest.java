package org.dromara.autotable.test.core.unit.builder;

import org.dromara.autotable.annotation.Index;
import org.dromara.autotable.annotation.TableIndex;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.builder.IndexMetadataBuilder;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.strategy.mysql.MysqlStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IndexMetadataBuilder 单元测试
 */
public class IndexMetadataBuilderTest {

    @BeforeEach
    void setUp() {
        IStrategy.setCurrentStrategy(new MysqlStrategy());
    }

    @AfterEach
    void tearDown() {
        IStrategy.clean();
    }

    @Test
    void testBuildList_withFieldIndex() {
        IndexMetadataBuilder builder = new IndexMetadataBuilder();
        List<Field> fields = java.util.Arrays.asList(EntityWithFieldIndex.class.getDeclaredFields());
        List<IndexMetadata> result = builder.buildList(EntityWithFieldIndex.class, fields);

        assertNotNull(result);
        assertEquals(1, result.size());

        IndexMetadata indexMetadata = result.get(0);
        assertTrue(indexMetadata.getName().startsWith("auto_idx_"));
        assertEquals(IndexTypeEnum.NORMAL, indexMetadata.getType());
        assertEquals(1, indexMetadata.getColumns().size());
        assertEquals("name", indexMetadata.getColumns().get(0).getColumn());
    }

    @Test
    void testBuildList_withTableIndex() {
        IndexMetadataBuilder builder = new IndexMetadataBuilder();
        List<Field> fields = java.util.Arrays.asList(EntityWithTableIndex.class.getDeclaredFields());
        List<IndexMetadata> result = builder.buildList(EntityWithTableIndex.class, fields);

        assertNotNull(result);
        assertEquals(1, result.size());

        IndexMetadata indexMetadata = result.get(0);
        assertEquals("auto_idx_idx_name_age", indexMetadata.getName());
        assertEquals(IndexTypeEnum.NORMAL, indexMetadata.getType());
        assertEquals(2, indexMetadata.getColumns().size());
        assertEquals("name", indexMetadata.getColumns().get(0).getColumn());
        assertEquals("age", indexMetadata.getColumns().get(1).getColumn());
    }

    @Test
    void testBuildList_withUniqueIndex() {
        IndexMetadataBuilder builder = new IndexMetadataBuilder();
        List<Field> fields = java.util.Arrays.asList(EntityWithUniqueIndex.class.getDeclaredFields());
        List<IndexMetadata> result = builder.buildList(EntityWithUniqueIndex.class, fields);

        assertNotNull(result);
        assertEquals(1, result.size());

        IndexMetadata indexMetadata = result.get(0);
        assertEquals(IndexTypeEnum.UNIQUE, indexMetadata.getType());
    }

    // 测试实体类
    @org.dromara.autotable.annotation.AutoTable
    public static class EntityWithFieldIndex {
        @Index(comment = "普通索引")
        private String name;
    }

    @org.dromara.autotable.annotation.AutoTable
    @TableIndex(name = "idx_name_age", fields = {"name", "age"})
    public static class EntityWithTableIndex {
        private String name;
        private Integer age;
    }

    @org.dromara.autotable.annotation.AutoTable
    public static class EntityWithUniqueIndex {
        @Index(type = IndexTypeEnum.UNIQUE)
        private String email;
    }
}
