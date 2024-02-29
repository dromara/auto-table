package com.tangzc.autotable.core.strategy.mysql.data;

import com.tangzc.autotable.core.converter.DatabaseTypeAndLength;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author don
 */

public class MysqlTypeHelper {
    public static final Set<String> CHAR_STRING_TYPE = new HashSet<>(Arrays.asList(
            MySqlDefaultTypeEnum.CHAR.getTypeName(),
            MySqlDefaultTypeEnum.VARCHAR.getTypeName(),
            MySqlDefaultTypeEnum.TEXT.getTypeName(),
            MySqlDefaultTypeEnum.TINYTEXT.getTypeName(),
            MySqlDefaultTypeEnum.MEDIUMTEXT.getTypeName(),
            MySqlDefaultTypeEnum.LONGTEXT.getTypeName(),
            MySqlDefaultTypeEnum.ENUM.getTypeName(),
            MySqlDefaultTypeEnum.SET.getTypeName()
    ));

    public static final Set<String> ENUM_OR_SET_TYPE = new HashSet<>(Arrays.asList(
            MySqlDefaultTypeEnum.ENUM.getTypeName(),
            MySqlDefaultTypeEnum.SET.getTypeName()
    ));

    public static final Set<String> DATE_TIME_TYPE = new HashSet<>(Arrays.asList(
            MySqlDefaultTypeEnum.DATE.getTypeName(),
            MySqlDefaultTypeEnum.DATETIME.getTypeName(),
            MySqlDefaultTypeEnum.YEAR.getTypeName(),
            MySqlDefaultTypeEnum.TIME.getTypeName()
    ));

    public static final Set<String> INTEGER_TYPE = new HashSet<>(Arrays.asList(
            MySqlDefaultTypeEnum.INT.getTypeName(),
            MySqlDefaultTypeEnum.TINYINT.getTypeName(),
            MySqlDefaultTypeEnum.SMALLINT.getTypeName(),
            MySqlDefaultTypeEnum.MEDIUMINT.getTypeName(),
            MySqlDefaultTypeEnum.BIGINT.getTypeName()
    ));

    public static final Set<String> FLOAT_TYPE = new HashSet<>(Arrays.asList(
            MySqlDefaultTypeEnum.FLOAT.getTypeName(),
            MySqlDefaultTypeEnum.DOUBLE.getTypeName(),
            MySqlDefaultTypeEnum.DECIMAL.getTypeName()
    ));

    public static String getFullType(DatabaseTypeAndLength databaseTypeAndLength) {
        // 例：double(4,2) unsigned zerofill
        String typeAndLength = databaseTypeAndLength.getType();
        // 类型具备长度属性 且 自定义长度不为空
        Integer length = databaseTypeAndLength.getLength();
        Integer decimalLength = databaseTypeAndLength.getDecimalLength();
        if (MysqlTypeHelper.isEnum(databaseTypeAndLength)) {
            typeAndLength += "('" + String.join("','", databaseTypeAndLength.getValues()) + "')";
        } else if (length != null) {
            typeAndLength += "(" + length;
            if (decimalLength != null) {
                typeAndLength += "," + decimalLength;
            }
            typeAndLength += ")";
        }

        return typeAndLength;
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
        return MySqlDefaultTypeEnum.BIT.getTypeName().equalsIgnoreCase(databaseTypeAndLength.getType());
    }

    public static boolean isNumber(DatabaseTypeAndLength databaseTypeAndLength) {
        return (INTEGER_TYPE.contains(databaseTypeAndLength.getType()) || FLOAT_TYPE.contains(databaseTypeAndLength.getType()));
    }

    public static boolean isEnum(DatabaseTypeAndLength databaseTypeAndLength) {
        return ENUM_OR_SET_TYPE.contains(databaseTypeAndLength.getType());
    }

    public static boolean isFloatNumber(DatabaseTypeAndLength databaseTypeAndLength) {
        return FLOAT_TYPE.contains(databaseTypeAndLength.getType());
    }

    public static boolean isNoLengthNumber(DatabaseTypeAndLength databaseTypeAndLength) {
        return INTEGER_TYPE.contains(databaseTypeAndLength.getType());
    }
}
