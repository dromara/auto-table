package org.dromara.autotable.core.strategy.dm.data;

/**
 * @author Min, Freddy
 * @date: 2025/2/25 22:34
 */

import org.dromara.autotable.core.converter.DatabaseTypeAndLength;

/**
 * 达梦类型判断工具
 */
public class DamengTypeHelper {

    public static boolean isCharString(DatabaseTypeAndLength databaseTypeAndLength) {
        String type = databaseTypeAndLength.getType().toUpperCase();
        return type.equals(DamengDefaultTypeEnum.CHAR.getTypeName()) ||
                type.equals(DamengDefaultTypeEnum.VARCHAR.getTypeName()) ||
                type.equals(DamengDefaultTypeEnum.VARCHAR2.getTypeName()) ||
                type.equals(DamengDefaultTypeEnum.TEXT.getTypeName());
    }

    public static boolean isBoolean(DatabaseTypeAndLength databaseTypeAndLength) {
        return databaseTypeAndLength.getType().equalsIgnoreCase(DamengDefaultTypeEnum.BOOLEAN.getTypeName());
    }

    public static boolean isTime(DatabaseTypeAndLength databaseTypeAndLength) {
        String type = databaseTypeAndLength.getType().toUpperCase();
        return type.equals(DamengDefaultTypeEnum.DATE.getTypeName()) ||
                type.equals(DamengDefaultTypeEnum.TIME.getTypeName()) ||
                type.equals(DamengDefaultTypeEnum.TIMESTAMP.getTypeName()) ||
                type.equals(DamengDefaultTypeEnum.DATETIME.getTypeName());
    }

    public static boolean isAutoIncrement(DatabaseTypeAndLength databaseTypeAndLength) {
        return databaseTypeAndLength.getType().equalsIgnoreCase(DamengDefaultTypeEnum.SERIAL.getTypeName());
    }
}