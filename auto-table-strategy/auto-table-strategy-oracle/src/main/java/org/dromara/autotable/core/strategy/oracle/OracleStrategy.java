package org.dromara.autotable.core.strategy.oracle;

import lombok.NonNull;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
import org.dromara.autotable.core.converter.TypeDefine;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.oracle.builder.OracleMetadataBuilder;
import org.dromara.autotable.core.strategy.oracle.builder.OracleSqlBuilder;
import org.dromara.autotable.core.strategy.oracle.data.OracleCompareTableInfo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OracleStrategy implements IStrategy<DefaultTableMetadata, OracleCompareTableInfo> {


    public static String databaseDialect = DatabaseDialect.Oracle;

    @Override
    public String databaseDialect() {
        return databaseDialect;
    }

    @Override
    public String sqlSeparator() {
        return "";
    }

    @Override
    public Map<Class<?>, DefaultTypeEnumInterface> typeMapping() {
        TypeDefine strType = TypeDefine.of("varchar2", 255);
        TypeDefine boolType = TypeDefine.ofNumber(1, 0);
        TypeDefine shortType = TypeDefine.ofNumber(5, 0);
        TypeDefine intType = TypeDefine.ofNumber(10, 0);
        TypeDefine longType = TypeDefine.ofNumber(19, 0);
        TypeDefine floatType = TypeDefine.of("binary_float");
        TypeDefine doubleType = TypeDefine.of("binary_double");
        TypeDefine bigDecimalType = TypeDefine.ofNumber(38, 18);
        TypeDefine timestampType = TypeDefine.of("timestamp");
        TypeDefine dateType = TypeDefine.of("date");


        return new HashMap<Class<?>, DefaultTypeEnumInterface>(32) {{
            put(String.class, strType);
            put(Character.class, strType);
            put(char.class, strType);

            put(Boolean.class, boolType);
            put(boolean.class, boolType);

            put(Short.class, shortType);
            put(short.class, shortType);

            put(Integer.class, intType);
            put(int.class, intType);

            put(BigInteger.class, longType);
            put(Long.class, longType);
            put(long.class, longType);

            put(Float.class, floatType);
            put(float.class, floatType);
            put(Double.class, doubleType);
            put(double.class, doubleType);
            put(BigDecimal.class, bigDecimalType);

            put(java.util.Date.class, timestampType);
            put(java.sql.Time.class, timestampType);
            put(java.sql.Date.class, dateType);
            put(java.sql.Timestamp.class, timestampType);
            put(java.time.LocalTime.class, timestampType);
            put(java.time.LocalDate.class, dateType);
            put(java.time.LocalDateTime.class, timestampType);


        }};
    }


    @Override
    public String dropTable(String schema, String tableName) {
        return String.format("DECLARE\n" +
                "    table_count INTEGER;\n" +
                "BEGIN\n" +
                "    SELECT COUNT(*) INTO table_count FROM user_tables WHERE table_name = '%s';\n" +
                "    IF table_count > 0 THEN\n" +
                "        EXECUTE IMMEDIATE 'DROP TABLE \"%s\"';\n" +
                "    END IF;\n" +
                "END;", tableName, tableName);
    }

    @Override
    public @NonNull DefaultTableMetadata analyseClass(Class<?> beanClass) {
        return new OracleMetadataBuilder().build(beanClass);
    }

    @Override
    public List<String> createTable(DefaultTableMetadata tableMetadata) {
        return OracleSqlBuilder.createTable(tableMetadata);
    }

    @Override
    public @NonNull OracleCompareTableInfo compareTable(DefaultTableMetadata tableMetadata) {
        return new OracleCompareTableInfo(tableMetadata.getTableName(), tableMetadata.getSchema());
    }

    @Override
    public List<String> modifyTable(OracleCompareTableInfo compareTableInfo) {
        return new ArrayList<>();
    }


}
