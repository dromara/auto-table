package org.dromara.autotable.strategy.oracle;

import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OracleIdentifierConsistencyTest {

    private final OracleStrategy strategy = new OracleStrategy();

    @BeforeEach
    void setUp() {
        IStrategy.setCurrentStrategy(strategy);
        AutoTableGlobalConfig.instance().getAutoTableProperties().setAutoDropCustomIndex(true);
    }

    @AfterEach
    void tearDown() {
        IStrategy.clean();
        AutoTableGlobalConfig.clear();
    }

    @Test
    void createTableUsesTheSameQuotedSequenceNameForCreationAndReference() {
        DefaultTableMetadata table = table("XXL_JOB_LOG",
                column("ID", "NUMBER", null, true, true));

        List<String> sql = strategy.createTable(table);

        assertEquals("CREATE SEQUENCE \"auto_seq_XXL_JOB_LOG\"", sql.get(0));
        assertTrue(sql.get(1).contains("DEFAULT \"auto_seq_XXL_JOB_LOG\".NEXTVAL"));
    }

    @Test
    void identifierResolutionPrefersExactNamesAndFallsBackOnlyToLegacyUppercaseNames() {
        List<String> names = Arrays.asList("USERLOG", "UserLog", "userlog");

        assertEquals("UserLog", OracleIdentifierUtils.resolveExistingName(names, "UserLog"));
        assertEquals("USERLOG", OracleIdentifierUtils.resolveExistingName(
                Collections.singletonList("USERLOG"), "UserLog"));
        assertNull(OracleIdentifierUtils.resolveExistingName(
                Collections.singletonList("userlog"), "UserLog"));
    }

    @Test
    void compareAndModifyPreserveActualMixedCaseObjectNames() {
        OracleCompareTableInfo compare = new OracleCompareTableInfo("UserLog", null);
        compare.setActualTableName("USERLOG");
        compare.setActualSequenceName("AUTO_SEQ_USERLOG");
        compare.setHasSequence(true);
        compare.setNeedSequence(true);
        compare.setDeleteColumnList(Collections.singletonList("ObsoleteField"));
        compare.setRenameColumnMap(Collections.emptyMap());
        compare.setCreateColumnList(Collections.emptyList());
        compare.setUpdateColumnList(Collections.singletonList("\"userName\" VARCHAR2(100)"));
        compare.setUpdateColumnCommentList(Collections.emptyList());
        compare.setDeleteIndexList(Collections.singleton("Idx_UserName"));
        compare.setCreateIndexList(Collections.emptyList());

        assertEquals(Collections.singletonList("ObsoleteField"), compare.getDeleteColumnList());
        assertEquals(Collections.singleton("Idx_UserName"), compare.getDeleteIndexList());
        assertEquals("AUTO_SEQ_USERLOG", compare.getActualSequenceName());
        assertTrue(compare.getUpdateColumnList().get(0).startsWith("\"userName\" VARCHAR2(100)"));

        List<String> modifySql = strategy.modifyTable(compare);
        assertTrue(modifySql.contains("DROP INDEX \"Idx_UserName\""));
        assertTrue(modifySql.stream().anyMatch(it -> it.startsWith(
                "ALTER TABLE \"USERLOG\" MODIFY (\"userName\" VARCHAR2(100)")));
        assertFalse(modifySql.stream().anyMatch(it -> it.contains("DEFAULT \"auto_seq_UserLog\".NEXTVAL")));
    }

    @Test
    void sequenceDefaultComparisonSupportsQuotedOwnerAndLegacyUnquotedReferences() {
        assertTrue(OracleIdentifierUtils.isSequenceNextVal(
                "\"APP\".\"auto_seq_UserLog\".\"NEXTVAL\"", "auto_seq_UserLog"));
        assertTrue(OracleIdentifierUtils.isSequenceNextVal(
                "AUTO_SEQ_USERLOG.NEXTVAL", "AUTO_SEQ_USERLOG"));
        assertFalse(OracleIdentifierUtils.isSequenceNextVal(
                "\"AUTO_SEQ_USERLOG\".NEXTVAL", "auto_seq_UserLog"));
    }

    @Test
    void dropTableChecksExactNamesBeforeLegacyUppercaseNames() {
        String sql = strategy.dropTable(null, "UserLog");

        assertTrue(sql.contains("table_name = 'UserLog'"));
        assertTrue(sql.contains("DROP TABLE \"UserLog\""));
        assertTrue(sql.contains("table_name = 'USERLOG'"));
        assertTrue(sql.contains("DROP TABLE \"USERLOG\""));
        assertTrue(sql.contains("DROP SEQUENCE \"auto_seq_UserLog\""));
        assertTrue(sql.contains("DROP SEQUENCE \"AUTO_SEQ_USERLOG\""));
    }

    @Test
    void sequencePresenceDifferenceRequiresModification() {
        OracleCompareTableInfo missingSequence = emptyCompareInfo();
        missingSequence.setNeedSequence(true);
        missingSequence.setHasSequence(false);
        assertTrue(missingSequence.needModify());

        OracleCompareTableInfo redundantSequence = emptyCompareInfo();
        redundantSequence.setNeedSequence(false);
        redundantSequence.setHasSequence(true);
        assertTrue(redundantSequence.needModify());
    }

    private static DefaultTableMetadata table(String name, ColumnMetadata... columns) {
        return new DefaultTableMetadata(Object.class, name, null, "")
                .setColumnMetadataList(Arrays.asList(columns))
                .setIndexMetadataList(Collections.emptyList());
    }

    private static ColumnMetadata column(String name, String type, Integer length, boolean primary, boolean autoIncrement) {
        return new ColumnMetadata()
                .setName(name)
                .setType(new DatabaseTypeAndLength(type, length, null, null))
                .setPrimary(primary)
                .setAutoIncrement(autoIncrement);
    }

    private static OracleCompareTableInfo emptyCompareInfo() {
        OracleCompareTableInfo compare = new OracleCompareTableInfo("TestTable", null);
        compare.setCreateColumnList(Collections.emptyList());
        compare.setDeleteColumnList(Collections.emptyList());
        compare.setUpdateColumnList(Collections.emptyList());
        compare.setUpdateColumnCommentList(Collections.emptyList());
        compare.setCreateIndexList(Collections.emptyList());
        compare.setDeleteIndexList(Collections.emptySet());
        return compare;
    }

}
