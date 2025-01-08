package org.dromara.autotable.springboot;

import org.dromara.autotable.core.AutoTableBootstrap;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.config.PropertyConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

/**
 * 测试执行监听器
 */
public class AutoTableTestExecutionListener implements TestExecutionListener {

    @Override
    public void beforeTestClass(TestContext testContext) {

        initBasePackages(testContext);

        // 单元测试模式下，启动
        AutoTableBootstrap.start();
    }

    private void initBasePackages(TestContext testContext) {
        // 初始化应用上下文，会阻塞等待上下文加载完，此处会优先加载注解的配置
        ApplicationContext applicationContext = testContext.getApplicationContext();

        PropertyConfig autoTableProperties = AutoTableGlobalConfig.getAutoTableProperties();
        String[] modelPackage = autoTableProperties.getModelPackage();
        Class<?>[] modelClass = autoTableProperties.getModelClass();
        // 当注解没有相关的配置的时候，则使用启动类的包名
        if (modelPackage.length == 0 && modelClass.length == 0) {
            // 获取启动类（带有@SpringBootApplication注解的类）
            Object mainClass = applicationContext.getBeansWithAnnotation(org.springframework.boot.autoconfigure.SpringBootApplication.class)
                    .values()
                    .stream()
                    .findFirst()
                    .orElseGet(() -> applicationContext.getBeansWithAnnotation(org.springframework.boot.SpringBootConfiguration.class)
                            .values()
                            .stream()
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("启动类未找到"))
                    );

            // 获取启动类的包名
            String packageName = mainClass.getClass().getPackage().getName();
            autoTableProperties.setModelPackage(new String[]{packageName});
        }
    }
}
