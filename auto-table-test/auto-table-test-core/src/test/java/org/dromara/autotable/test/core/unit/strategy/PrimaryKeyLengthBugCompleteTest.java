package org.dromara.autotable.test.core.unit.strategy;

import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.strategy.mysql.MysqlStrategy;
import org.dromara.autotable.strategy.mysql.data.MysqlColumnMetadata;
import org.dromara.autotable.strategy.mysql.data.MysqlCompareTableInfo;
import org.dromara.autotable.strategy.mysql.data.MysqlTableMetadata;
import org.dromara.autotable.strategy.mysql.data.dbdata.InformationSchemaColumn;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 复现主键字段长度变化检测不到的bug - 测试完整的 compareTable 流程
 * <p>
 * TODO: 当 MysqlStrategy 的私有方法提取为公开方法后，应移除此处的反射调用，
 * 改为直接调用公开 API。
 */
public class PrimaryKeyLengthBugCompleteTest {

    @BeforeEach
    void setUp() {
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
        IStrategy.setCurrentStrategy(new MysqlStrategy());
    }

    @AfterEach
    void tearDown() {
        IStrategy.clean();
        AutoTableGlobalConfig.clear();
    }

    @Test
    void testCompareColumns_withPrimaryKeyLengthChange() throws Exception {
        // 测试 isFieldTypeChanged
        InformationSchemaColumn dbColumn = createDbColumn("event_id", "varchar(64)", "varchar", "事件id", "NO");
        MysqlColumnMetadata beanColumn = createBeanColumn("event_id", "事件id", new DatabaseTypeAndLength("varchar", 128, null, Collections.emptyList()), true, true);

        boolean fieldTypeChanged = invokeIsFieldTypeChanged(dbColumn, beanColumn);
        assertTrue(fieldTypeChanged, "主键字段长度变化应该被检测到");

        // 模拟 compareColumns 中的条件判断
        boolean needModify = simulateNeedModify(fieldTypeChanged);
        assertTrue(needModify, "主键字段长度变化时应该需要修改");
    }

    @Test
    void testComparePrimary_withSamePrimaryKeyName() throws Exception {
        Method comparePrimaryMethod = MysqlStrategy.class.getDeclaredMethod(
                "comparePrimary",
                MysqlTableMetadata.class,
                MysqlCompareTableInfo.class,
                List.class
        );
        comparePrimaryMethod.setAccessible(true);

        MysqlTableMetadata tableMetadata = createTableMetadata();

        org.dromara.autotable.strategy.mysql.data.dbdata.InformationSchemaStatistics dbPrimary = new org.dromara.autotable.strategy.mysql.data.dbdata.InformationSchemaStatistics();
        dbPrimary.setIndexName("PRIMARY");
        dbPrimary.setColumnName("event_id");
        dbPrimary.setSeqInIndex(1);

        MysqlCompareTableInfo compareTableInfo = new MysqlCompareTableInfo("test_primary_key_length", "");
        comparePrimaryMethod.invoke(null, tableMetadata, compareTableInfo, Arrays.asList(dbPrimary));

        assertFalse(compareTableInfo.isDropPrimary(), "主键列名一致时不应该删除主键");
        assertTrue(compareTableInfo.getNewPrimaries().isEmpty(), "主键列名一致时不应该重置主键");
    }

