package org.dromara.autotable.adapter.mybatisplus.spring.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 注解默认值构建助手。
 * 提供注解默认值获取、注解实例创建和修改能力，供 {@link AnnotatedElementUtilsPlus} 使用。
 *
 * @author don
 */
@Slf4j
public class AnnotationDefaultValueHelper {

    /**
     * 获取一个注解的默认值，同时提供一个修改默认值的回调函数
     */
    public static <A extends Annotation> A getAnnotationWithDefaultValues(
            Class<A> annotationType, @Nullable Consumer<Map<String, Object>> changeDefaultValues) {
        Map<String, Object> defaultValues = getDefaultValues(annotationType);
        if (changeDefaultValues != null) {
            changeDefaultValues.accept(defaultValues);
        }
        return createAnnotationInstance(annotationType, defaultValues);
    }

    /**
     * 获取注解所有属性的默认值
     */
    public static <A extends Annotation> Map<String, Object> getDefaultValues(Class<A> annotationType) {
        Map<String, Object> defaultValues = new HashMap<>();
        Method[] declaredMethods = annotationType.getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getParameterCount() == 0 && method.getReturnType() != void.class) {
                Object defaultValue = method.getDefaultValue();
                defaultValues.put(method.getName(), defaultValue);
            }
        }
        return defaultValues;
    }

    /**
     * 通过反射设置注解实例的属性值
     */
    public static void setAnnoVal(Annotation annotation, String fieldName, Object newVal) {
        InvocationHandler invocationHandler = Proxy.getInvocationHandler(annotation);
        try {
            Field valuesField = invocationHandler.getClass().getDeclaredField("valueCache");
            valuesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> memberValues = (Map<String, Object>) valuesField.get(invocationHandler);
            memberValues.put(fieldName, newVal);
        } catch (Exception e) {
            log.error("注解反射出错", e);
        }
    }

    /**
     * 创建注解实例，用指定值覆盖默认值
     */
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A createAnnotationInstance(Class<A> annotationType, Map<String, Object> values) {
        return (A) Proxy.newProxyInstance(annotationType.getClassLoader(), new Class[]{annotationType}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                if (values.containsKey(methodName)) {
                    return values.get(methodName);
                }
                return method.invoke(this, args);
            }
        });
    }

    /**
     * 基于已有注解实例，用指定值覆盖创建新实例
     */
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A updateAnnotationInstance(A annotation, Class<A> annotationType, Map<String, Object> values) {
        return (A) Proxy.newProxyInstance(annotationType.getClassLoader(), new Class[]{annotationType}, (proxy, method, args) -> {
            String methodName = method.getName();
            if (values.containsKey(methodName)) {
                return values.get(methodName);
            }
            return method.invoke(annotation, args);
        });
    }
}
