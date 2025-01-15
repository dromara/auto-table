package org.dromara.autotable.springboot;

import org.dromara.autotable.springboot.properties.AutoTableProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;

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
@TestExecutionListeners(
        listeners = {AutoTableTestExecutionListener.class},
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@EnableConfigurationProperties(AutoTableProperties.class)
@Import({AutoTableAutoConfig.class, AutoTableImportRegister.class, AutoTableTest.class})
public @interface EnableAutoTableTest {

    /**
     * 指定包扫描路径
     */
    String[] basePackages() default {};

    /**
     * 单独指定扫描的类
     */
    Class[] classes() default {};
}