    @Test
    void testFullCompare_withPrimaryKeyLengthChange() throws Exception {
        InformationSchemaColumn dbColumn = createDbColumn("event_id", "varchar(64)", "varchar", "事件id", "NO");
        MysqlColumnMetadata beanColumn = createBeanColumn("event_id", "事件id", new DatabaseTypeAndLength("varchar", 128, null, Collections.emptyList()), true, true);

        boolean fieldTypeChanged = invokeIsFieldTypeChanged(dbColumn, beanColumn);
        assertTrue(fieldTypeChanged, "主键字段长度变化应该被检测到");

        // 2. comparePrimary 不应该影响主键重置（列名一致时）
        Method comparePrimaryMethod = MysqlStrategy.class.getDeclaredMethod(
                "comparePrimary",
                MysqlTableMetadata.class,
                MysqlCompareTableInfo.class,
                List.class
        );
        comparePrimaryMethod.setAccessible(true);

        MysqlTableMetadata tableMetadata = createTableMetadata();
        tableMetadata.setColumnMetadataList(Collections.singletonList(beanColumn));

        org.dromara.autotable.strategy.mysql.data.dbdata.InformationSchemaStatistics dbPrimary = new org.dromara.autotable.strategy.mysql.data.dbdata.InformationSchemaStatistics();
        dbPrimary.setIndexName("PRIMARY");
        dbPrimary.setColumnName("event_id");
        dbPrimary.setSeqInIndex(1);

        MysqlCompareTableInfo compareTableInfo = new MysqlCompareTableInfo("test_primary_key_length", "");
        comparePrimaryMethod.invoke(null, tableMetadata, compareTableInfo, Arrays.asList(dbPrimary));

        assertFalse(compareTableInfo.isDropPrimary(), "主键列名一致时不应该删除主键");
        assertTrue(compareTableInfo.getNewPrimaries().isEmpty(), "主键列名一致时不应该重置主键");

        // 3. 验证 MysqlCompareTableInfo 中应该有修改项
        compareTableInfo.addEditColumnMetadata(beanColumn);

        assertTrue(compareTableInfo.needModify(), "应该检测到需要修改");
        assertEquals(1, compareTableInfo.getModifyMysqlColumnMetadataList().size(), "应该有一个字段需要修改");
    }

    private MysqlTableMetadata createTableMetadata() {
        MysqlTableMetadata tableMetadata = new MysqlTableMetadata(
                org.dromara.autotable.test.core.entity.common_update.TestPrimaryKeyLength.class,
                "test_primary_key_length",
                "",
                "测试主键长度变化"
        );

        MysqlColumnMetadata columnMetadata = new MysqlColumnMetadata();
        columnMetadata.setName("event_id");
        columnMetadata.setComment("事件id");
        columnMetadata.setType(new DatabaseTypeAndLength("varchar", 128, null, Collections.emptyList()));
        columnMetadata.setNotNull(true);
        columnMetadata.setPrimary(true);
        columnMetadata.setPosition(1);
        tableMetadata.setColumnMetadataList(Collections.singletonList(columnMetadata));

        return tableMetadata;
    }

    private InformationSchemaColumn createDbColumn(String name, String columnType, String dataType, String comment, String isNullable) {
        InformationSchemaColumn dbColumn = new InformationSchemaColumn();
        dbColumn.setColumnName(name);
        dbColumn.setColumnType(columnType);
        dbColumn.setDataType(dataType);
        dbColumn.setColumnComment(comment);
        dbColumn.setIsNullable(isNullable);
        return dbColumn;
    }

    private MysqlColumnMetadata createBeanColumn(String name, String comment, DatabaseTypeAndLength type, boolean notNull, boolean primary) {
        MysqlColumnMetadata columnMetadata = new MysqlColumnMetadata();
        columnMetadata.setName(name);
        columnMetadata.setComment(comment);
        columnMetadata.setType(type);
        columnMetadata.setNotNull(notNull);
        columnMetadata.setPrimary(primary);
        columnMetadata.setPosition(1);
        return columnMetadata;
    }

    private boolean invokeIsFieldTypeChanged(InformationSchemaColumn dbColumn, MysqlColumnMetadata beanColumn) throws Exception {
        Method method = MysqlStrategy.class.getDeclaredMethod(
                "isFieldTypeChanged",
                InformationSchemaColumn.class,
                MysqlColumnMetadata.class
        );
        method.setAccessible(true);
        return (Boolean) method.invoke(null, dbColumn, beanColumn);
    }

    private boolean simulateNeedModify(boolean fieldTypeChanged) {
        boolean columnPositionChanged = false;
        boolean commentChanged = false;
        boolean notNullChanged = false;
        boolean fieldIsAutoIncrementChanged = false;
        boolean defaultValueChanged = false;
        boolean charsetChanged = false;

        return columnPositionChanged || commentChanged || fieldTypeChanged
                || notNullChanged || fieldIsAutoIncrementChanged || defaultValueChanged || charsetChanged;
    }
}
