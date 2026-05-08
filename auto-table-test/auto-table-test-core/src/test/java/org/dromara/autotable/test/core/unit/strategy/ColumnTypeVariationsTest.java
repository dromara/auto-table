package org.dromara.autotable.test.core.unit.strategy;

import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.strategy.mysql.MysqlStrategy;
import org.dromara.autotable.strategy.mysql.data.MysqlColumnMetadata;
import org.dromara.autotable.strategy.mysql.data.dbdata.InformationSchemaColumn;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试不同 column_type 格式下的字段类型变化检测
 * <p>
 * TODO: 当 MysqlStrategy.isFieldTypeChanged 提取为公开方法后，
 * 应移除此处的反射调用，改为直接调用公开 API。
 */
public class ColumnTypeVariationsTest {

    @Test
    void testIsFieldTypeChanged_withExtraInfoInColumnType() throws Exception {
        Method isFieldTypeChangedMethod = MysqlStrategy.class.getDeclaredMethod(
                "isFieldTypeChanged",
                InformationSchemaColumn.class,
                MysqlColumnMetadata.class
        );
        isFieldTypeChangedMethod.setAccessible(true);

        // 测试1: column_type 包含额外信息，如字符集
        InformationSchemaColumn dbColumn1 = createDbColumn("event_id", "varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci", "varchar", "事件id", "NO");
        MysqlColumnMetadata beanColumn1 = createBeanColumn("event_id", "事件id", new DatabaseTypeAndLength("varchar", 128, null, Collections.emptyList()), true, true);

        boolean result1 = (Boolean) isFieldTypeChangedMethod.invoke(null, dbColumn1, beanColumn1);
        assertTrue(result1, "包含额外信息时，长度变化应该被检测到");

        // 测试2: column_type 只包含类型和长度
        InformationSchemaColumn dbColumn2 = createDbColumn("event_id", "varchar(64)", "varchar", "事件id", "NO");
        MysqlColumnMetadata beanColumn2 = createBeanColumn("event_id", "事件id", new DatabaseTypeAndLength("varchar", 128, null, Collections.emptyList()), true, true);

        boolean result2 = (Boolean) isFieldTypeChangedMethod.invoke(null, dbColumn2, beanColumn2);
        assertTrue(result2, "正常格式时，长度变化应该被检测到");

        // 测试3: 长度一致时
        InformationSchemaColumn dbColumn3 = createDbColumn("event_id", "varchar(64)", "varchar", "事件id", "NO");
        MysqlColumnMetadata beanColumn3 = createBeanColumn("event_id", "事件id", new DatabaseTypeAndLength("varchar", 64, null, Collections.emptyList()), true, true);

        boolean result3 = (Boolean) isFieldTypeChangedMethod.invoke(null, dbColumn3, beanColumn3);
        assertFalse(result3, "长度一致时，不应该检测到变化");
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
        MysqlColumnMetadata beanColumn = new MysqlColumnMetadata();
        beanColumn.setName(name);
        beanColumn.setComment(comment);
        beanColumn.setType(type);
        beanColumn.setNotNull(notNull);
        beanColumn.setPrimary(primary);
        return beanColumn;
    }
}
