package org.dromara.autotable.adapter.mybatisflex;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.EnumValue;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.RelationManyToMany;
import com.mybatisflex.annotation.RelationManyToOne;
import com.mybatisflex.annotation.RelationOneToMany;
import com.mybatisflex.annotation.RelationOneToOne;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.table.DynamicTableProcessor;
import com.mybatisflex.core.table.TableManager;
import lombok.Getter;
import org.dromara.autotable.core.AutoTableMetadataAdapter;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis-Flex 元数据适配器（零 Spring 依赖）。
 * <p>
 * 读取 MyBatis-Flex 原生注解（@Table / @Column / @Id / @EnumValue），
 * 使用 Java 原生反射（{@link Class#getAnnotation} / {@link Field#getAnnotation}）。
 *
 * @author auto-table
 */
public class MybatisFlexMetadataAdapter implements AutoTableMetadataAdapter {

    @Getter
    private final MybatisFlexAdapterConfig config;

    public MybatisFlexMetadataAdapter(MybatisFlexAdapterConfig config) {
        this.config = config;
    }

    @Override
    public Boolean isIgnoreField(Field field, Class<?> clazz) {
        Column column = field.getAnnotation(Column.class);
        if (column != null && column.ignore()) {
            return true;
        }
        // 关联关系字段自动忽略
        return field.getAnnotation(RelationOneToOne.class) != null
                || field.getAnnotation(RelationOneToMany.class) != null
                || field.getAnnotation(RelationManyToOne.class) != null
                || field.getAnnotation(RelationManyToMany.class) != null;
    }

    @Override
    public Boolean isPrimary(Field field, Class<?> clazz) {
        if (field.getAnnotation(Id.class) != null) {
            return true;
        }
        return "id".equals(field.getName());
    }

    @Override
    public Boolean isAutoIncrement(Field field, Class<?> clazz) {
        if (!isPrimary(field, clazz)) {
            return false;
        }
        Id id = field.getAnnotation(Id.class);
        if (id == null || id.keyType() == KeyType.None) {
            // 回退到全局配置
            try {
                FlexGlobalConfig.KeyConfig keyConfig = FlexGlobalConfig.getDefaultConfig().getKeyConfig();
                return Optional.ofNullable(keyConfig)
                        .map(FlexGlobalConfig.KeyConfig::getKeyType)
                        .orElse(KeyType.None) == KeyType.Auto;
            } catch (Exception e) {
                return false;
            }
        }
        return id.keyType() == KeyType.Auto;
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
                .map(e -> ((Enum<?>) e).name())
                .collect(Collectors.toList());
    }

    @Override
    public String getTableName(Class<?> clazz) {
        String tableName;
        Table table = clazz.getAnnotation(Table.class);
        if (table != null && hasText(table.value())) {
            tableName = table.value();
        } else {
            tableName = smartConvert(table, clazz.getSimpleName());
        }
        // 支持 mybatis-flex 的动态表名处理器
        DynamicTableProcessor dynamicTableProcessor = TableManager.getDynamicTableProcessor();
        if (dynamicTableProcessor != null) {
            tableName = dynamicTableProcessor.process(tableName);
        }
        return tableName;
    }

    @Override
    public String getColumnName(Class<?> clazz, Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null && hasText(column.value()) && !column.ignore()) {
            return filterSpecialChar(column.value());
        }
        Table table = clazz.getAnnotation(Table.class);
        return smartConvert(table, field.getName());
    }

    @Override
    public String getTableComment(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (table != null && hasText(table.comment())) {
            return table.comment();
        }
        return AutoTableMetadataAdapter.super.getTableComment(clazz);
    }

    @Override
    public String getTableSchema(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (table != null && hasText(table.schema())) {
            return table.schema();
        }
        return AutoTableMetadataAdapter.super.getTableSchema(clazz);
    }

    @Override
    public String getColumnComment(Field field, Class<?> clazz) {
        // 修复原项目 Bug: 从 field 读取，不是 clazz
        Column column = field.getAnnotation(Column.class);
        if (column != null && hasText(column.comment())) {
            return column.comment();
        }
        return AutoTableMetadataAdapter.super.getColumnComment(field, clazz);
    }

    // ===== 工具方法 =====

    private static boolean hasText(String str) {
        return str != null && !str.isEmpty() && str.trim().length() > 0;
    }

    private static String filterSpecialChar(String name) {
        return name.replaceAll("`", "");
    }

    /**
     * 根据 @Table 注解级配置和全局配置做驼峰转下划线。
     */
    private String smartConvert(Table table, String column) {
        boolean camelToUnderline;
        if (table == null) {
            camelToUnderline = config.isMapUnderscoreToCamelCase();
        } else {
            camelToUnderline = table.camelToUnderline();
        }
        if (camelToUnderline) {
            column = camelToUnderline(column);
        }
        return column;
    }

    /**
     * 驼峰转下划线（内联实现，不依赖外部工具类）。
     */
    static String camelToUnderline(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
