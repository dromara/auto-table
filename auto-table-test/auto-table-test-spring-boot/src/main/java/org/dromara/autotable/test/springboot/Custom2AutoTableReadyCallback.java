package org.dromara.autotable.test.springboot;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.callback.AutoTableReadyCallback;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@Order(2)
public class Custom2AutoTableReadyCallback implements AutoTableReadyCallback {

    @Override
    public void ready(Set<Class<?>> tableClasses) {
        log.info("(2) AutoTableReadyCallback ready");
    }
}
