package com.tangzc.autotable.core.strategy.h2.data;

import com.tangzc.autotable.annotation.h2.H2TypeConstant;
import com.tangzc.autotable.core.converter.DatabaseTypeAndLength;

/**
 * @author don
 */
public class H2TypeHelper {

    public static boolean isCharString(DatabaseTypeAndLength databaseTypeAndLength) {
        String type = databaseTypeAndLength.getType();
        return H2DefaultTypeEnum.CHARACTER.getTypeName().equalsIgnoreCase(type) ||
                H2DefaultTypeEnum.VARCHAR_IGNORECASE.getTypeName().equalsIgnoreCase(type) ||
                H2DefaultTypeEnum.CHARACTER_VARYING.getTypeName().equalsIgnoreCase(type);
    }

    public static boolean isNumber(DatabaseTypeAndLength databaseTypeAndLength) {
        String type = databaseTypeAndLength.getType();
        return H2DefaultTypeEnum.INTEGER.getTypeName().equalsIgnoreCase(type) ||
                H2DefaultTypeEnum.BIGINT.getTypeName().equalsIgnoreCase(type) ||
                H2DefaultTypeEnum.SMARTINT.getTypeName().equalsIgnoreCase(type) ||
                H2DefaultTypeEnum.TINYINT.getTypeName().equalsIgnoreCase(type) ||
                H2DefaultTypeEnum.REAL.getTypeName().equalsIgnoreCase(type) ||
                H2DefaultTypeEnum.NUMERIC.getTypeName().equalsIgnoreCase(type);
    }

    public static boolean isNumber(String dataType) {
        return H2TypeConstant.INTEGER.equals(dataType) ||
                H2TypeConstant.BIGINT.equals(dataType) ||
                H2TypeConstant.SMARTINT.equals(dataType) ||
                H2TypeConstant.TINYINT.equals(dataType) ||
                H2TypeConstant.REAL.equals(dataType) ||
                H2TypeConstant.NUMERIC.equals(dataType);
    }
}
