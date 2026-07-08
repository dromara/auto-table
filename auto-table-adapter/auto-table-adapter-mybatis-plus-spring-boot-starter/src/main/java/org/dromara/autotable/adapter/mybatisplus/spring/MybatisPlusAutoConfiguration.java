package org.dromara.autotable.adapter.mybatisplus.spring;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import org.dromara.autotable.adapter.mybatisplus.MybatisPlusAdapterConfig;
import org.dromara.autotable.adapter.mybatisplus.MybatisPlusJavaTypeToDatabaseTypeConverter;
import org.dromara.autotable.adapter.mybatisplus.MybatisPlusRunAfterCallback;
import org.dromara.autotable.adapter.mybatisplus.MybatisPlusRunBeforeCallback;
import org.dromara.autotable.core.AutoTableMetadataAdapter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 适配器 Spring Boot 自动配置。
 * <p>
 * 职责：
 * <ol>
 *     <li>桥接：从 MP 原生 {@link MybatisPlusProperties} 提取配置 → 注入 adapter 的 {@link MybatisPlusAdapterConfig}</li>
 *     <li>注册 adapter 主体类为 Bean（由 auto-table-spring-boot-starter 的 AutoTableAutoConfig 通过 ObjectProvider 自动发现）</li>
 *     <li>注册 starter 扩展类（自定义注解支持）</li>
 * </ol>
 * <p>
 * 动态数据源相关 Bean 独立为内部类 {@link DynamicDataSourceConfiguration}，
 * 通过类级 {@code @ConditionalOnClass} 保护，避免 {@code DynamicDataSourceProperties}
 * 不在 classpath 时导致 {@code NoClassDefFoundError}。
 *
 * @author auto-table
 */
@Configuration
@ConditionalOnClass(name = "com.baomidou.mybatisplus.annotation.TableName")
@AutoConfigureBefore(name = "org.dromara.autotable.springboot.AutoTableAutoConfig")
public class MybatisPlusAutoConfiguration {

    /**
     * 桥接：MP 原生 Properties → adapter config POJO。
     */
    @Bean
    public MybatisPlusAdapterConfig mybatisPlusAdapterConfig(MybatisPlusProperties mpProperties) {
        MybatisPlusAdapterConfig config = new MybatisPlusAdapterConfig();
        config.setTablePrefix(mpProperties.getGlobalConfig().getDbConfig().getTablePrefix());
        Boolean mapUnderscore = mpProperties.getConfiguration().getMapUnderscoreToCamelCase();
        config.setMapUnderscoreToCamelCase(mapUnderscore != null && mapUnderscore);
        config.setCapitalMode(mpProperties.getGlobalConfig().getDbConfig().isCapitalMode());
        config.setLogicDeleteField(mpProperties.getGlobalConfig().getDbConfig().getLogicDeleteField());
        config.setLogicNotDeleteValue(mpProperties.getGlobalConfig().getDbConfig().getLogicNotDeleteValue());
        return config;
    }

    @Bean
    @ConditionalOnMissingBean(AutoTableMetadataAdapter.class)
    public MybatisPlusExtendedMetadataAdapter mybatisPlusExtendedMetadataAdapter(
            MybatisPlusAdapterConfig config) {
        return new MybatisPlusExtendedMetadataAdapter(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusJavaTypeToDatabaseTypeConverter mybatisPlusJavaTypeToDatabaseTypeConverter() {
        return new MybatisPlusJavaTypeToDatabaseTypeConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusExtendedClassScanner mybatisPlusExtendedClassScanner() {
        return new MybatisPlusExtendedClassScanner();
    }

    @Bean
    public MybatisPlusRunBeforeCallback mybatisPlusRunBeforeCallback() {
        return new MybatisPlusRunBeforeCallback();
    }

    @Bean
    public MybatisPlusRunAfterCallback mybatisPlusRunAfterCallback() {
        return new MybatisPlusRunAfterCallback();
    }

    // ===== 动态数据源（独立内部类，类级 @ConditionalOnClass 保护）=====

    /**
     * 动态数据源配置。
     * 仅在 classpath 存在 {@code DynamicDataSourceProperties} 时加载，
     * 避免未引入 dynamic-datasource 时 {@code NoClassDefFoundError}。
     */
    @Configuration
    @ConditionalOnClass(name = "com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties")
    static class DynamicDataSourceConfiguration {

        @Bean
        @ConditionalOnProperty(
                prefix = "spring.datasource.dynamic",
                name = "enabled", havingValue = "true", matchIfMissing = true)
        public MybatisPlusDynamicDataSourceHandler mybatisPlusDynamicDataSourceHandler(
                com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties props) {
            return new MybatisPlusDynamicDataSourceHandler(props.getPrimary());
        }

        @Bean
        public MybatisPlusDataSourceInfoExtractor mybatisPlusDataSourceInfoExtractor() {
            return new MybatisPlusDataSourceInfoExtractor();
        }
    }
}
