package org.dromara.autotable.core.utils;

import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.core.AutoTableAnnotationFinder;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.config.PropertyConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author don
 */
public class BeanClassUtil {

    /**
     * 查找类下指定的字段，如果当前类没有，那就去它的父类寻找
     *
     * @param clazz     类
     * @param fieldName 字段名
     * @return 字段
     */
    public static Field getField(Class<?> clazz, String fieldName) {

        Field field;
        while (true) {
            field = Arrays.stream(clazz.getDeclaredFields())
                    .filter(f -> f.getName().equals(fieldName))
                    .findFirst().orElse(null);
            // 如果没有找到
            if (field == null) {
                Class<?> superclass = clazz.getSuperclass();
                // 如果存在父类，且不是Object，则去上一级的父类继续寻找
                if (superclass != null && superclass != Object.class) {
                    clazz = superclass;
                    continue;
                }
            }
            break;
        }

        if (field == null) {
            throw new RuntimeException(String.format("%s上没有找到字段：%s（友情提示：请配置java字段名，而不是数据库列名）", clazz.getName(), fieldName));
        }

        return field;
    }

    /**
     * 查询某个类下所有的列的字段并排序
     *
     * @param beanClass 类class
     * @return 所有列的字段
     */
    public static List<Field> sortAllFieldForColumn(Class<?> beanClass) {

        // 获取父类追加到子类位置的配置
        PropertyConfig autoTableProperties = AutoTableGlobalConfig.getAutoTableProperties();
        PropertyConfig.SuperInsertPosition superInsertPosition = autoTableProperties.getSuperInsertPosition();

        List<Field> fieldList = new ArrayList<>();
        getColumnFieldList(fieldList, beanClass, false, superInsertPosition == PropertyConfig.SuperInsertPosition.after, autoTableProperties.getStrictExtends());

        /* 处理自定义排序的情况 */
        // 排序后的字段
        List<Field> sortFieldList = new ArrayList<>(fieldList.size());
        AutoTableAnnotationFinder autoTableAnnotationFinder = AutoTableGlobalConfig.getAutoTableAnnotationFinder();
        for (Field field : fieldList) {
            AutoColumn autoColumn = autoTableAnnotationFinder.find(field, AutoColumn.class);
            if (autoColumn != null) {
                int sort = autoColumn.sort();
                if (sort > 0) {
                    // 插入到开头
                    sortFieldList.add(sort - 1, field);
                    continue;
                }
                if (sort < 0) {
                    // 负数，插入到末尾
                    sortFieldList.add(fieldList.size() + sort, field);
                    continue;
                }
            }

            // 顺序不变
            sortFieldList.add(field);
        }

        return sortFieldList;
    }

    /**
     * 获取某个类下所有的字段
     *
     * @param fields           预先声明的集合
     * @param beanClass        指定类
     * @param parentInsertBack 是否追加到集合后面
     */
    private static void getColumnFieldList(List<Field> fields, Class<?> beanClass, boolean isParent, boolean parentInsertBack, boolean strictExtends) {

        Field[] declaredFields = beanClass.getDeclaredFields();
        // 获取当前class的所有fields的name列表
        Set<String> fieldNames = fields.stream().map(Field::getName).collect(Collectors.toSet());

        List<Field> newFields = Arrays.stream(declaredFields)
                // 避免重载属性
                .filter(field -> !fieldNames.contains(field.getName()))
                // 忽略静态变量
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                // 忽略final字段
                .filter(field -> !Modifier.isFinal(field.getModifiers()))
                // 父类字段，必须声明字段为protected或者public
                .filter(field -> !isParent || !strictExtends || Modifier.isProtected(field.getModifiers()) || Modifier.isPublic(field.getModifiers()))
                .collect(Collectors.toList());

        if (parentInsertBack) {
            fields.addAll(newFields);
        } else {
            fields.addAll(0, newFields);
        }

        Class<?> superclass = beanClass.getSuperclass();
        if (superclass != null) {
            getColumnFieldList(fields, superclass, true, parentInsertBack, strictExtends);
        }
    }
}
