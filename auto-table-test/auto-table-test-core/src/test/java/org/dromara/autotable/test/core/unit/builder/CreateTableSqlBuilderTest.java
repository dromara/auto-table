package org.dromara.autotable.test.core.unit.builder;

import org.dromara.autotable.annotation.enums.IndexSortTypeEnum;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.strategy.mysql.MysqlStrategy;
import org.dromara.autotable.strategy.mysql.builder.CreateTableSqlBuilder;
import org.dromara.autotable.strategy.mysql.data.MysqlColumnMetadata;
import org.dromara.autotable.strategy.mysql.data.MysqlTableMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CreateTableSqlBuilder 单元测试
 */
public class CreateTableSqlBuilderTest {

    @BeforeEach
    void setUp() {
        IStrategy.setCurrentStrategy(new MysqlStrategy());
    }

    @Test
    void testBuildSql_withSimpleTable() {
        MysqlTableMetadata tableMetadata = new MysqlTableMetadata(null, "test_user", "用户表");
        tableMetadata.setEngine("InnoDB");
        tableMetadata.setCharacterSet("utf8mb4");
        tableMetadata.setCollate("utf8mb4_unicode_ci");

        List<MysqlColumnMetadata> columns = new ArrayList<>();
        MysqlColumnMetadata idColumn = new MysqlColumnMetadata();
        idColumn.setName("id");
        idColumn.setType(new DatabaseTypeAndLength("bigint", 20, null, Collections.emptyList()));
        idColumn.setNotNull(true);
        idColumn.setPrimary(true);
        idColumn.setAutoIncrement(true);
        idColumn.setComment("主键");
        idColumn.setPosition(0);
        columns.add(idColumn);

        MysqlColumnMetadata nameColumn = new MysqlColumnMetadata();
        nameColumn.setName("name");
        nameColumn.setType(new DatabaseTypeAndLength("varchar", 100, null, Collections.emptyList()));
        nameColumn.setNotNull(true);
        nameColumn.setComment("用户名");
        nameColumn.setPosition(1);
        columns.add(nameColumn);

        tableMetadata.setColumnMetadataList(columns);
        tableMetadata.setIndexMetadataList(new ArrayList<>());

        String sql = CreateTableSqlBuilder.buildSql(tableMetadata);

        assertTrue(sql.startsWith("CREATE TABLE"));
        assertTrue(sql.contains("`test_user`"));
        assertTrue(sql.contains("`id`"));
        assertTrue(sql.contains("bigint(20)"));
        assertTrue(sql.contains("AUTO_INCREMENT"));
        assertTrue(sql.contains("PRIMARY KEY"));
        assertTrue(sql.contains("ENGINE = InnoDB"));
        assertTrue(sql.contains("CHARACTER SET = utf8mb4"));
        assertTrue(sql.contains("COLLATE = utf8mb4_unicode_ci"));
        assertTrue(sql.contains("COMMENT = '用户表'"));
    }

    @Test
    void testBuildSql_withIndex() {
        MysqlTableMetadata tableMetadata = new MysqlTableMetadata(null, "test_index", "");
        tableMetadata.setEngine("InnoDB");

        List<MysqlColumnMetadata> columns = new ArrayList<>();
        MysqlColumnMetadata idColumn = new MysqlColumnMetadata();
        idColumn.setName("id");
        idColumn.setType(new DatabaseTypeAndLength("bigint", 20, null, Collections.emptyList()));
        idColumn.setPrimary(true);
        idColumn.setPosition(0);
        columns.add(idColumn);

        MysqlColumnMetadata nameColumn = new MysqlColumnMetadata();
        nameColumn.setName("name");
        nameColumn.setType(new DatabaseTypeAndLength("varchar", 100, null, Collections.emptyList()));
        nameColumn.setPosition(1);
        columns.add(nameColumn);

        tableMetadata.setColumnMetadataList(columns);

        IndexMetadata indexMetadata = new IndexMetadata();
        indexMetadata.setName("idx_name");
        indexMetadata.setType(IndexTypeEnum.NORMAL);
        indexMetadata.setColumns(Arrays.asList(
            IndexMetadata.IndexColumnParam.newInstance("name", null)
        ));
        tableMetadata.setIndexMetadataList(Arrays.asList(indexMetadata));

        String sql = CreateTableSqlBuilder.buildSql(tableMetadata);

        assertTrue(sql.contains("INDEX `idx_name`(`name`)"));
    }

    @Test
    void testBuildSql_withUniqueIndex() {
        MysqlTableMetadata tableMetadata = new MysqlTableMetadata(null, "test_unique", "");
        tableMetadata.setEngine("InnoDB");

        List<MysqlColumnMetadata> columns = new ArrayList<>();
        MysqlColumnMetadata idColumn = new MysqlColumnMetadata();
        idColumn.setName("id");
        idColumn.setType(new DatabaseTypeAndLength("bigint", 20, null, Collections.emptyList()));
        idColumn.setPrimary(true);
        idColumn.setPosition(0);
        columns.add(idColumn);

        tableMetadata.setColumnMetadataList(columns);

        IndexMetadata indexMetadata = new IndexMetadata();
        indexMetadata.setName("uk_email");
        indexMetadata.setType(IndexTypeEnum.UNIQUE);
        indexMetadata.setColumns(Arrays.asList(
            IndexMetadata.IndexColumnParam.newInstance("email", null)
        ));
        tableMetadata.setIndexMetadataList(Arrays.asList(indexMetadata));

        String sql = CreateTableSqlBuilder.buildSql(tableMetadata);

        assertTrue(sql.contains("UNIQUE INDEX `uk_email`(`email`)"));
    }

    @Test
    void testGetTableProperties_withAllProperties() {
        List<String> properties = CreateTableSqlBuilder.getTableProperties("InnoDB", "utf8mb4", "utf8mb4_unicode_ci", "测试表");

        assertEquals(4, properties.size());
        assertTrue(properties.contains("ENGINE = InnoDB"));
        assertTrue(properties.contains("CHARACTER SET = utf8mb4"));
        assertTrue(properties.contains("COLLATE = utf8mb4_unicode_ci"));
        assertTrue(properties.stream().anyMatch(p -> p.contains("COMMENT")));
    }

    @Test
    void testGetTableProperties_withEmptyValues() {
        List<String> properties = CreateTableSqlBuilder.getTableProperties(null, null, null, null);

        assertTrue(properties.isEmpty());
    }

    @Test
    void testGetPrimaryKeySql() {
        String sql = CreateTableSqlBuilder.getPrimaryKeySql(Arrays.asList("id", "tenant_id"));

        assertEquals("PRIMARY KEY (`id`,`tenant_id`)", sql);
    }
}
