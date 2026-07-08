package org.dromara.autotable.adapter.mybatisplus.spring;

import org.dromara.autotable.adapter.mybatisplus.MybatisPlusAdapterConfig;
import org.dromara.autotable.adapter.mybatisplus.MybatisPlusMetadataAdapter;
import org.dromara.autotable.adapter.mybatisplus.spring.annotation.Column;
import org.dromara.autotable.adapter.mybatisplus.spring.annotation.ColumnId;
import org.dromara.autotable.adapter.mybatisplus.spring.annotation.Table;
import org.dromara.autotable.adapter.mybatisplus.spring.util.AnnotatedElementUtilsPlus;
import org.dromara.autotable.annotation.ColumnDefault;
import org.dromara.autotable.springboot.InitializeBeans;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;

/**
 * 扩展 adapter 的 MetadataAdapter，支持自定义注解（@Table/@Column/@ColumnId）。
 * <p>
 * 使用 {@link AnnotatedElementUtilsPlus} 处理 {@code @AliasFor} 深度合并逻辑。
 * 优先读取自定义注解，找不到时回退到 adapter 基础逻辑（读 MP 原生注解）。
 * <p>
 * 实现 {@link InitializeBeans} 确保在 {@code AutoTableAutoConfig} 构造器
 * 处理 {@code ObjectProvider<AutoTableMetadataAdapter>} 之前被创建。
 *
 * @author auto-table
 */
public class MybatisPlusExtendedMetadataAdapter extends MybatisPlusMetadataAdapter implements InitializeBeans {

    public MybatisPlusExtendedMetadataAdapter(MybatisPlusAdapterConfig config) {
        super(config);
    }

    @Override
    public Boolean isIgnoreField(Field field, Class<?> clazz) {
        // 检查自定义 @Column 是否标注了 exist=false（通过 meta-annotation @TableField）
        com.baomidou.mybatisplus.annotation.TableField tableFieldAnno =
                AnnotatedElementUtilsPlus.findDeepMergedAnnotation(field,
                        com.baomidou.mybatisplus.annotation.TableField.class);
        if (tableFieldAnno != null && !tableFieldAnno.exist()) {
            return true;
        }
        // 检查自定义 @Table.excludeProperty()
        Table tableAnno = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(clazz, Table.class);
        if (tableAnno != null) {
            for (String property : tableAnno.excludeProperty()) {
                if (property.equals(field.getName())) {
                    return true;
                }
            }
        }
        return super.isIgnoreField(field, clazz);
    }

    @Override
    public Boolean isPrimary(Field field, Class<?> clazz) {
        ColumnId columnIdAnno = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(field, ColumnId.class);
        if (columnIdAnno != null) {
            return true;
        }
        return super.isPrimary(field, clazz);
    }

    @Override
    public Boolean isAutoIncrement(Field field, Class<?> clazz) {
        if (!isPrimary(field, clazz)) {
            return false;
        }
        ColumnId columnIdAnno = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(field, ColumnId.class);
        if (columnIdAnno != null) {
            return columnIdAnno.mode() == com.baomidou.mybatisplus.annotation.IdType.AUTO;
        }
        return super.isAutoIncrement(field, clazz);
    }

    @Override
    public String getTableName(Class<?> clazz) {
        // 优先读取自定义 @Table 注解（通过 @AliasFor 合并到 @TableName）
        Table tableAnno = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(clazz, Table.class);
        if (tableAnno != null && StringUtils.hasText(tableAnno.value())) {
            String finalTableName = filterSpecialChar(tableAnno.value());
            String tablePrefix = getConfig().getTablePrefix();
            // 当配置了全局表前缀且未设置 keepGlobalPrefix 时，不添加前缀（与 MP 原生行为一致）
            boolean addTablePrefix = StringUtils.hasText(tablePrefix) && !tableAnno.keepGlobalPrefix();
            if (addTablePrefix) {
                finalTableName = tablePrefix + finalTableName;
            }
            return finalTableName;
        }
        return super.getTableName(clazz);
    }

    @Override
    public String getColumnName(Class<?> clazz, Field field) {
        // 优先读 @Column.value()（@Column 无 exist 属性，exist 检查在 isIgnoreField 中通过 @TableField 完成）
        Column columnAnno = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(field, Column.class);
        if (columnAnno != null && StringUtils.hasText(columnAnno.value())) {
            return filterSpecialChar(columnAnno.value());
        }
        // 优先读 @ColumnId.value()
        ColumnId columnIdAnno = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(field, ColumnId.class);
        if (columnIdAnno != null && StringUtils.hasText(columnIdAnno.value())) {
            return filterSpecialChar(columnIdAnno.value());
        }
        return super.getColumnName(clazz, field);
    }

    @Override
    public ColumnDefault getColumnDefaultValue(Field field, Class<?> clazz) {
        // 读取自定义 @Column.defaultValue()
        Column columnAnno = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(field, Column.class);
        if (columnAnno != null && StringUtils.hasText(columnAnno.defaultValue())) {
            final String value = columnAnno.defaultValue();
            return new ColumnDefault() {
                @Override
                public Class<? extends java.lang.annotation.Annotation> annotationType() {
                    return null;
                }

                @Override
                public org.dromara.autotable.annotation.enums.DefaultValueEnum type() {
                    return columnAnno.defaultValueType();
                }

                @Override
                public String value() {
                    return value;
                }
            };
        }
        return super.getColumnDefaultValue(field, clazz);
    }
}
