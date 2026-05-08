package org.dromara.autotable.test.core.unit.builder;

import org.dromara.autotable.annotation.Index;
import org.dromara.autotable.annotation.TableIndex;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.builder.ColumnMetadataBuilder;
import org.dromara.autotable.core.builder.DefaultTableMetadataBuilder;
import org.dromara.autotable.core.builder.IndexMetadataBuilder;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.strategy.mysql.MysqlStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultTableMetadataBuilder 单元测试
 */
public class DefaultTableMetadataBuilderTest {

    @BeforeEach
    void setUp() {
        IStrategy.setCurrentStrategy(new MysqlStrategy());
    }

    @AfterEach
    void tearDown() {
        IStrategy.clean();
    }

    @Test
    void testBuild_withSimpleEntity() {
        DefaultTableMetadataBuilder builder = new DefaultTableMetadataBuilder(
                new ColumnMetadataBuilder("MySQL"),
                new IndexMetadataBuilder()
        );

        DefaultTableMetadata metadata = builder.build(SimpleEntity.class);

        assertNotNull(metadata);
        assertEquals("simple_entity", metadata.getTableName());
        assertNull(metadata.getComment());

        List<ColumnMetadata> columns = metadata.getColumnMetadataList();
        assertNotNull(columns);
        assertEquals(2, columns.size());
    }

    @Test
    void testBuild_withAnnotatedEntity() {
        DefaultTableMetadataBuilder builder = new DefaultTableMetadataBuilder(
                new ColumnMetadataBuilder("MySQL"),
                new IndexMetadataBuilder()
        );

        DefaultTableMetadata metadata = builder.build(AnnotatedEntity.class);

        assertNotNull(metadata);
        assertEquals("custom_table", metadata.getTableName());
        assertEquals("测试表", metadata.getComment());

        List<ColumnMetadata> columns = metadata.getColumnMetadataList();
        assertNotNull(columns);
        assertEquals(2, columns.size());

        ColumnMetadata idColumn = columns.stream().filter(c -> "id".equals(c.getName())).findFirst().orElse(null);
        assertNotNull(idColumn);
        assertTrue(idColumn.isPrimary());
    }

    @Test
    void testBuild_withIndex() {
        DefaultTableMetadataBuilder builder = new DefaultTableMetadataBuilder(
                new ColumnMetadataBuilder("MySQL"),
                new IndexMetadataBuilder()
        );

        DefaultTableMetadata metadata = builder.build(EntityWithIndex.class);

        assertNotNull(metadata);
        List<IndexMetadata> indexes = metadata.getIndexMetadataList();
        assertNotNull(indexes);
        assertFalse(indexes.isEmpty());
    }

    @org.dromara.autotable.annotation.AutoTable
    public static class SimpleEntity {
        private Long id;
        private String name;
    }

    @org.dromara.autotable.annotation.AutoTable(value = "custom_table", comment = "测试表")
    public static class AnnotatedEntity {
        @org.dromara.autotable.annotation.PrimaryKey
        private Long id;
        private String name;
    }

    @org.dromara.autotable.annotation.AutoTable
    public static class EntityWithIndex {
        @Index(comment = "姓名索引")
        private String name;
        private Integer age;
    }
}
