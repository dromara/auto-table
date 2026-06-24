package org.dromara.autotable.strategy.sqlserver.data;

import org.dromara.autotable.core.converter.DatabaseTypeAndLength;

/**
 * SQLServer 类型判断辅助。
 *
 * @author don
 */
public class SqlServerTypeHelper {

    public static boolean isCharString(DatabaseTypeAndLength databaseTypeAndLength) {
        String type = databaseTypeAndLength.getType();
        return SqlServerDefaultTypeEnum.CHAR.getTypeName().equalsIgnoreCase(type) ||
                SqlServerDefaultTypeEnum.VARCHAR.getTypeName().equalsIgnoreCase(type) ||
                SqlServerDefaultTypeEnum.TEXT.getTypeName().equalsIgnoreCase(type);
    }

    public static boolean isBoolean(DatabaseTypeAndLength databaseTypeAndLength) {
        return SqlServerDefaultTypeEnum.BIT.getTypeName().equalsIgnoreCase(databaseTypeAndLength.getType());
    }

    public static boolean isTime(DatabaseTypeAndLength databaseTypeAndLength) {
        String type = databaseTypeAndLength.getType();
        return SqlServerDefaultTypeEnum.DATE.getTypeName().equalsIgnoreCase(type)
                || SqlServerDefaultTypeEnum.DATETIME2.getTypeName().equalsIgnoreCase(type)
                || SqlServerDefaultTypeEnum.TIME.getTypeName().equalsIgnoreCase(type);
    }
}
