package org.dromara.autotable.core.utils;

import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.core.AutoTableAnnotationFinder;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.config.PropertyConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
        PropertyConfig autoTableProperties = AutoTableGlobalConfig.instance().getAutoTableProperties();
        PropertyConfig.SuperInsertPosition superInsertPosition = autoTableProperties.getSuperInsertPosition();

        List<Field> fieldList = new ArrayList<>();
        getColumnFieldList(fieldList, beanClass, false, superInsertPosition == PropertyConfig.SuperInsertPosition.after, autoTableProperties.getStrictExtends());

        /* 处理自定义排序的情况 */
        // 指定排序的字段
        List<Field> sortFieldList = new ArrayList<>(Collections.nCopies(fieldList.size(), null));
        // 未指定排序的字段。sortFieldList + unSortFieldList = fieldList
        List<Field> unSortFieldList = new ArrayList<>();
        AutoTableAnnotationFinder autoTableAnnotationFinder = AutoTableGlobalConfig.instance().getAutoTableAnnotationFinder();
        for (Field field : fieldList) {
            AutoColumn autoColumn = autoTableAnnotationFinder.find(field, AutoColumn.class);
            // 记录指定排序的字段到指定位置
            if (autoColumn != null) {
                int sort = autoColumn.sort();
                Integer index = null;
                if (sort > 0) {
                    // 插入到开头
                    index = sort - 1;
                }
                if (sort < 0) {
                    // 负数，插入到末尾
                    index = fieldList.size() + sort;
                }
                if(index != null) {
                    if (index < 0 || index >= fieldList.size()) {
                        throw new RuntimeException(String.format("%s下的字段%s的排序配置错误，范围在[-%s~%s]之间", beanClass.getName(), field.getName(), fieldList.size(), fieldList.size()));
                    }
                    Field eleField = sortFieldList.get(index);
                    if(eleField != null) {
                        int existEleSort = autoTableAnnotationFinder.find(eleField, AutoColumn.class).sort();
                        throw new RuntimeException(String.format("%s下的字段%s(sort:%s)的排序配置错误，排序位置已经被%s(sort:%s)占用了", beanClass.getName(), field.getName(), sort, eleField.getName(), existEleSort));
                    }
                    sortFieldList.set(index, field);
                    continue;
                }
            }

            // 记录未指定排序的字段
            unSortFieldList.add(field);
        }

        // 将未指定排序的字段，按照空白顺序填充到指定排序的字段中
        for (int i = 0, j = 0; i < sortFieldList.size(); i++) {
            if(sortFieldList.get(i) == null) {
                sortFieldList.set(i, unSortFieldList.get(j++));
            }
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
