package org.dromara.autotable.adapter.mybatisflex.spring;

import com.mybatisflex.spring.boot.MybatisFlexProperties;
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

import java.util.Optional;

/**
 * MyBatis-Flex 适配器 Spring Boot 自动配置。
 * <p>
 * 职责：
 * <ol>
 *     <li>桥接：从 MybatisFlexProperties 提取配置 → 注入 MybatisFlexAdapterConfig</li>
 *     <li>注册 adapter 主体类为 Bean（由 auto-table-spring-boot-starter 的 AutoTableAutoConfig 通过 ObjectProvider 自动发现）</li>
 * </ol>
 *
 * @author auto-table
 */
@Configuration
@ConditionalOnClass(name = "com.mybatisflex.annotation.Table")
@AutoConfigureAfter(name = "com.mybatisflex.spring.boot.MybatisFlexAutoConfiguration")
@AutoConfigureBefore(name = "org.dromara.autotable.springboot.AutoTableAutoConfig")
public class MybatisFlexAutoConfiguration {

    /**
     * 桥接：MybatisFlexProperties → adapter config POJO。
     */
    @Bean
    public MybatisFlexAdapterConfig mybatisFlexAdapterConfig(MybatisFlexProperties flexProperties) {
        MybatisFlexAdapterConfig config = new MybatisFlexAdapterConfig();
        Boolean mapUnderscore = Optional.ofNullable(flexProperties.getConfiguration())
                .map(MybatisFlexProperties.CoreConfiguration::getMapUnderscoreToCamelCase)
                .orElse(true);
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
