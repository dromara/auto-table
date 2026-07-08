package org.dromara.autotable.test.core.unit.strategy;

import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.RunMode;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.strategy.mysql.MysqlStrategy;
import org.dromara.autotable.strategy.mysql.data.MysqlColumnMetadata;
import org.dromara.autotable.strategy.mysql.data.MysqlTableMetadata;
import org.dromara.autotable.strategy.mysql.data.dbdata.InformationSchemaColumn;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 复现主键字段长度变化检测不到的bug - 测试compareColumns方法
 * <p>
 * TODO: 当 MysqlStrategy.isFieldTypeChanged 提取为公开方法或 MysqlTypeHelper 的静态方法后，
 * 应移除此处的反射调用，改为直接调用公开 API。
 */
public class PrimaryKeyLengthBugTest {

    @BeforeEach
    void setUp() {
        AutoTableGlobalConfig.instance().getAutoTableProperties().setMode(RunMode.create);
    }

    @AfterEach
    void tearDown() {
        AutoTableGlobalConfig.clear();
    }

    @Test
    void testIsFieldTypeChanged_withPrimaryKeyLengthChange() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method isFieldTypeChangedMethod = MysqlStrategy.class.getDeclaredMethod(
                "isFieldTypeChanged",
                InformationSchemaColumn.class,
                MysqlColumnMetadata.class
        );
        isFieldTypeChangedMethod.setAccessible(true);

        // 构造数据库中的列信息：varchar(64)
        InformationSchemaColumn dbColumn = new InformationSchemaColumn();
        dbColumn.setColumnName("event_id");
        dbColumn.setColumnType("varchar(64)");
        dbColumn.setDataType("varchar");
        dbColumn.setColumnComment("事件id");
        dbColumn.setIsNullable("NO");

        // 构造 Bean 上的列信息：varchar(64)
        MysqlColumnMetadata beanColumn = new MysqlColumnMetadata();
        beanColumn.setName("event_id");
        beanColumn.setComment("事件id");
        beanColumn.setType(new DatabaseTypeAndLength("varchar", 64, null, Collections.emptyList()));
        beanColumn.setNotNull(true);
        beanColumn.setPrimary(true);

        // 验证：长度一致时，isFieldTypeChanged 返回 false
        boolean result1 = (Boolean) isFieldTypeChangedMethod.invoke(null, dbColumn, beanColumn);
        assertFalse(result1, "长度一致时，不应该检测到变化");

        // 修改 Bean 上的长度为 128
        beanColumn.setType(new DatabaseTypeAndLength("varchar", 128, null, Collections.emptyList()));

        // 验证：长度变化时，isFieldTypeChanged 返回 true
        boolean result2 = (Boolean) isFieldTypeChangedMethod.invoke(null, dbColumn, beanColumn);
        assertTrue(result2, "长度变化时，应该检测到变化");
    }

    @Test
    void testCompareColumns_withPrimaryKeyLengthChange() throws Exception {
        // 构造 MysqlTableMetadata
        MysqlTableMetadata tableMetadata = new MysqlTableMetadata(
                org.dromara.autotable.test.core.entity.common_update.TestPrimaryKeyLength.class,
                "test_primary_key_length",
                "",
                "测试主键长度变化"
        );

        // 构造字段列表：主键字段 length=128
        MysqlColumnMetadata columnMetadata = new MysqlColumnMetadata();
        columnMetadata.setName("event_id");
        columnMetadata.setComment("事件id");
        columnMetadata.setType(new DatabaseTypeAndLength("varchar", 128, null, Collections.emptyList()));
        columnMetadata.setNotNull(true);
        columnMetadata.setPrimary(true);
        columnMetadata.setPosition(1);
        tableMetadata.setColumnMetadataList(Collections.singletonList(columnMetadata));

        // 构造数据库中的列列表：varchar(64)
        InformationSchemaColumn dbColumn = new InformationSchemaColumn();
        dbColumn.setColumnName("event_id");
        dbColumn.setColumnType("varchar(64)");
        dbColumn.setDataType("varchar");
        dbColumn.setColumnComment("事件id");
        dbColumn.setIsNullable("NO");
        dbColumn.setOrdinalPosition(1);

        // 测试 isFieldTypeChanged
        boolean fieldTypeChanged = invokeIsFieldTypeChanged(dbColumn, columnMetadata);
        assertTrue(fieldTypeChanged, "主键字段长度变化应该被检测到");

        // 模拟 compareColumns 中的条件判断
        boolean columnPositionChanged = false;
        boolean commentChanged = false;
        boolean notNullChanged = false;
        boolean fieldIsAutoIncrementChanged = false;
        boolean defaultValueChanged = false;
        boolean charsetChanged = false;
        boolean needModify = columnPositionChanged || commentChanged || fieldTypeChanged
                || notNullChanged || fieldIsAutoIncrementChanged || defaultValueChanged || charsetChanged;

        assertTrue(needModify, "主键字段长度变化时应该需要修改");
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
}
