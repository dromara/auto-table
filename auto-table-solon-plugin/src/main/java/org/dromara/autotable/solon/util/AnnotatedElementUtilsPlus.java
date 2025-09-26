package org.dromara.autotable.solon.util;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.utils.AnnotationDefaultValueHelper;
import org.dromara.autotable.core.utils.AnnotationMergeUtils;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.util.*;

/**
 * 注解合并处理类，可以将相同的注解的不同实例，中的属性合并为一个注解实例
 *
 * @author don, chengliang4810
 */
@Slf4j
public class AnnotatedElementUtilsPlus {

    public static <ANNO extends Annotation> ANNO getDeepMergedAnnotation(AnnotatedElement element, Class<ANNO> annoClass) {
        final List<ANNO> allMergedAnnotations = getAllSynthesizedAnnotations(element, annoClass);
        return AnnotationMergeUtils.merge(annoClass, new HashSet<>(allMergedAnnotations));
    }

    public static <ANNO extends Annotation> ANNO findDeepMergedAnnotation(AnnotatedElement element, Class<ANNO> annoClass) {
        final List<ANNO> allMergedAnnotations = getAllSynthesizedAnnotations(element, annoClass);
        return AnnotationMergeUtils.merge(annoClass, new HashSet<>(allMergedAnnotations));
    }

    /**
     * 获取元素上指定类型的所有注解，包括重复注解
     * 替换 hutool AnnotationUtil.getAllSynthesizedAnnotations
     */
    private static <ANNO extends Annotation> List<ANNO> getAllSynthesizedAnnotations(AnnotatedElement element, Class<ANNO> annotationClass) {
        if (element == null || annotationClass == null) {
            return Collections.emptyList();
        }

        List<ANNO> annotations = new ArrayList<>();

        // 获取直接注解
        ANNO directAnnotation = element.getAnnotation(annotationClass);
        if (directAnnotation != null) {
            annotations.add(directAnnotation);
        }

        // 处理重复注解
        Repeatable repeatable = annotationClass.getAnnotation(Repeatable.class);
        if (repeatable != null) {
            Class<? extends Annotation> containerClass = repeatable.value();
            Annotation containerAnnotation = element.getAnnotation(containerClass);
            if (containerAnnotation != null) {
                try {
                    Object value = containerClass.getMethod("value").invoke(containerAnnotation);
                    if (value instanceof Annotation[]) {
                        for (Annotation anno : (Annotation[]) value) {
                            if (annotationClass.isInstance(anno)) {
                                annotations.add(annotationClass.cast(anno));
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("处理重复注解时出错", e);
                }
            }
        }

        return annotations;
    }

}
