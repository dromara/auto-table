package org.dromara.autotable.strategy.sqlserver;

import org.dromara.autotable.strategy.sqlserver.data.dbdata.SqlServerDbColumn;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SqlServerDbColumn 类型还原测试。
 * 验证 getDataTypeFormat() 输出与 core getDefaultFullType() 对齐。
 */
public class SqlServerDbColumnDataTypeFormatTest {

    private SqlServerDbColumn dbColumn(String dataType, Integer maxLength, Integer precision, Integer scale) {
        SqlServerDbColumn c = new SqlServerDbColumn();
        c.setDataType(dataType);
        c.setCharacterMaximumLength(maxLength);
        c.setNumericPrecision(precision);
        c.setNumericScale(scale);
        return c;
    }

    @Test
    void test整数类型_无长度() {
        assertEquals("bigint", dbColumn("bigint", null, null, null).getDataTypeFormat());
        assertEquals("int", dbColumn("int", null, null, null).getDataTypeFormat());
        assertEquals("smallint", dbColumn("smallint", null, null, null).getDataTypeFormat());
        assertEquals("tinyint", dbColumn("tinyint", null, null, null).getDataTypeFormat());
        assertEquals("bit", dbColumn("bit", null, null, null).getDataTypeFormat());
    }

    @Test
    void testNVARCHAR_字节长度除以2得字符数() {
        // NVARCHAR(255) 在 sys.columns 中 max_length = 510 字节
        assertEquals("nvarchar(255)", dbColumn("nvarchar", 510, null, null).getDataTypeFormat());
        assertEquals("nchar(100)", dbColumn("nchar", 200, null, null).getDataTypeFormat());
    }

    @Test
    void testVARCHAR_字节长度即字符数() {
        assertEquals("varchar(255)", dbColumn("varchar", 255, null, null).getDataTypeFormat());
        assertEquals("char(10)", dbColumn("char", 10, null, null).getDataTypeFormat());
    }

    @Test
    void testDECIMAL_始终输出precision和scale() {
        // 对齐 getDefaultFullType：decimalLength=0 时仍输出 ",0"
        assertEquals("decimal(19,4)", dbColumn("decimal", null, 19, 4).getDataTypeFormat());
        assertEquals("decimal(10,0)", dbColumn("decimal", null, 10, 0).getDataTypeFormat());
        assertEquals("numeric(10,0)", dbColumn("numeric", null, 10, 0).getDataTypeFormat());
    }

    @Test
    void test时间类型_无精度() {
        // scale=null（未指定）→ 裸类型，对齐实体未指定 length 时的 getDefaultFullType
        assertEquals("datetime2", dbColumn("datetime2", null, null, null).getDataTypeFormat());
        assertEquals("date", dbColumn("date", null, null, null).getDataTypeFormat());
        assertEquals("time", dbColumn("time", null, null, null).getDataTypeFormat());
    }

    @Test
    void test时间类型_带小数秒精度() {
        // datetime2/time/datetimeoffset 的小数秒精度存于 sys.columns.scale（0-7），输出以对齐实体 @ColumnType(length=n)
        assertEquals("datetime2(7)", dbColumn("datetime2", null, null, 7).getDataTypeFormat());
        assertEquals("datetime2(0)", dbColumn("datetime2", null, null, 0).getDataTypeFormat());
        assertEquals("time(3)", dbColumn("time", null, null, 3).getDataTypeFormat());
        assertEquals("datetimeoffset(6)", dbColumn("datetimeoffset", null, null, 6).getDataTypeFormat());
        // date / 旧式 datetime 为固定精度类型，不带括号
        assertEquals("date", dbColumn("date", null, null, 0).getDataTypeFormat());
        assertEquals("datetime", dbColumn("datetime", null, null, 3).getDataTypeFormat());
    }

    @Test
    void testMAX长度_返回无长度() {
        assertEquals("nvarchar", dbColumn("nvarchar", -1, null, null).getDataTypeFormat());
        assertEquals("varchar", dbColumn("varchar", -1, null, null).getDataTypeFormat());
    }

    @Test
    void testgetColumnDefaultWithoutParen_去外层括号() {
        SqlServerDbColumn c = new SqlServerDbColumn();
        c.setColumnDefault("(0)");
        assertEquals("0", c.getColumnDefaultWithoutParen());

        c.setColumnDefault("('abc')");
        assertEquals("'abc'", c.getColumnDefaultWithoutParen());

        c.setColumnDefault("(getdate())");
        assertEquals("getdate()", c.getColumnDefaultWithoutParen());

        c.setColumnDefault(null);
        assertNull(c.getColumnDefaultWithoutParen());
    }
}
