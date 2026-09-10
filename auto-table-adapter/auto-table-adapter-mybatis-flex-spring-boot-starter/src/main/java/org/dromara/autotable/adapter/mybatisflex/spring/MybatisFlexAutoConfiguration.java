package org.dromara.autotable.adapter.mybatisflex.spring;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.adapter.mybatisflex.MybatisFlexAdapterConfig;
import org.dromara.autotable.adapter.mybatisflex.MybatisFlexAutoTableClassScanner;
import org.dromara.autotable.adapter.mybatisflex.MybatisFlexJavaTypeToDatabaseTypeConverter;
import org.dromara.autotable.adapter.mybatisflex.MybatisFlexMetadataAdapter;
import org.dromara.autotable.core.AutoTableClassScanner;
import org.dromara.autotable.core.AutoTableMetadataAdapter;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

/**
 * MyBatis-Flex 适配器 Spring Boot 自动配置。
 * <p>
 * 职责：
 * <ol>
 *     <li>桥接：从 {@link Environment} 读取 MyBatis-Flex 配置 → 注入 {@link MybatisFlexAdapterConfig}</li>
 *     <li>注册 adapter 主体类为 Bean（由 auto-table-spring-boot-starter 的 AutoTableAutoConfig 通过 ObjectProvider 自动发现）</li>
 * </ol>
 * <p>
 * <b>为什么用 Environment 而不是注入 MybatisFlexProperties</b>：
 * MyBatis-Flex 自 1.11.5 起为 Spring Boot 4 提供了独立的 {@code mybatis-flex-spring-boot4-starter}，
 * 其属性类被复制到新包 {@code com.mybatisflex.spring.boot.v4.MybatisFlexProperties}，
 * 与老包 {@code com.mybatisflex.spring.boot.MybatisFlexProperties} 在类型系统上毫无关系（都直接继承 Object，
 * 无共同父类或接口）。直接注入老包类型在 Boot 4 环境下会因 Bean 缺失导致启动失败。
 * 改用 Environment 按 key 读取（配置 key 属于公开契约，比包名稳定），
 * 可在 Boot 2.7 / 3.x / 4.x 三个大版本下同时工作，且未来 MF 再调整包结构也无需改动 AutoTable。
 *
 * @author auto-table
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "com.mybatisflex.annotation.Table")
@AutoConfigureAfter(name = {
        "com.mybatisflex.spring.boot.MybatisFlexAutoConfiguration",
        "com.mybatisflex.spring.boot.v4.MybatisFlexAutoConfiguration"
})
@AutoConfigureBefore(name = "org.dromara.autotable.springboot.AutoTableAutoConfig")
public class MybatisFlexAutoConfiguration {

    /**
     * MyBatis-Flex 全局配置中"驼峰转下划线"的 key。
     * 该 key 属于 MyBatis-Flex 官方公开契约，Boot 2/3/4 三个 starter 版本均一致。
     */
    private static final String MAP_UNDERSCORE_KEY =
            "mybatis-flex.configuration.map-underscore-to-camel-case";

    /**
     * MyBatis-Flex v4 属性类的全限定名，用于判定当前是否处于 Boot 4 环境。
     */
    private static final String V4_PROPERTIES_CLASS =
            "com.mybatisflex.spring.boot.v4.MybatisFlexProperties";

    /**
     * 桥接：Environment → adapter config POJO。
     */
    @Bean
    public MybatisFlexAdapterConfig mybatisFlexAdapterConfig(Environment environment) {
        MybatisFlexAdapterConfig config = new MybatisFlexAdapterConfig();
        Boolean mapUnderscore = environment.getProperty(MAP_UNDERSCORE_KEY, Boolean.class);
        if (mapUnderscore == null) {
            mapUnderscore = true;
            // 仅在 Boot 4 环境（classpath 存在 v4 属性类）才打 WARN，
            // 避免 Boot 2.7/3.x 用户因未显式配置该项而被误告警。
            if (ClassUtils.isPresent(V4_PROPERTIES_CLASS, getClass().getClassLoader())) {
                log.warn("未读取到配置项 [{}]，mapUnderscoreToCamelCase 按默认值 true 处理。"
                                + "若你所用的 MyBatis-Flex 版本调整了该配置项的 key，请反馈给 AutoTable。",
                        MAP_UNDERSCORE_KEY);
            }
        }
        config.setMapUnderscoreToCamelCase(mapUnderscore);
        return config;
    }

    @Bean
    @ConditionalOnMissingBean(AutoTableMetadataAdapter.class)
    public MybatisFlexMetadataAdapter mybatisFlexMetadataAdapter(MybatisFlexAdapterConfig config) {
        return new MybatisFlexMetadataAdapter(config);
    }

    @Bean
    @ConditionalOnMissingBean(AutoTableClassScanner.class)
    public MybatisFlexAutoTableClassScanner mybatisFlexAutoTableClassScanner() {
        return new MybatisFlexAutoTableClassScanner();
    }

    @Bean
    @ConditionalOnMissingBean
    public MybatisFlexJavaTypeToDatabaseTypeConverter mybatisFlexJavaTypeToDatabaseTypeConverter() {
        return new MybatisFlexJavaTypeToDatabaseTypeConverter();
    }
}
