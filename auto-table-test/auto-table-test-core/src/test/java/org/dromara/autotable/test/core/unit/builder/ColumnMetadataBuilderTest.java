package org.dromara.autotable.test.core.unit.builder;

import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.ColumnComment;
import org.dromara.autotable.annotation.ColumnNotNull;
import org.dromara.autotable.annotation.PrimaryKey;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.builder.ColumnMetadataBuilder;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.strategy.mysql.MysqlStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ColumnMetadataBuilder 单元测试
 */
public class ColumnMetadataBuilderTest {

    @BeforeEach
    void setUp() {
        MysqlStrategy mysqlStrategy = new MysqlStrategy();
        IStrategy.setCurrentStrategy(mysqlStrategy);
        AutoTableGlobalConfig.instance().addStrategy(mysqlStrategy);
    }

    @AfterEach
    void tearDown() {
        IStrategy.clean();
    }

    @Test
    void testBuildList_withSimpleFields() {
        ColumnMetadataBuilder builder = new ColumnMetadataBuilder("MySQL");
        List<Field> fields = java.util.Arrays.asList(SimpleEntity.class.getDeclaredFields());
        List<ColumnMetadata> result = builder.buildList(SimpleEntity.class, fields);

        assertNotNull(result);
        assertEquals(2, result.size());

        ColumnMetadata idColumn = result.stream().filter(c -> "id".equals(c.getName())).findFirst().orElse(null);
        assertNotNull(idColumn);
        assertTrue(idColumn.isPrimary());
        assertTrue(idColumn.isNotNull());

        ColumnMetadata nameColumn = result.stream().filter(c -> "name".equals(c.getName())).findFirst().orElse(null);
        assertNotNull(nameColumn);
        assertFalse(nameColumn.isPrimary());
    }

    @Test
    void testBuildList_withAnnotations() {
        ColumnMetadataBuilder builder = new ColumnMetadataBuilder("MySQL");
        List<Field> fields = java.util.Arrays.asList(AnnotatedEntity.class.getDeclaredFields());
        List<ColumnMetadata> result = builder.buildList(AnnotatedEntity.class, fields);

        assertNotNull(result);
        assertEquals(3, result.size());

        ColumnMetadata idColumn = result.stream().filter(c -> "id".equals(c.getName())).findFirst().orElse(null);
        assertNotNull(idColumn);
        assertTrue(idColumn.isPrimary());
        assertEquals("主键", idColumn.getComment());

        ColumnMetadata userNameColumn = result.stream().filter(c -> "user_name".equals(c.getName())).findFirst().orElse(null);
        assertNotNull(userNameColumn);
        assertEquals("用户名", userNameColumn.getComment());
        assertTrue(userNameColumn.isNotNull());
    }

    // 测试实体类
    public static class SimpleEntity {
        @PrimaryKey
        private Long id;
        private String name;
    }

    public static class AnnotatedEntity {
        @PrimaryKey
        @ColumnComment("主键")
        private Long id;

        @ColumnComment("用户名")
        @ColumnNotNull
        @org.dromara.autotable.annotation.ColumnName("user_name")
        private String userName;

        @AutoColumn(comment = "年龄")
        private Integer age;
    }
}
