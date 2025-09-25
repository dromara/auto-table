package org.dromara.autotable.solon.properties;

import lombok.Data;
import org.dromara.autotable.core.config.PropertyConfig;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.BindProps;
import org.noear.solon.annotation.Configuration;


/**
 * @author chengliang
 * @date 2025/01/07
 */
@Data
@Configuration
public class AutoTablePropertiesRegister {

    @BindProps(prefix = "auto-table")
    @Bean
    public PropertyConfig propertyConfig() {
        return new PropertyConfig();
    }
}
