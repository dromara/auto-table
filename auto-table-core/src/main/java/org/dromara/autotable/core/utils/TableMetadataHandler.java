package org.dromara.autotable.core.utils;

import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoIncrement;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.ColumnComment;
import org.dromara.autotable.annotation.ColumnDefault;
import org.dromara.autotable.annotation.ColumnName;
import org.dromara.autotable.annotation.ColumnNotNull;
import org.dromara.autotable.annotation.ColumnType;
import org.dromara.autotable.annotation.Ignore;
import org.dromara.autotable.annotation.Index;
import org.dromara.autotable.annotation.PrimaryKey;
import org.dromara.autotable.annotation.TableIndex;
import org.dromara.autotable.annotation.TableIndexes;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.AutoTableAnnotationFinder;
import org.dromara.autotable.core.AutoTableGlobalConfig;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author don
 */
public class TableMetadataHandler {

    /************               表相关                **************/

    /**
     * 获取表索引
     *
     * @param clazz 实体类class
     * @return 索引列表
     */
    public static List<TableIndex> getTableIndexes(Class<?> clazz) {
        List<TableIndex> tableIndices = new ArrayList<>();
        // 获取自定义的注解查找器
        AutoTableAnnotationFinder autoTableAnnotationFinder = AutoTableGlobalConfig.getAutoTableAnnotationFinder();
        TableIndexes tableIndexes = autoTableAnnotationFinder.find(clazz, TableIndexes.class);
        if (tableIndexes != null) {
            Collections.addAll(tableIndices, tableIndexes.value());
        }
        TableIndex tableIndex = autoTableAnnotationFinder.find(clazz, TableIndex.class);
        if (tableIndex != null) {
            tableIndices.add(tableIndex);
        }
        return tableIndices;
    }

    /**
     * 获取bean上的dialect
     *
     * @param clazz bean
     * @return dialect
     */
    public static String getTableDialect(Class<?> clazz) {

        AutoTableAnnotationFinder autoTableAnnotationFinder = AutoTableGlobalConfig.getAutoTableAnnotationFinder();
        AutoTable autoTable = autoTableAnnotationFinder.find(clazz, AutoTable.class);
        if (autoTable != null && StringUtils.hasText(autoTable.dialect())) {
            return autoTable.dialect();
        }

        // 调用第三方实现
        return AutoTableGlobalConfig.getAutoTableMetadataAdapter().getTableDialect(clazz);
    }

    /**
     * 获取bean上的schema
     *
     * @param clazz bean
     * @return schema
     */
    public static String getTableSchema(Class<?> clazz) {

        AutoTableAnnotationFinder autoTableAnnotationFinder = AutoTableGlobalConfig.getAutoTableAnnotationFinder();
        AutoTable autoTable = autoTableAnnotationFinder.find(clazz, AutoTable.class);
        if (autoTable != null && StringUtils.hasText(autoTable.schema())) {
            return autoTable.schema();
        }

        // 调用第三方实现
        return AutoTableGlobalConfig.getAutoTableMetadataAdapter().getTableSchema(clazz);
    }

    /**
     * 获取bean上的表名
     *
     * @param clazz bean
     * @return 表名
     */
    public static String getTableName(Class<?> clazz) {

        AutoTableAnnotationFinder autoTableAnnotationFinder = AutoTableGlobalConfig.getAutoTableAnnotationFinder();

        AutoTable autoTable = autoTableAnnotationFinder.find(clazz, AutoTable.class);
        if (autoTable != null && StringUtils.hasText(autoTable.value())) {
            return autoTable.value();
        }

        // 调用第三方实现
        String tableName = AutoTableGlobalConfig.getAutoTableMetadataAdapter().getTableName(clazz);
        if (StringUtils.hasText(tableName)) {
            return tableName;
        }

        // 兜底，使用下划线命名
        return StringUtils.camelToUnderline(clazz.getSimpleName());
    }

