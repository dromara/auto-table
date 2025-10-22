package org.dromara.autotable.springboot;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;

/**
 * <p> 获取注解的 basePackagesFromAnno、classesFromAnno 属性
 * <p> {@link EnableAutoTable}和{@link EnableAutoTableTest}都会激活该类，其中{@link EnableAutoTableTest}是用于单元测试的，因此在执行顺序上优先级要高于{@link EnableAutoTable}
 * @author don
 */
@Lazy(false)
public class AutoTableImportRegister implements ImportBeanDefinitionRegistrar {

    /**
     * 提取注解的basePackages
     */
    public static volatile String[] basePackagesFromAnno;
    /**
     * 提取注解的classes
     */
    public static volatile Class<?>[] classesFromAnno;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        // 当取到basePackages或者classesFromAnno，则不再继续执行，发生的场景是单元测试和启动类都指定了basePackages，优先以单元测试的为准
        if (basePackagesFromAnno != null || classesFromAnno != null) {
            return;
        }

        Map<String, Object> autoTableAttributes = getAutoTableAttributes(importingClassMetadata);
        AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(autoTableAttributes);
        if (annotationAttributes != null) {
            String[] basePackages = Arrays.stream(annotationAttributes.getStringArray("basePackages"))
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toArray(String[]::new);
            if (basePackages.length > 0) {
                basePackagesFromAnno = basePackages;
            }
            Class<?>[] classes = Arrays.stream(annotationAttributes.getClassArray("classes"))
                    .distinct()
                    .toArray(Class[]::new);
            if (classes.length > 0) {
                classesFromAnno = classes;
            }
        }
    }

    /**
     * 分别尝试获取两个注解的值
     */
    private Map<String, Object> getAutoTableAttributes(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> autoTableAttributes = importingClassMetadata.getAnnotationAttributes(EnableAutoTableTest.class.getName());
        if (autoTableAttributes == null) {
            autoTableAttributes = importingClassMetadata.getAnnotationAttributes(EnableAutoTable.class.getName());
        }
        return autoTableAttributes;
    }
}
