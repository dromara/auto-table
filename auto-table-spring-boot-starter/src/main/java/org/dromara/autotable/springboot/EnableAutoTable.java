package org.dromara.autotable.springboot;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author don
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({AutoTablePropertiesRegister.class, AutoTableAutoConfig.class, AutoTableImportRegister.class, AutoTableRunner.class})
public @interface EnableAutoTable {

    /**
     * 指定包扫描路径
     */
    String[] basePackages() default {};

    /**
     * 单独指定扫描的类
     */
    Class[] classes() default {};
}
