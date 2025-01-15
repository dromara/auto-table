package org.dromara.autotable.springboot;

import org.dromara.autotable.core.AutoTableBootstrap;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * @author don
 */
@AutoConfigureAfter({AutoTableAutoConfig.class})
@ConditionalOnMissingBean(AutoTableTest.class)
public class AutoTableRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        // 启动AutoTable
        AutoTableBootstrap.start();
    }
}
