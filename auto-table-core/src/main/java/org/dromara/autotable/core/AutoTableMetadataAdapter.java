package org.dromara.autotable.core;

import org.dromara.autotable.annotation.ColumnDefault;
import org.dromara.autotable.annotation.ColumnType;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

/**
 * @author don
 */
public interface AutoTableMetadataAdapter {

    /**
     * 获取数据库方言
     *
     * @param clazz 实体类
     * @return 数据库方言
     */
    default String getTableDialect(Class<?> clazz) {
        return null;
    }

    /**
     * 获取表schema
     *
     * @param clazz 实体类
     * @return 表schema
     */
    default String getTableSchema(Class<?> clazz) {
        return null;
    }


    /**
     * 获取表名
     *
     * @param clazz 实体类
     * @return 表名
     */
    default String getTableName(Class<?> clazz) {
        return null;
    }

    /**
     * 获取表注释
     *
     * @param clazz 实体类
     * @return 标注释
     */
    default String getTableComment(Class<?> clazz) {
        return null;
    }


    /**
     * 获取字段名
     *
     * @param clazz 实体类
     * @param field 字段
     * @return 字段名
     */
    default String getColumnName(Class<?> clazz, Field field) {
        return null;
    }

    /**
     * 获取字段注释
     *
     * @param field 字段
     * @param clazz 实体类
     * @return 字段注释
     */
    default String getColumnComment(Field field, Class<?> clazz) {
        return null;
    }

    /**
     * 获取字段类型
     *
     * @param field 字段
     * @param clazz 实体类
     * @return 字段类型
     */
    default ColumnType getColumnType(Field field, Class<?> clazz) {
        return null;
    }

    /**
     * 获取字段默认值
     *
     * @param field 字段
     * @param clazz 实体类
     * @return 字段默认值
     */
    default ColumnDefault getColumnDefaultValue(Field field, Class<?> clazz) {
        return null;
    }

    /**
     * 获取枚举值，默认是枚举的名字
     *
     * @param enumType 枚举类型
     * @return 该枚举下的所有追
     */
    default List<String> getColumnEnumValues(Class<?> enumType) {
        return Collections.emptyList();
    }

    /**
     * 拓展判断是否是忽略的字段
     *
     * @param field 字段
     * @param clazz 类
     * @return 是否忽略
     */
    default boolean isIgnoreField(Field field, Class<?> clazz) {
        // 默认所有字段均不被排除
        return false;
    }

    /**
     * 判断是否是主键
     *
     * @param field 字段
     * @param clazz 类
     * @return 是否是主键
     */
    default boolean isPrimary(Field field, Class<?> clazz) {
        // 默认不是主键
        return false;
    }

    /**
     * 判断是否是自增的主键
     *
     * @param field 字段
     * @param clazz 类
     * @return 是否是自增的主键
     */
    default boolean isAutoIncrement(Field field, Class<?> clazz) {
        // 默认都不是自增的
        return false;
    }

    /**
     * 获取字段是否非空
     *
     * @param field 字段
     * @param clazz 实体类
     * @return 字段是否非空
     */
    default boolean isNotNull(Field field, Class<?> clazz) {
        return false;
    }
}