    /**
     * 获取bean上的表注释
     *
     * @param clazz bean
     * @return 表注释
     */
    public static String getTableComment(Class<?> clazz) {

        AutoTable autoTable = AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(clazz, AutoTable.class);
        if (autoTable != null && StringUtils.hasText(autoTable.comment())) {
            return replaceSingleQuote(autoTable.comment());
        }

        // 调用第三方实现
        String adapterTableComment = AutoTableGlobalConfig.getAutoTableMetadataAdapter().getTableComment(clazz);
        if (StringUtils.hasText(adapterTableComment)) {
            return replaceSingleQuote(adapterTableComment);
        }

        return null;
    }


    /************               字段相关                **************/
    public static boolean isIncludeField(Field field, Class<?> clazz) {

        Ignore ignore = AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(field, Ignore.class);
        if (ignore != null) {
            return false;
        }

        // 调用第三方实现
        Boolean isIgnoreField = AutoTableGlobalConfig.getAutoTableMetadataAdapter().isIgnoreField(field, clazz);
        if(isIgnoreField != null) {
            return !isIgnoreField;
        }

        // 所有字段均不被排除
        return true;
    }

    public static boolean isPrimary(Field field, Class<?> clazz) {

        PrimaryKey isPrimary = AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(field, PrimaryKey.class);
        if (isPrimary != null) {
            return true;
        }

        // 调用第三方实现
        Boolean primary = AutoTableGlobalConfig.getAutoTableMetadataAdapter().isPrimary(field, clazz);
        if (primary != null) {
            return primary;
        }

        return false;
    }

    public static boolean isAutoIncrement(Field field, Class<?> clazz) {

        AutoIncrement autoIncrement = AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(field, AutoIncrement.class);
        if (autoIncrement != null) {
            return autoIncrement.value();
        }

        // 调用第三方实现（因为布尔值在注解中无法存在true和false以外的值，所以不知道用户是否填写了值还是默认值，所以，先获取第三方自定义值）
        Boolean isAutoIncrement = AutoTableGlobalConfig.getAutoTableMetadataAdapter().isAutoIncrement(field, clazz);
        if (isAutoIncrement != null) {
            return isAutoIncrement;
        }

        PrimaryKey isPrimary = AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(field, PrimaryKey.class);
        if (isPrimary != null) {
            return isPrimary.autoIncrement();
        }

        return false;
    }

    public static Boolean isNotNull(Field field, Class<?> clazz) {
        // 主键默认为非空
        if (isPrimary(field, clazz)) {
            return true;
        }

        // 自增默认为非null
        if (isAutoIncrement(field, clazz)) {
            return true;
        }

        ColumnNotNull column = AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(field, ColumnNotNull.class);
        if (column != null) {
            return column.value();
        }
        // 调用第三方实现（因为布尔值在注解中无法存在true和false以外的值，所以不知道用户是否填写了值还是默认值，所以，先获取第三方自定义值）
        Boolean notNull = AutoTableGlobalConfig.getAutoTableMetadataAdapter().isNotNull(field, clazz);
        if (notNull != null) {
            return notNull;
        }
        AutoColumn autoColumn = AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(field, AutoColumn.class);
        if (autoColumn != null) {
            return autoColumn.notNull();
        }
        return false;
    }

    /**
     * 获取字段类型
     *
     * @param field 字段
     * @param clazz 实体
     * @return 字段类型
     */
    public static ColumnType getColumnType(Field field, Class<?> clazz) {

        ColumnType columnType = AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(field, ColumnType.class);
        if (columnType != null) {
            return columnType;
        }

        AutoColumn autoColumn = AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(field, AutoColumn.class);
        if (autoColumn != null && (StringUtils.hasText(autoColumn.type()) || autoColumn.length() > 0 || autoColumn.decimalLength() > 0)) {
            return new ColumnType() {
                @Override
                public String value() {
                    return autoColumn.type();
                }

                @Override
                public int length() {
                    return autoColumn.length();
                }

                @Override
                public int decimalLength() {
                    return autoColumn.decimalLength();
                }

                @Override
                public String[] values() {
                    return new String[0];
                }

                @Override
                public Class<? extends Annotation> annotationType() {
                    return ColumnType.class;
                }
            };
        }

        // 调用第三方实现
        return AutoTableGlobalConfig.getAutoTableMetadataAdapter().getColumnType(field, clazz);
    }

