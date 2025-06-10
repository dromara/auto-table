package org.dromara.autotable.support.springdoc;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.ParameterCustomizer;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({PropertyCustomizer.class, ParameterCustomizer.class})
@Import({AutoTablePropertyCustomizer.class, AutoTableParameterCustomizer.class})
public class AutoTableSpringdocAutoConfiguration {

}
