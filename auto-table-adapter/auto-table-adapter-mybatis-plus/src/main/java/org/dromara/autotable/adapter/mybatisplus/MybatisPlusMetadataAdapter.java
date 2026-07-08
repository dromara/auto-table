package org.dromara.autotable.adapter.mybatisplus;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.Getter;
import org.dromara.autotable.annotation.ColumnDefault;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.AutoTableMetadataAdapter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * MyBatis-Plus 元数据适配器（零 Spring 依赖）。
 * <p>
 * 只读取 MP 原生注解（@TableName / @TableField / @TableId / @EnumValue），
 * 使用 Java 原生反射（{@link Class#getAnnotation}）而非 Spring 的 AnnotatedElementUtils。
 * <p>
 * 自定义注解（@Table / @Column / @ColumnId 等使用 @AliasFor 的注解）
 * 由 starter 模块的 {@code MybatisPlusExtendedMetadataAdapter} 扩展支持。
 *
 * @author auto-table
 */
public class MybatisPlusMetadataAdapter implements AutoTableMetadataAdapter {

    @Getter
    private final MybatisPlusAdapterConfig config;

    public MybatisPlusMetadataAdapter(MybatisPlusAdapterConfig config) {
        this.config = config;
    }

    @Override
    public Boolean isIgnoreField(Field field, Class<?> clazz) {
        // 1. 检查 @TableField.exist() = false
        TableField tableField = field.getAnnotation(TableField.class);
        if (tableField != null && !tableField.exist()) {
            return true;
        }
        // 2. 检查 @TableName.excludeProperty()
        TableName tableName = clazz.getAnnotation(TableName.class);
        if (tableName != null) {
            boolean excluded = Arrays.stream(tableName.excludeProperty())
                    .anyMatch(property -> property.equals(field.getName()));
            if (excluded) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean isPrimary(Field field, Class<?> clazz) {
        if (field.getAnnotation(TableId.class) != null) {
            return true;
        }
        // 默认 id 字段视为主键
        return "id".equals(field.getName());
    }

    @Override
    public Boolean isAutoIncrement(Field field, Class<?> clazz) {
        if (!isPrimary(field, clazz)) {
            return false;
        }
        TableId tableId = field.getAnnotation(TableId.class);
        return tableId != null && tableId.type() == IdType.AUTO;
    }

    @Override
    public List<String> getColumnEnumValues(Class<?> enumClassType) {
        if (!enumClassType.isEnum()) {
            throw new IllegalArgumentException(String.format("Class: %s 非枚举类型", enumClassType.getName()));
        }
        // 查找 @EnumValue 标注的字段
        Field valField = Arrays.stream(enumClassType.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(EnumValue.class))
                .findFirst()
                .orElse(null);
        if (valField != null) {
            valField.setAccessible(true);
            return Arrays.stream(enumClassType.getEnumConstants())
                    .map(enumConstant -> {
                        try {
                            return valField.get(enumConstant);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .map(Objects::toString)
                    .collect(Collectors.toList());
        }
        // 没有 @EnumValue，使用枚举 name()
        return Arrays.stream(enumClassType.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    @Override
    public String getTableName(Class<?> clazz) {
        String finalTableName = "";
        String tablePrefix = config.getTablePrefix();
        boolean addTablePrefix = hasText(tablePrefix);

        TableName tableNameAnno = clazz.getAnnotation(TableName.class);
        if (tableNameAnno != null && hasText(tableNameAnno.value())) {
            finalTableName = filterSpecialChar(tableNameAnno.value());
            if (addTablePrefix && !tableNameAnno.keepGlobalPrefix()) {
                addTablePrefix = false;
            }
        }
        if (!hasText(finalTableName)) {
            finalTableName = smartConvert(clazz.getSimpleName());
        }
        // 添加表前缀
        if (addTablePrefix) {
            finalTableName = tablePrefix + finalTableName;
        }
        return finalTableName;
    }

    @Override
    public String getTableSchema(Class<?> clazz) {
        TableName tableNameAnno = clazz.getAnnotation(TableName.class);
        if (tableNameAnno != null && hasText(tableNameAnno.schema())) {
            return tableNameAnno.schema();
        }
        return null;
    }

    @Override
    public String getColumnName(Class<?> clazz, Field field) {
        // 1. 优先读 @TableField.value()
        TableField tableField = field.getAnnotation(TableField.class);
        if (tableField != null && hasText(tableField.value()) && tableField.exist()) {
            return filterSpecialChar(tableField.value());
        }
        // 2. 其次读 @TableId.value()
        TableId tableId = field.getAnnotation(TableId.class);
        if (tableId != null && hasText(tableId.value())) {
            return filterSpecialChar(tableId.value());
        }
        // 3. 默认：字段名 + 驼峰转换
        return smartConvert(field.getName());
    }

    @Override
    public ColumnDefault getColumnDefaultValue(Field field, Class<?> clazz) {
        // 逻辑删除字段：当配置了 logicDeleteField 且当前字段匹配时，返回 logicNotDeleteValue 作为默认值
        boolean isLogicDeleteField = Objects.equals(config.getLogicDeleteField(), field.getName());
        String logicNotDeleteValue = config.getLogicNotDeleteValue();
        if (isLogicDeleteField && hasText(logicNotDeleteValue)) {
            final String value = logicNotDeleteValue;
            return new ColumnDefault() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }

                @Override
                public DefaultValueEnum type() {
                    return null;
                }

                @Override
                public String value() {
                    return value;
                }
            };
        }
        return AutoTableMetadataAdapter.super.getColumnDefaultValue(field, clazz);
    }

    // ===== 工具方法（protected 供 starter 扩展类继承使用）=====

    protected static boolean hasText(String str) {
        return str != null && !str.isEmpty() && str.trim().length() > 0;
    }

    protected static String filterSpecialChar(String name) {
        return name.replaceAll("`", "");
    }

    /**
     * 根据配置做驼峰转下划线 + 大写转换。
     * 使用 MP 原生 {@link StringUtils#camelToUnderline}（不依赖 Spring）。
     */
    protected String smartConvert(String column) {
        if (config.isMapUnderscoreToCamelCase()) {
            column = StringUtils.camelToUnderline(column);
        }
        if (config.isCapitalMode()) {
            column = column.toUpperCase();
        }
        return column;
    }
}
