package org.dromara.autotable.springboot;

import org.dromara.autotable.core.AutoTableGlobalConfig;

public class AutoTableTest {

    public AutoTableTest() {
        // 开启单元测试
        AutoTableGlobalConfig.instance().setUnitTestMode(true);
        System.out.println("=========================");
        System.out.println("== 开启AutoTable单元测试 ==");
        System.out.println("=========================");
    }
}
