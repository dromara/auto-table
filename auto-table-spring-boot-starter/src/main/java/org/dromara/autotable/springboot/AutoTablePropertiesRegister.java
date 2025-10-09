package org.dromara.autotable.springboot;

import org.dromara.autotable.core.config.PropertyConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 注册AutoTable的配置属性
 */
@AutoConfiguration
public class AutoTablePropertiesRegister {

    @Bean
    @ConfigurationProperties("auto-table")
    public PropertyConfig propertyConfig() {
        return new PropertyConfig();
    }
}
