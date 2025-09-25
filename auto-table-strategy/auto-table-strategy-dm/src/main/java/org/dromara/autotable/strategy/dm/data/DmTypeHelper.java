package org.dromara.autotable.strategy.dm.data;

/**
 * @author Min, Freddy
 * @date: 2025/2/25 22:34
 */

import org.dromara.autotable.core.converter.DatabaseTypeAndLength;

/**
 * 达梦类型判断工具
 */
public class DmTypeHelper {

    public static boolean isCharString(DatabaseTypeAndLength databaseTypeAndLength) {
        String type = databaseTypeAndLength.getType().toUpperCase();
        return type.equals(DmDefaultTypeEnum.CHAR.getTypeName()) ||
                type.equals(DmDefaultTypeEnum.VARCHAR.getTypeName()) ||
                type.equals(DmDefaultTypeEnum.VARCHAR2.getTypeName()) ||
                type.equals(DmDefaultTypeEnum.TEXT.getTypeName());
    }

    public static boolean isBoolean(DatabaseTypeAndLength databaseTypeAndLength) {
        return databaseTypeAndLength.getType().equalsIgnoreCase(DmDefaultTypeEnum.BIT.getTypeName());
    }

    public static boolean isTime(DatabaseTypeAndLength databaseTypeAndLength) {
        String type = databaseTypeAndLength.getType().toUpperCase();
        return type.equals(DmDefaultTypeEnum.DATE.getTypeName()) ||
                type.equals(DmDefaultTypeEnum.TIME.getTypeName()) ||
                type.equals(DmDefaultTypeEnum.TIMESTAMP.getTypeName()) ||
                type.equals(DmDefaultTypeEnum.DATETIME.getTypeName());
    }

    public static boolean isAutoIncrement(DatabaseTypeAndLength databaseTypeAndLength) {
        return databaseTypeAndLength.getType().equalsIgnoreCase(DmDefaultTypeEnum.SERIAL.getTypeName());
    }
}