    public static String getColumnComment(Field field, Class<?> clazz) {
        ColumnComment column = AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(field, ColumnComment.class);
        if (column != null) {
            return replaceSingleQuote(column.value());
        }
        AutoColumn autoColumn = AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(field, AutoColumn.class);
        if (autoColumn != null && StringUtils.hasText(autoColumn.comment())) {
            return replaceSingleQuote(autoColumn.comment());
        }

        // 调用第三方实现
        String adapterColumnComment = AutoTableGlobalConfig.getAutoTableMetadataAdapter().getColumnComment(field, clazz);
        if (StringUtils.hasText(adapterColumnComment)) {
            return replaceSingleQuote(adapterColumnComment);
        }

        return "";
    }

    /**
     * 替换字符串中的单引号为双单引号
     */
    public static String replaceSingleQuote(String input) {

        if (input == null || input.isEmpty()) {
            return input; // 空字符串或null直接返回
        }

        // 解决单引号引发的bug: https://gitee.com/dromara/auto-table/issues/IB9RJW
        return input.replace("'", "''");
    }

    public static ColumnDefault getColumnDefaultValue(Field field, Class<?> clazz) {
        ColumnDefault columnDefault = AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(field, ColumnDefault.class);
        if (columnDefault != null) {
            return columnDefault;
        }
        AutoColumn autoColumn = AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(field, AutoColumn.class);
        if (autoColumn != null && (autoColumn.defaultValueType() != DefaultValueEnum.UNDEFINED || StringUtils.hasText(autoColumn.defaultValue()))) {
            return new ColumnDefault() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return ColumnDefault.class;
                }

                @Override
                public DefaultValueEnum type() {
                    return autoColumn.defaultValueType();
                }

                @Override
                public String value() {
                    return autoColumn.defaultValue();
                }
            };
        }
        // 调用第三方实现
        return AutoTableGlobalConfig.getAutoTableMetadataAdapter().getColumnDefaultValue(field, clazz);
    }

    public static Index getIndex(Field field) {
        return AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(field, Index.class);
    }

    /**
     * 根据注解顺序和配置，获取字段对应的数据库字段名
     *
     * @param clazz bean
     * @param field 字段
     * @return 字段名
     */
    public static String getColumnName(Class<?> clazz, Field field) {

        ColumnName columnNameAnno = AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(field, ColumnName.class);
        if (columnNameAnno != null) {
            return columnNameAnno.value();
        }
        AutoColumn autoColumn = AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(field, AutoColumn.class);
        if (autoColumn != null && StringUtils.hasText(autoColumn.value())) {
            return autoColumn.value();
        }

        // 调用第三方实现
        String realColumnName = AutoTableGlobalConfig.getAutoTableMetadataAdapter().getColumnName(clazz, field);
        if (StringUtils.hasText(realColumnName)) {
            return realColumnName;
        }

        // 兜底
        return StringUtils.camelToUnderline(field.getName());
    }

    /**
     * 根据注解顺序和配置，获取字段对应的数据库字段名
     *
     * @param beanClazz bean class
     * @param fieldName 字段名
     * @return 字段名
     */
    public static String getColumnName(Class<?> beanClazz, String fieldName) {

        Field field = BeanClassUtil.getField(beanClazz, fieldName);
        return getColumnName(beanClazz, field);
    }
}
