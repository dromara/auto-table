package org.dromara.autotable.test.solon;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.core.config.Order;
import org.dromara.autotable.core.callback.AutoTableReadyCallback;
import org.noear.solon.annotation.Component;

import java.util.Set;

@Slf4j
@Component
@Order(1)
public class Custom1AutoTableReadyCallback implements AutoTableReadyCallback {

    @Override
    public void ready(Set<Class<?>> tableClasses) {
        log.info("(1) AutoTableReadyCallback ready");
    }
}
