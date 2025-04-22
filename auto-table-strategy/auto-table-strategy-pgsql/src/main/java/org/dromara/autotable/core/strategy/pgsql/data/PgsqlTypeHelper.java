package org.dromara.autotable.core.strategy.pgsql.data;

import org.dromara.autotable.core.converter.DatabaseTypeAndLength;

/**
 * @author don
 */
public class PgsqlTypeHelper {

    public static boolean isCharString(DatabaseTypeAndLength databaseTypeAndLength) {
        String type = databaseTypeAndLength.getType();
        return PgsqlDefaultTypeEnum.CHAR.getTypeName().equalsIgnoreCase(type) ||
                PgsqlDefaultTypeEnum.VARCHAR.getTypeName().equalsIgnoreCase(type) ||
                PgsqlDefaultTypeEnum.TEXT.getTypeName().equalsIgnoreCase(type);
    }

    public static boolean isBoolean(DatabaseTypeAndLength databaseTypeAndLength) {
        return PgsqlDefaultTypeEnum.BOOL.getTypeName().equalsIgnoreCase(databaseTypeAndLength.getType());
    }

    public static boolean isTime(DatabaseTypeAndLength databaseTypeAndLength) {
        String type = databaseTypeAndLength.getType();
        return PgsqlDefaultTypeEnum.DATE.getTypeName().equalsIgnoreCase(type)
                || PgsqlDefaultTypeEnum.TIMESTAMP.getTypeName().equalsIgnoreCase(type)
                || PgsqlDefaultTypeEnum.TIME.getTypeName().equalsIgnoreCase(type);
    }
}
