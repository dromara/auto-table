package org.dromara.autotable.strategy.doris;

import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.strategy.doris.data.DorisDefaultTypeEnum;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DorisTypeHelper {
    public static final Set<String> CHAR_STRING_TYPE = new HashSet<>(Arrays.asList(
            DorisDefaultTypeEnum.CHAR.getTypeName(),
            DorisDefaultTypeEnum.VARCHAR.getTypeName(),
            DorisDefaultTypeEnum.STRING.getTypeName(),
            DorisDefaultTypeEnum.IPv4.getTypeName(),
            DorisDefaultTypeEnum.IPv6.getTypeName()
    ));


    public static final Set<String> DATE_TIME_TYPE = new HashSet<>(Arrays.asList(
            DorisDefaultTypeEnum.DATE.getTypeName(),
            DorisDefaultTypeEnum.DATETIME.getTypeName()
    ));

    public static final Set<String> INTEGER_TYPE = new HashSet<>(Arrays.asList(
            DorisDefaultTypeEnum.TINYINT.getTypeName(),
            DorisDefaultTypeEnum.SMALLINT.getTypeName(),
            DorisDefaultTypeEnum.INT.getTypeName(),
            DorisDefaultTypeEnum.BIGINT.getTypeName(),
            DorisDefaultTypeEnum.LARGEINT.getTypeName()
    ));

    public static final Set<String> FLOAT_TYPE = new HashSet<>(Arrays.asList(
            DorisDefaultTypeEnum.FLOAT.getTypeName(),
            DorisDefaultTypeEnum.DOUBLE.getTypeName(),
            DorisDefaultTypeEnum.DECIMAL.getTypeName()
    ));

    public static String getFullType(DatabaseTypeAndLength databaseTypeAndLength) {
        return databaseTypeAndLength.getDefaultFullType();
    }

    public static boolean isCharString(DatabaseTypeAndLength databaseTypeAndLength) {
        return CHAR_STRING_TYPE.contains(databaseTypeAndLength.getType());
    }

    public static boolean isDateTime(DatabaseTypeAndLength databaseTypeAndLength) {
        return DATE_TIME_TYPE.contains(databaseTypeAndLength.getType());
    }

    public static boolean needStringCompatibility(DatabaseTypeAndLength databaseTypeAndLength) {
        return isCharString(databaseTypeAndLength) || isDateTime(databaseTypeAndLength);
    }

    public static boolean isBoolean(DatabaseTypeAndLength databaseTypeAndLength) {
        return DorisDefaultTypeEnum.BOOLEAN.getTypeName().equalsIgnoreCase(databaseTypeAndLength.getType());
    }

    public static boolean isNumber(DatabaseTypeAndLength databaseTypeAndLength) {
        return (INTEGER_TYPE.contains(databaseTypeAndLength.getType()) || FLOAT_TYPE.contains(databaseTypeAndLength.getType()));
    }


    public static boolean isFloatNumber(DatabaseTypeAndLength databaseTypeAndLength) {
        return FLOAT_TYPE.contains(databaseTypeAndLength.getType());
    }

    public static boolean isNoLengthNumber(DatabaseTypeAndLength databaseTypeAndLength) {
        return INTEGER_TYPE.contains(databaseTypeAndLength.getType());
    }
}
