package org.dromara.autotable.adapter.mybatisplus.spring;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.dromara.autotable.adapter.mybatisplus.MybatisPlusAdapterConfig;
import org.dromara.autotable.adapter.mybatisplus.MybatisPlusAutoTableClassScanner;
import org.dromara.autotable.adapter.mybatisplus.MybatisPlusJavaTypeToDatabaseTypeConverter;
import org.dromara.autotable.adapter.mybatisplus.MybatisPlusMetadataAdapter;
import org.dromara.autotable.adapter.mybatisplus.MybatisPlusRunAfterCallback;
import org.dromara.autotable.adapter.mybatisplus.MybatisPlusRunBeforeCallback;
import org.dromara.autotable.core.AutoTableClassScanner;
import org.dromara.autotable.core.AutoTableMetadataAdapter;
import org.dromara.autotable.springboot.InitializeBeans;
import org.springframework.beans.factory.ObjectProvider;
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
 *     <li>桥接：从 {@link SqlSessionFactory} 读取 MP 实际运行时配置 → 注入 adapter 的 {@link MybatisPlusAdapterConfig}</li>
 *     <li>注册 adapter 主体类为 Bean（由 auto-table-spring-boot-starter 的 AutoTableAutoConfig 通过 ObjectProvider 自动发现）</li>
 * </ol>
 * <p>
 * 配置读取策略：优先从 {@code SqlSessionFactory.getConfiguration()} + {@link GlobalConfigUtils} 获取
 * MP 应用了内部默认值后的实际配置（如 {@code mapUnderscoreToCamelCase=true}），
 * 而非从 {@link MybatisPlusProperties} 读取可能不包含 MP 内部默认值的原始属性。
 * 通过 {@link InitializeBeans} 机制强制 {@code SqlSessionFactory} 在 AutoTable 启动前创建。
 * <p>
 * 本模块只处理 MP 原生注解（{@code @TableName}/{@code @TableField}/{@code @TableId}/{@code @EnumValue}）的兼容。
 * 自定义注解（{@code @Table}/{@code @Column}/{@code @ColumnId}）由 mybatis-plus-ext 项目提供。
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
     * 强制 SqlSessionFactory 在 AutoTable 启动前创建。
     * <p>
     * 通过实现 {@link InitializeBeans}，在 AutoTableAutoConfig 构造器的 {@code initializeBeans.orderedStream()}
     * 阶段触发 SqlSessionFactory 初始化，使后续 {@link MybatisPlusAdapterConfig} 能读取到
     * MP 应用了内部默认值后的实际配置（如 {@code mapUnderscoreToCamelCase=true}）。
     */
    @Bean
    public InitializeBeans mpSqlSessionFactoryInitializer(ObjectProvider<SqlSessionFactory> sqlSessionFactory) {
        sqlSessionFactory.getIfAvailable();
        return new InitializeBeans() {};
    }

    /**
     * 桥接：从 SqlSessionFactory 读取 MP 实际运行时配置 → adapter config POJO。
     * <p>
     * 优先从 {@code SqlSessionFactory.getConfiguration()} + {@link GlobalConfigUtils} 获取
     * MP 应用了内部默认值后的实际配置；若 SqlSessionFactory 不存在则回退到 {@link MybatisPlusProperties}。
     */
    @Bean
    public MybatisPlusAdapterConfig mybatisPlusAdapterConfig(
            ObjectProvider<SqlSessionFactory> sqlSessionFactoryProvider,
            ObjectProvider<MybatisPlusProperties> mpPropertiesProvider) {
        MybatisPlusAdapterConfig config = new MybatisPlusAdapterConfig();

        SqlSessionFactory factory = sqlSessionFactoryProvider.getIfAvailable();
        if (factory != null) {
            // 从 SqlSessionFactory 读取 MP 实际运行时配置（包含 MP 内部默认值）
            org.apache.ibatis.session.Configuration configuration = factory.getConfiguration();
            config.setMapUnderscoreToCamelCase(configuration.isMapUnderscoreToCamelCase());

            GlobalConfig globalConfig = GlobalConfigUtils.getGlobalConfig(configuration);
            GlobalConfig.DbConfig dbConfig = globalConfig.getDbConfig();
            config.setTablePrefix(dbConfig.getTablePrefix());
            config.setCapitalMode(dbConfig.isCapitalMode());
            config.setLogicDeleteField(dbConfig.getLogicDeleteField());
            config.setLogicNotDeleteValue(dbConfig.getLogicNotDeleteValue());
        } else {
            // 回退：SqlSessionFactory 不存在时从 MybatisPlusProperties 读取
            MybatisPlusProperties mpProperties = mpPropertiesProvider.getIfAvailable();
            if (mpProperties != null) {
                config.setTablePrefix(mpProperties.getGlobalConfig().getDbConfig().getTablePrefix());
                config.setCapitalMode(mpProperties.getGlobalConfig().getDbConfig().isCapitalMode());
                config.setLogicDeleteField(mpProperties.getGlobalConfig().getDbConfig().getLogicDeleteField());
                config.setLogicNotDeleteValue(mpProperties.getGlobalConfig().getDbConfig().getLogicNotDeleteValue());
            }
            // mapUnderscoreToCamelCase 保持 MybatisPlusAdapterConfig 默认值 true
        }
        return config;
    }

    @Bean
    @ConditionalOnMissingBean(AutoTableMetadataAdapter.class)
    public MybatisPlusMetadataAdapter mybatisPlusMetadataAdapter(
            MybatisPlusAdapterConfig config) {
        return new MybatisPlusMetadataAdapter(config);
    }

    @Bean
    @ConditionalOnMissingBean(AutoTableClassScanner.class)
    public MybatisPlusAutoTableClassScanner mybatisPlusAutoTableClassScanner() {
        return new MybatisPlusAutoTableClassScanner();
    }

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusJavaTypeToDatabaseTypeConverter mybatisPlusJavaTypeToDatabaseTypeConverter() {
        return new MybatisPlusJavaTypeToDatabaseTypeConverter();
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
