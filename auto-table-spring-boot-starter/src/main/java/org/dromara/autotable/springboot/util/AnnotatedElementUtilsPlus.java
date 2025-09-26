package org.dromara.autotable.springboot.util;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.utils.AnnotationDefaultValueHelper;
import org.dromara.autotable.core.utils.AnnotationMergeUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 注解合并处理类，可以将相同的注解的不同实例，中的属性合并为一个注解实例
 *
 * @author don
 */
@Slf4j
public class AnnotatedElementUtilsPlus extends AnnotatedElementUtils {

    public static <ANNO extends Annotation> ANNO getDeepMergedAnnotation(AnnotatedElement element, Class<ANNO> annoClass) {
        final Set<ANNO> allMergedAnnotations = AnnotatedElementUtils.getAllMergedAnnotations(element, annoClass);
        return AnnotationMergeUtils.merge(annoClass, allMergedAnnotations);
    }

    public static <ANNO extends Annotation> ANNO findDeepMergedAnnotation(AnnotatedElement element, Class<ANNO> annoClass) {
        final Set<ANNO> allMergedAnnotations = AnnotatedElementUtils.findAllMergedAnnotations(element, annoClass);
        return AnnotationMergeUtils.merge(annoClass, allMergedAnnotations);
    }
}
