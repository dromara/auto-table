package org.dromara.autotable.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 注解查找接口
 * @author don
 */
public interface AutoTableAnnotationFinder {

    /**
     * 查找注解
     *
     * @param clazz           查找的目标类
     * @param annotationClass 查找的注解
     * @param <A>             注解类型
     * @return 注解
     */
    default <A extends Annotation> A find(Class<?> clazz, Class<A> annotationClass) {
        return clazz.getDeclaredAnnotation(annotationClass);
    }

    /**
     * 查找注解
     *
     * @param method          查找的目标方法
     * @param annotationClass 查找的注解
     * @param <A>             注解类型
     * @return 注解
     */
    default <A extends Annotation> A find(Method method, Class<A> annotationClass) {
        return method.getDeclaredAnnotation(annotationClass);
    }

    /**
     * 查找注解
     *
     * @param field           查找的目标字段
     * @param annotationClass 查找的注解
     * @param <A>             注解类型
     * @return 注解
     */
    default <A extends Annotation> A find(Field field, Class<A> annotationClass) {
        return field.getDeclaredAnnotation(annotationClass);
    }

    /**
     * 判断是否存在注解
     *
     * @param clazz           查找的目标类
     * @param annotationClass 查找的注解
     * @param <A>             注解类型
     * @return 是否存在
     */
    default <A extends Annotation> boolean exist(Class<?> clazz, Class<A> annotationClass) {
        return find(clazz, annotationClass) != null;
    }

    /**
     * 判断是否存在注解
     *
     * @param method          查找的目标方法
     * @param annotationClass 查找的注解
     * @param <A>             注解类型
     * @return 是否存在
     */
    default <A extends Annotation> boolean exist(Method method, Class<A> annotationClass) {
        return find(method, annotationClass) != null;
    }

    /**
     * 判断是否存在注解
     *
     * @param field           查找的目标字段
     * @param annotationClass 查找的注解
     * @param <A>             注解类型
     * @return 是否存在
     */
    default <A extends Annotation> boolean exist(Field field, Class<A> annotationClass) {
        return find(field, annotationClass) != null;
    }
}
