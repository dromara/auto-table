package org.dromara.autotable.core.strategy.sqlite.data;

import org.dromara.autotable.core.converter.DatabaseTypeAndLength;

/**
 * @author don
 */
public class SqliteTypeHelper {

    public static String getFullType(DatabaseTypeAndLength databaseTypeAndLength, boolean isAutoIncrement) {
        // 如果是自增的主键，则不需要长度，否则sqlite报错
        if (isAutoIncrement) {
            return databaseTypeAndLength.getType();
        }

        return databaseTypeAndLength.getDefaultFullType();
    }

    public static boolean isText(DatabaseTypeAndLength type) {
        return SqliteDefaultTypeEnum.TEXT.getTypeName().equalsIgnoreCase(type.getType());
    }

    public static boolean isInteger(DatabaseTypeAndLength type) {
        return SqliteDefaultTypeEnum.INTEGER.getTypeName().equalsIgnoreCase(type.getType());
    }
}
