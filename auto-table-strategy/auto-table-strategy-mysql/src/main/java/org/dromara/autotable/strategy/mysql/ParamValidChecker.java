package org.dromara.autotable.strategy.mysql;

import org.dromara.autotable.annotation.ColumnType;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.strategy.mysql.data.MysqlColumnMetadata;
import org.dromara.autotable.strategy.mysql.data.MysqlTypeHelper;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * 参数校验器
 * @author don
 */
public class ParamValidChecker {

    /**
     * 自增与类型的匹配校验
     */
    private static final IColumnChecker CHECK_AUTO_INCREMENT = (clazz, field, columnParam) -> {
        DatabaseTypeAndLength columnType = columnParam.getType();
        if (columnParam.isAutoIncrement() && !MysqlTypeHelper.isNumber(columnType)) {
            return new RuntimeException(String.format("类(%s)的字段(%s[%s])设置了自增，但是匹配到的(%s)非数字类型，无法自增，请尝试通过@%s手动指定数据库类型或更换类的字段类型",
                    clazz.getName(), field.getName(), field.getType().getName(), columnType.getType(), ColumnType.class.getName()));
        }
        return null;
    };
    /**
     * 空字符默认值，只能在字段类型为字符串的时候设置
     */
    private static final IColumnChecker CHECK_DEFAULT_IS_EMPTY_STRING = (clazz, field, columnParam) -> {
        DatabaseTypeAndLength columnType = columnParam.getType();
        boolean defaultIsEmptyString = columnParam.getDefaultValueType() == DefaultValueEnum.EMPTY_STRING;
        if (defaultIsEmptyString && !MysqlTypeHelper.isCharString(columnType)) {
            return new RuntimeException(String.format("类(%s)的字段(%s[%s])设置了默认值为空字符，但是匹配到的(%s)非字符类型，请尝试通过@%s手动指定数据库类型或更换类的字段类型",
                    clazz.getName(), field.getName(), field.getType().getName(), columnType.getType(), ColumnType.class.getName()));
        }
        return null;
    };

    private static final List<IColumnChecker> COLUMN_PARAM_CHECKER_LIST = Arrays.asList(CHECK_AUTO_INCREMENT, CHECK_DEFAULT_IS_EMPTY_STRING);

    /**
     * 字段参数校验
     * @param clazz 类
     * @param field 字段
     * @param mysqlColumnMetadata 字段参数
     * @throws RuntimeException 校验不通过
     */
    public static void checkColumnParam(Class<?> clazz, Field field, MysqlColumnMetadata mysqlColumnMetadata) {
        for (IColumnChecker iColumnChecker : COLUMN_PARAM_CHECKER_LIST) {
            RuntimeException exception = iColumnChecker.check(clazz, field, mysqlColumnMetadata);
            if(exception != null) {
                throw exception;
            }
        }
    }

    /**
     * 字段参数校验
     */
    @FunctionalInterface
    public static interface IColumnChecker {
        /**
         * 校验
         * @param clazz 类
         * @param field 字段
         * @param mysqlColumnMetadata 字段参数
         * @return 异常信息
         */
        RuntimeException check(Class<?> clazz, Field field, MysqlColumnMetadata mysqlColumnMetadata);
    }
}
