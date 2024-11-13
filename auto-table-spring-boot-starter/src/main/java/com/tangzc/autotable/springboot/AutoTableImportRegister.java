package com.tangzc.autotable.springboot;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;

/**
 * 获取注解的basePackages
 * {@link EnableAutoTable}和{@link EnableAutoTableTest}都会激活该类，其中{@link EnableAutoTableTest}是用于单元测试的，因此优先级要高于{@link EnableAutoTable}
 * @author don
 */
public class AutoTableImportRegister implements ImportBeanDefinitionRegistrar {

    /**
     * 提取注解的basePackages
     */
    public static volatile String[] basePackagesFromAnno;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        // 当取到basePackages，则不再继续执行，发生的场景是单元测试和启动类都指定了basePackages，优先以单元测试的为准
        if (basePackagesFromAnno != null) {
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
        }
    }

    /**
     * 分别尝试获取两个注解的值
     */
    private Map<String, Object> getAutoTableAttributes(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> autoTableAttributes = importingClassMetadata.getAnnotationAttributes(EnableAutoTableTest.class.getName());
        if (autoTableAttributes == null) {
            importingClassMetadata.getAnnotationAttributes(EnableAutoTable.class.getName());
        }
        return autoTableAttributes;
    }
}
