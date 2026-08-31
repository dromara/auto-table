package org.dromara.autotable.strategy.yashandb;

import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.annotation.enums.IndexSortTypeEnum;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.strategy.yashandb.builder.YashanColumnSqlBuilder;
import org.dromara.autotable.strategy.yashandb.data.YashanCompareTableInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the YashanDB MySQL-compatible AutoTable strategy.
 */
class YashanStrategyTest {
    private final YashanStrategy strategy = new YashanStrategy();

    /**
     * Installs the YashanDB strategy for each isolated test.
     */
    @BeforeEach
    void setUp() {
        IStrategy.setCurrentStrategy(strategy);
    }

    /**
     * Clears the global strategy so tests do not leak state to other modules.
     */
    @AfterEach
    void tearDown() {
        IStrategy.clean();
    }

    /**
     * Verifies dialect identification, identifier handling, and SQL terminator cleanup.
     */
    @Test
    void matchesYashanProductNameAndKeepsIdentifiersUnquoted() {
        assertEquals("YashanDB", strategy.databaseDialect());
        assertEquals("magic_api_file", strategy.wrapIdentifier("magic_api_file"));
        assertEquals("hmx_lowcode.magic_api_file", strategy.concatWrapName("hmx_lowcode", "magic_api_file"));
        assertEquals("SELECT 1", strategy.wrapSql(" SELECT 1;;; "));
    }

    /**
     * Verifies that the SPI service file exposes the YashanDB strategy.
     */
    @Test
    void isRegisteredThroughSpi() {
        boolean registered = false;
        for (IStrategy<?, ?> candidate : ServiceLoader.load(IStrategy.class)) {
            if (candidate instanceof YashanStrategy) {
                registered = true;
            }
        }
        assertTrue(registered);
    }

    /**
     * Verifies Java-to-YashanDB type mappings and their immutability.
     */
    @Test
    void mapsCoreJavaTypes() {
        assertEquals("VARCHAR", strategy.typeMapping().get(String.class).getTypeName());
        assertEquals("BIGINT", strategy.typeMapping().get(Long.class).getTypeName());
        assertEquals("TINYINT", strategy.typeMapping().get(Boolean.class).getTypeName());
        assertEquals("DECIMAL", strategy.typeMapping().get(BigDecimal.class).getTypeName());
        assertEquals("BLOB", strategy.typeMapping().get(byte[].class).getTypeName());
        assertThrows(UnsupportedOperationException.class,
                () -> strategy.typeMapping().put(String.class, null));
    }

    /**
     * Verifies native YashanDB CREATE TABLE, index, and comment SQL generation.
     */
    @Test
    void buildsNativeYashanCreateSql() {
        DefaultTableMetadata table = tableMetadata();
        List<String> sql = strategy.createTable(table);

        assertTrue(sql.get(0).contains("CREATE TABLE hmx_lowcode.auto_table_case"));
        assertTrue(sql.get(0).contains("id BIGINT AUTO_INCREMENT"));
        assertTrue(sql.get(0).contains("amount DECIMAL(18,2)"));
        assertTrue(sql.get(0).contains("PRIMARY KEY (id)"));
        assertTrue(sql.stream().anyMatch(value -> value.equals(
                "CREATE UNIQUE INDEX auto_idx_case_code ON hmx_lowcode.auto_table_case (code DESC)")));
        assertTrue(sql.stream().anyMatch(value -> value.contains("COMMENT ON TABLE") && value.contains("case''s table")));
        assertTrue(sql.stream().noneMatch(value -> value.contains("\"")));
    }

    /**
     * Verifies generated index names are valid unquoted YashanDB identifiers.
     */
    @Test
    void normalizesGeneratedHashCharactersForUnquotedIdentifiers() {
        assertEquals("auto_idx_case_1_A_B", YashanIdentifierUtils.normalizeIndexName("auto_idx_case_1-A-B"));
    }

    /**
     * Verifies identity columns are rejected when they are not primary keys.
     */
    @Test
    void rejectsNonKeyAutoIncrementColumn() {
        ColumnMetadata column = column("counter", "BIGINT", null, null);
        column.setAutoIncrement(true);
        assertThrows(IllegalArgumentException.class, () -> YashanColumnSqlBuilder.buildSql(column));
    }

    /**
     * Verifies ALTER statements are emitted separately and without terminators.
     */
    @Test
    void buildsModifySqlAsSeparateStatements() {
        YashanCompareTableInfo changes = new YashanCompareTableInfo("case_table", "hmx_lowcode");
        changes.getDropIndexes().add("auto_idx_old");
        changes.getRenameColumns().put("old_name", "_deleted_old_name");
        changes.getNewColumns().add(column("enabled", "TINYINT", null, null)
                .setDefaultValueType(DefaultValueEnum.UNDEFINED).setDefaultValue("0"));
        changes.getColumnComments().put("enabled", "enabled flag");

        List<String> sql = strategy.modifyTable(changes);
        assertTrue(sql.contains("DROP INDEX hmx_lowcode.auto_idx_old"));
        assertTrue(sql.contains("ALTER TABLE hmx_lowcode.case_table RENAME COLUMN old_name TO _deleted_old_name"));
        assertTrue(sql.contains("ALTER TABLE hmx_lowcode.case_table ADD enabled TINYINT NULL DEFAULT 0"));
        assertTrue(sql.stream().allMatch(value -> !value.contains(";")));
    }

    /**
     * Creates representative MySQL-originated metadata for SQL generation tests.
     */
    private static DefaultTableMetadata tableMetadata() {
        DefaultTableMetadata table = new DefaultTableMetadata(null, "auto_table_case", "hmx_lowcode", "case's table");
        ColumnMetadata id = column("id", "BIGINT", null, null).setPrimary(true).setAutoIncrement(true);
        ColumnMetadata code = column("code", "VARCHAR", 64, null).setNotNull(true).setComment("case code");
        ColumnMetadata amount = column("amount", "DECIMAL", 18, 2);
        IndexMetadata index = new IndexMetadata().setName("auto_idx_case_code").setType(IndexTypeEnum.UNIQUE)
                .setColumns(Collections.singletonList(
                        IndexMetadata.IndexColumnParam.newInstance("code", IndexSortTypeEnum.DESC)));
        table.setColumnMetadataList(Arrays.asList(id, code, amount));
        table.setIndexMetadataList(Collections.singletonList(index));
        return table;
    }

    /**
     * Creates a compact column metadata object for the test fixtures.
     */
    private static ColumnMetadata column(String name, String type, Integer length, Integer scale) {
        return new ColumnMetadata().setName(name)
                .setType(new DatabaseTypeAndLength(type, length, scale, Collections.emptyList()));
    }
}
