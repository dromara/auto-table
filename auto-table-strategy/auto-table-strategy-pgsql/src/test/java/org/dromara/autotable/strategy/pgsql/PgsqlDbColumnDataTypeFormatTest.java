package org.dromara.autotable.strategy.pgsql;

import org.dromara.autotable.strategy.pgsql.data.dbdata.PgsqlDbColumn;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PgsqlDbColumn 类型还原测试。
 * 验证 getDataTypeFormat() 输出与 core getDefaultFullType() 对齐。
 */
public class PgsqlDbColumnDataTypeFormatTest {

    private PgsqlDbColumn dbColumn(String udtName, String characterMaximumLength,
                                   String numericPrecision, String numericScale, String datetimePrecision) {
        PgsqlDbColumn c = new PgsqlDbColumn();
        c.setUdtName(udtName);
        c.setCharacterMaximumLength(characterMaximumLength);
        c.setNumericPrecision(numericPrecision);
        c.setNumericScale(numericScale);
        c.setDatetimePrecision(datetimePrecision);
        return c;
    }

    @Test
    void test整数类型_输出精度() {
        // information_schema 中 int 类型带 numeric_precision，保持原有输出
        assertEquals("int2(16)", dbColumn("int2", null, "16", null, null).getDataTypeFormat());
        assertEquals("int4(32)", dbColumn("int4", null, "32", null, null).getDataTypeFormat());
        assertEquals("int8(64)", dbColumn("int8", null, "64", null, null).getDataTypeFormat());
    }

    @Test
    void test数值类型_输出精度和标度() {
        assertEquals("numeric(10,6)", dbColumn("numeric", null, "10", "6", null).getDataTypeFormat());
        assertEquals("numeric(10,0)", dbColumn("numeric", null, "10", "0", null).getDataTypeFormat());
    }

    @Test
    void test字符串类型() {
        assertEquals("varchar(255)", dbColumn("varchar", "255", null, null, null).getDataTypeFormat());
        // 无长度（不限长）的 varchar
        assertEquals("varchar", dbColumn("varchar", null, null, null, null).getDataTypeFormat());
        assertEquals("char(10)", dbColumn("bpchar", "10", null, null, null).getDataTypeFormat());
        assertEquals("text", dbColumn("text", null, null, null, null).getDataTypeFormat());
    }

    @Test
    void test时间类型_带小数秒精度() {
        // timestamp/time 的小数秒精度存于 information_schema 的 datetime_precision，
        // 输出以对齐实体 @AutoColumn(length=n)，避免 isTypeDiff 反复误判触发无意义 ALTER
        assertEquals("timestamp(0)", dbColumn("timestamp", null, null, null, "0").getDataTypeFormat());
        assertEquals("timestamp(6)", dbColumn("timestamp", null, null, null, "6").getDataTypeFormat());
        assertEquals("time(3)", dbColumn("time", null, null, null, "3").getDataTypeFormat());
        assertEquals("timestamptz(2)", dbColumn("timestamptz", null, null, null, "2").getDataTypeFormat());
        assertEquals("timetz(1)", dbColumn("timetz", null, null, null, "1").getDataTypeFormat());
    }

    @Test
    void test时间类型_无精度() {
        // datetime_precision 缺失 → 裸类型
        assertEquals("timestamp", dbColumn("timestamp", null, null, null, null).getDataTypeFormat());
        // date 为固定精度类型，不带括号
        assertEquals("date", dbColumn("date", null, null, null, null).getDataTypeFormat());
    }
}
