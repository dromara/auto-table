package org.dromara.autotable.core.initdata;

import java.lang.reflect.Field;

public interface InitDataValueConverter {

    Object convert(Class<?> clazz, Field field, Object value);

    /**
     * 默认实现，期待你的PR拓展
     */
    class DefaultInitDataValueConverter implements InitDataValueConverter {
        public Object convert(Class<?> clazz, Field field, Object value) {

            // 枚举类型 转换为 枚举值
            if (value != null && value.getClass().isEnum()) {
                return convertEnum(clazz, field, (Enum<?>) value);
            }

            return value;
        }

        public String convertEnum(Class<?> clazz, Field field, Enum<?> value) {
            return value.name();
        }
    }
}
