package org.dromara.autotable.adapter.mybatisflex;

import com.mybatisflex.annotation.Column;
import org.apache.ibatis.type.UnknownTypeHandler;
import org.dromara.autotable.core.converter.JavaTypeToDatabaseTypeConverter;

import java.lang.reflect.Field;

/**
 * MyBatis-Flex 字段类型转换器（零 Spring 依赖）。
 * <p>
 * 处理枚举类型和自定义 TypeHandler 场景。
 *
 * @author auto-table
 */
public class MybatisFlexJavaTypeToDatabaseTypeConverter implements JavaTypeToDatabaseTypeConverter {

    @Override
    public Class<?> getFieldType(Class<?> clazz, Field field) {
        // 枚举类型按 String 处理
        if (field.getType().isEnum()) {
            return String.class;
        }
        // 自定义 TypeHandler（非默认）：JSON 等复杂类型按 String 处理
        Column column = field.getAnnotation(Column.class);
        if (column != null && column.typeHandler() != UnknownTypeHandler.class) {
            return String.class;
        }
        return field.getType();
    }
}
