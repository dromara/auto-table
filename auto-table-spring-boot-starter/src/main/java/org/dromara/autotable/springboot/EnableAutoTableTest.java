package org.dromara.autotable.springboot;

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
@Import({AutoTableTest.class, AutoTablePropertiesRegister.class, AutoTableAutoConfig.class, AutoTableImportRegister.class})
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
