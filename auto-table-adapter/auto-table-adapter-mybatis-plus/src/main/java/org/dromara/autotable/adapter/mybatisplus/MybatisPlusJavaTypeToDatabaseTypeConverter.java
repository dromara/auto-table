package org.dromara.autotable.adapter.mybatisplus;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import org.apache.ibatis.type.UnknownTypeHandler;
import org.dromara.autotable.core.converter.JavaTypeToDatabaseTypeConverter;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * MyBatis-Plus 字段类型转换器（零 Spring 依赖）。
 * <p>
 * 处理枚举类型（@EnumValue / IEnum）和自定义 TypeHandler 场景。
 * 使用 Java 原生反射读取 @TableField，不依赖 AnnotatedElementUtilsPlus。
 *
 * @author auto-table
 */
public class MybatisPlusJavaTypeToDatabaseTypeConverter implements JavaTypeToDatabaseTypeConverter {

    @Override
    public Class<?> getFieldType(Class<?> clazz, Field field) {
        // 1. 枚举类型：根据 MP 枚举方案确定入库类型
        if (field.getType().isEnum()) {
            return getEnumFieldSaveDbType(field.getType());
        }
        // 2. 自定义 TypeHandler（非默认）：JSON 等复杂类型按 String 处理
        TableField tableField = field.getAnnotation(TableField.class);
        if (tableField != null && tableField.typeHandler() != UnknownTypeHandler.class) {
            return String.class;
        }
        return field.getType();
    }

    /**
     * 获取枚举入库的数据库字段类型。
     * <p>
     * 如果使用了 MP 枚举方案（@EnumValue 或 IEnum），会有一个指定字段作为入库数据，
     * 数据类型为该字段的类型；否则默认按枚举 name() 入库，类型为 String。
     *
     * @param enumClassType 枚举类
     * @return 入库字段类型
     */
    private Class<?> getEnumFieldSaveDbType(Class<?> enumClassType) {
        if (!enumClassType.isEnum()) {
            throw new IllegalArgumentException(String.format("Class: %s 非枚举类型", enumClassType.getName()));
        }
        if (MybatisEnumTypeHandler.isMpEnums(enumClassType)) {
            // IEnum 实现类：取泛型参数类型
            if (IEnum.class.isAssignableFrom(enumClassType)) {
                return ReflectionKit.getSuperClassGenericType(enumClassType, IEnum.class, 0);
            }
            // @EnumValue 标注的字段：取该字段类型
            return Arrays.stream(enumClassType.getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(EnumValue.class))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("Could not find @EnumValue in Class: %s.", enumClassType.getName())))
                    .getType();
        }
        // 未使用 MP 枚举方案，按 String 处理
        return String.class;
    }
}
