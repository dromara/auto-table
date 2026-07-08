package org.dromara.autotable.adapter.mybatisplus.spring.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 注解深度合并处理类。
 * <p>
 * 继承 Spring 的 {@link AnnotatedElementUtils}，增加深度合并能力：
 * 收集元素上所有同类型注解（含 meta-annotation），逐属性比较默认值，
 * 将非默认值合并为一个注解实例。
 * <p>
 * 典型场景：自定义 {@code @Table("user")} 通过 {@code @AliasFor} 合并到
 * {@code @TableName}，本工具类可以正确处理这种跨层级的属性合并。
 *
 * @author don
 */
@Slf4j
public class AnnotatedElementUtilsPlus extends AnnotatedElementUtils {

    /**
     * 获取元素上指定注解的深度合并结果（包含 meta-annotation）
     */
    public static <ANNO extends Annotation> ANNO getDeepMergedAnnotation(AnnotatedElement element, Class<ANNO> annoClass) {
        final Set<ANNO> allMergedAnnotations = AnnotatedElementUtils.getAllMergedAnnotations(element, annoClass);
        return merge(annoClass, allMergedAnnotations);
    }

    /**
     * 查找元素上指定注解的深度合并结果（含继承层级）
     */
    public static <ANNO extends Annotation> ANNO findDeepMergedAnnotation(AnnotatedElement element, Class<ANNO> annoClass) {
        final Set<ANNO> allMergedAnnotations = AnnotatedElementUtils.findAllMergedAnnotations(element, annoClass);
        return merge(annoClass, allMergedAnnotations);
    }

    /**
     * 将多个同类型注解脱合并为一个：逐属性取第一个非默认值
     */
    public static <ANNO extends Annotation> ANNO merge(Class<ANNO> annoClass, Set<ANNO> allAnnotations) {
        if (allAnnotations == null || allAnnotations.isEmpty()) {
            return null;
        }
        try {
            Map<String, Object> annoDefaultAttributes = AnnotationDefaultValueHelper.getDefaultValues(annoClass);
            for (Map.Entry<String, Object> attribute : annoDefaultAttributes.entrySet()) {
                String annoFieldName = attribute.getKey();
                Object defaultVal = attribute.getValue();

                // 从所有注解中寻找第一个不同于默认值的值
                Object newVal = null;
                for (ANNO annotation : allAnnotations) {
                    try {
                        Object annoVal = annoClass.getMethod(annoFieldName).invoke(annotation);
                        if (compareValIsDiff(annoVal, defaultVal)) {
                            newVal = annoVal;
                        }
                    } catch (Exception e) {
                        log.warn("合并注解 {} 属性 {} 时出错", annoClass.getSimpleName(), annoFieldName, e);
                    }
                }
                if (newVal != null) {
                    annoDefaultAttributes.put(annoFieldName, newVal);
                }
            }
            return AnnotationDefaultValueHelper.createAnnotationInstance(annoClass, annoDefaultAttributes);
        } catch (Exception e) {
            log.warn("合并注解 {} 时获取默认值出错", annoClass.getSimpleName(), e);
            return null;
        }
    }

    private static boolean compareValIsDiff(Object val, Object defVal) {
        if (val == null) {
            return false;
        }
        if (defVal == null) {
            return true;
        }
        if (defVal.getClass().isArray()) {
            List<Object> list = Arrays.asList(val);
            List<Object> defList = Arrays.asList(defVal);
            if (!list.isEmpty() && list.size() == defList.size()) {
                return !list.containsAll(defList);
            }
            return !list.isEmpty();
        }
        return !Objects.equals(val, defVal);
    }
}
