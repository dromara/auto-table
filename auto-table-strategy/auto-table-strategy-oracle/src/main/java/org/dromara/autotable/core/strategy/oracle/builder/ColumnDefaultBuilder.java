package org.dromara.autotable.core.strategy.oracle.builder;

import org.dromara.autotable.core.strategy.oracle.data.OracleColumnMetadata;

import java.util.Objects;

public class ColumnDefaultBuilder {
    public static String build(OracleColumnMetadata column) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" DEFAULT ");
        String defaultValue = column.getDefaultValue();
        defaultValue = handle(defaultValue,column.getFieldType());
        stringBuilder.append(defaultValue);
        return stringBuilder.toString();
    }

    public static String handle(String defaultValue, Class<?> fieldType){
        if(fieldType== Boolean.class || fieldType == Boolean.TYPE) {
            defaultValue = Boolean.parseBoolean(defaultValue) ? "1" : "0";
            defaultValue = "'" + defaultValue + "'";
        }else if(CharSequence.class.isAssignableFrom(fieldType)){
            defaultValue = "'" + defaultValue + "'";
        }
        return defaultValue;
    }

    public static boolean isValid(String defaultValue){
        return defaultValue != null && !defaultValue.isEmpty();
    }

    public static String defStr(Class<?> fieldType,String defaultValue){
        if(defaultValue == null){
            return "";
        }
        if(isValid(defaultValue)){
            return "DEFAULT " + handle(defaultValue,fieldType);
        }else {
            return defaultValue;
        }
    }

    public static boolean equals(Class<?> fieldType,String defaultValue, String dataDefault) {
        return Objects.equals(handle(defaultValue,fieldType),handle(dataDefault,fieldType));
    }
}
