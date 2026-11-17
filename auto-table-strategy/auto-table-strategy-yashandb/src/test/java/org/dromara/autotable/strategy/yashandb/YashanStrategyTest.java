package org.dromara.autotable.strategy.yashandb;

import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.PrimaryKey;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.annotation.enums.IndexSortTypeEnum;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.strategy.yashandb.builder.YashanColumnSqlBuilder;
import org.dromara.autotable.strategy.yashandb.data.YashanCompareTableInfo;
import org.dromara.autotable.strategy.yashandb.data.YashanTypeHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        AutoTableGlobalConfig.clear();
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
        // Comments are inline: COMMENT ON needs schema-owner privileges that a MySQL-style database grant lacks.
        assertTrue(sql.get(0).contains("code VARCHAR(64) NOT NULL COMMENT 'case code'"));
        assertTrue(sql.get(0).endsWith(") COMMENT 'case''s table'"));
        assertTrue(sql.stream().noneMatch(value -> value.startsWith("COMMENT ON")));
        assertTrue(sql.stream().noneMatch(value -> value.contains("\"")));
    }

    /**
     * Verifies MySQL boolean, unsigned, bit-literal and fractional-second handling.
     */
    @Test
    void normalizesMysqlBooleanUnsignedAndPrecisionTypes() {
        assertEquals("TINYINT", YashanColumnSqlBuilder.buildType(new DatabaseTypeAndLength("bit", 1, null, null)));
        assertEquals("BIT(8)", YashanColumnSqlBuilder.buildType(new DatabaseTypeAndLength("bit", 8, null, null)));
        // DATETIME stays DATETIME: a MySQL-mode TIMESTAMP NOT NULL gets an implicit ON UPDATE CURRENT_TIMESTAMP.
        assertEquals("DATETIME(3)", YashanColumnSqlBuilder.buildType(new DatabaseTypeAndLength("datetime", 3, null, null)));
        assertEquals("DATETIME", YashanColumnSqlBuilder.buildType(new DatabaseTypeAndLength("datetime", null, null, null)));
        assertEquals("DATETIME", strategy.typeMapping().get(LocalDateTime.class).getTypeName());
        // Catalog spellings of existing MySQL-mode columns compare equal to the model types.
        assertEquals(YashanTypeHelper.canonicalType("BIGINT"), YashanTypeHelper.canonicalType("BIGINT UNSIGNED"));
        assertEquals(YashanTypeHelper.canonicalType("bit"), YashanTypeHelper.canonicalType("TINYINT"));
        assertEquals(YashanTypeHelper.canonicalType("datetime"), YashanTypeHelper.canonicalType("TIMESTAMP(3)"));
        assertEquals("1", YashanTypeHelper.normalizeBitLiteral("b'1'"));
        assertEquals("0", YashanTypeHelper.normalizeBitLiteral("B'0'"));
        assertEquals("'b'", YashanTypeHelper.normalizeBitLiteral("'b'"));

        ColumnMetadata flag = column("enabled", "bit", 1, null).setNotNull(true)
                .setDefaultValueType(DefaultValueEnum.UNDEFINED).setDefaultValue("b'1'");
        assertEquals("enabled TINYINT NOT NULL DEFAULT 1", YashanColumnSqlBuilder.buildSql(flag));
    }

    /**
     * Verifies annotation defaults are quoted or converted like the MySQL strategy does.
     */
    @Test
    void normalizesAnnotationDefaultsLikeMysql() {
        AutoTableGlobalConfig.instance().addStrategy(strategy);
        DefaultTableMetadata table = strategy.analyseClass(DefaultsEntity.class);
        String ddl = strategy.createTable(table).get(0);
        assertTrue(ddl.contains("provider VARCHAR(20) NOT NULL DEFAULT 'EXA'"), ddl);
        assertTrue(ddl.contains("tiers VARCHAR(400) NOT NULL DEFAULT '[]'"), ddl);
        assertTrue(ddl.contains("quoted VARCHAR(20) NULL DEFAULT 'it''s'"), ddl);
        assertTrue(ddl.contains("enabled TINYINT NOT NULL DEFAULT 1"), ddl);
        assertTrue(ddl.contains("legacy_flag TINYINT NOT NULL DEFAULT 0"), ddl);
        assertTrue(ddl.contains("created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP"), ddl);
        assertTrue(ddl.contains("valid_from DATETIME NULL DEFAULT '2000-01-01 00:00:00'"), ddl);
        assertTrue(ddl.contains("expires_at DATETIME NOT NULL COMMENT"), ddl);
    }

    @AutoTable(value = "defaults_case", schema = "hmx_lowcode")
    static class DefaultsEntity {
        @PrimaryKey(autoIncrement = true)
        private Long id;
        @AutoColumn(length = 20, notNull = true, defaultValue = "EXA")
        private String provider;
        @AutoColumn(length = 400, notNull = true, defaultValue = "[]")
        private String tiers;
        @AutoColumn(length = 20, defaultValue = "'it''s'")
        private String quoted;
        @AutoColumn(notNull = true, defaultValue = "true")
        private Boolean enabled;
        @AutoColumn(type = "bit", length = 1, notNull = true, defaultValue = "b'0'")
        private Boolean legacyFlag;
        @AutoColumn(notNull = true, defaultValue = "CURRENT_TIMESTAMP")
        private LocalDateTime createdAt;
        @AutoColumn(defaultValue = "2000-01-01 00:00:00")
        private LocalDateTime validFrom;
        @AutoColumn(notNull = true, comment = "expiry")
        private LocalDateTime expiresAt;
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
                .setDefaultValueType(DefaultValueEnum.UNDEFINED).setDefaultValue("0").setComment("enabled flag"));
        changes.getModifyColumns().add(column("title", "VARCHAR", 64, null).setComment("it's a title"));
        changes.setComment("case table");
        changes.setTableCommentChanged(true);

        List<String> sql = strategy.modifyTable(changes);
        // MySQL compatibility mode rejects "DROP INDEX schema.index"; the owning table is required.
        assertTrue(sql.contains("DROP INDEX auto_idx_old ON hmx_lowcode.case_table"));
        assertTrue(sql.contains("ALTER TABLE hmx_lowcode.case_table RENAME COLUMN old_name TO _deleted_old_name"));
        assertTrue(sql.contains("ALTER TABLE hmx_lowcode.case_table ADD enabled TINYINT NULL DEFAULT 0 COMMENT 'enabled flag'"));
        assertTrue(sql.contains("ALTER TABLE hmx_lowcode.case_table MODIFY title VARCHAR(64) NULL COMMENT 'it''s a title'"));
        assertTrue(sql.contains("ALTER TABLE hmx_lowcode.case_table COMMENT 'case table'"));
        assertTrue(sql.stream().noneMatch(value -> value.startsWith("COMMENT ON")));
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
