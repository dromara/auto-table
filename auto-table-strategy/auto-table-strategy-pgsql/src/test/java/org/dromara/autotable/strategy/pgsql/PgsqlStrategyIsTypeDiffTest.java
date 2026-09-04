package org.dromara.autotable.strategy.pgsql;

import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.strategy.pgsql.data.dbdata.PgsqlDbColumn;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PgsqlStrategy.isTypeDiff 类型比较规则测试。
 * <p>核心场景：实体 {@code @AutoColumn(type="timestamp", length=0)} 与 DB {@code timestamp(0)}
 * 必须判为一致，否则每次启动都会触发无意义的 ALTER COLUMN TYPE。</p>
 */
public class PgsqlStrategyIsTypeDiffTest {

    private boolean isTypeDiff(String type, Integer length, Integer decimalLength, PgsqlDbColumn dbColumn) throws Exception {
        PgsqlStrategy strategy = new PgsqlStrategy();
        Method m = PgsqlStrategy.class.getDeclaredMethod("isTypeDiff", ColumnMetadata.class, PgsqlDbColumn.class);
        m.setAccessible(true);
        ColumnMetadata columnMetadata = new ColumnMetadata()
                .setType(new DatabaseTypeAndLength(type, length, decimalLength, null));
        return (boolean) m.invoke(strategy, columnMetadata, dbColumn);
    }

    private PgsqlDbColumn dbColumn(String udtName, String datetimePrecision) {
        PgsqlDbColumn c = new PgsqlDbColumn();
        c.setUdtName(udtName);
        c.setDatetimePrecision(datetimePrecision);
        return c;
    }

    private PgsqlDbColumn dbColumn(String udtName, String characterMaximumLength, String numericPrecision, String numericScale) {
        PgsqlDbColumn c = new PgsqlDbColumn();
        c.setUdtName(udtName);
        c.setCharacterMaximumLength(characterMaximumLength);
        c.setNumericPrecision(numericPrecision);
        c.setNumericScale(numericScale);
        return c;
    }

    @Test
    void test时间类型_实体指定长度_与DB精度一致() throws Exception {
        // 问题场景：实体 timestamp(0) vs DB timestamp(0) → 一致，不再每次启动反复 ALTER
        assertFalse(isTypeDiff("timestamp", 0, null, dbColumn("timestamp", "0")));
        assertFalse(isTypeDiff("timestamp", 6, null, dbColumn("timestamp", "6")));
        assertFalse(isTypeDiff("time", 3, null, dbColumn("time", "3")));
    }

    @Test
    void test时间类型_实体指定长度_与DB精度不同() throws Exception {
        assertTrue(isTypeDiff("timestamp", 0, null, dbColumn("timestamp", "6")));
        assertTrue(isTypeDiff("timestamp", 3, null, dbColumn("timestamp", "0")));
    }

    @Test
    void test时间类型_实体未指定长度_忽略DB精度() throws Exception {
        // 实体未指定长度视为"使用数据库默认"，忽略 DB 侧精度差异，避免无意义 ALTER
        assertFalse(isTypeDiff("timestamp", null, null, dbColumn("timestamp", "6")));
        assertFalse(isTypeDiff("timestamp", null, null, dbColumn("timestamp", "0")));
    }

    @Test
    void test时间类型_类型名不同() throws Exception {
        assertTrue(isTypeDiff("timestamp", null, null, dbColumn("date", null)));
        assertTrue(isTypeDiff("date", null, null, dbColumn("timestamp", "6")));
    }

    @Test
    void test整数类型_保持startsWith规则() throws Exception {
        // 实体 int4 无长度 vs DB int4(32) → 一致（原有行为）
        assertFalse(isTypeDiff("int4", null, null, dbColumn("int4", null, "32", null)));
        // 类型名不同 → 变更
        assertTrue(isTypeDiff("int8", null, null, dbColumn("int4", null, "32", null)));
    }

    @Test
    void test字符串类型() throws Exception {
        assertFalse(isTypeDiff("varchar", 255, null, dbColumn("varchar", "255", null, null)));
        assertTrue(isTypeDiff("varchar", 100, null, dbColumn("varchar", "255", null, null)));
        // 实体有长度但 DB 无（不限长 varchar）→ 变更
        assertTrue(isTypeDiff("varchar", 255, null, dbColumn("varchar", null, null, null)));
    }

    @Test
    void test数值类型_实体只给精度_只比较精度() throws Exception {
        // 实体 numeric(10) vs DB numeric(10,0) → 一致，避免反复误判
        assertFalse(isTypeDiff("numeric", 10, null, dbColumn("numeric", null, "10", "0")));
        assertTrue(isTypeDiff("numeric", 12, null, dbColumn("numeric", null, "10", "0")));
        // 实体给全精度+标度 → 严格比较
        assertFalse(isTypeDiff("numeric", 10, 6, dbColumn("numeric", null, "10", "6")));
        assertTrue(isTypeDiff("numeric", 10, 2, dbColumn("numeric", null, "10", "6")));
    }

    @Test
    void test无长度类型() throws Exception {
        assertFalse(isTypeDiff("text", null, null, dbColumn("text", null, null, null)));
        assertFalse(isTypeDiff("bool", null, null, dbColumn("bool", null, null, null)));
        assertTrue(isTypeDiff("text", null, null, dbColumn("varchar", "255", null, null)));
    }
}
