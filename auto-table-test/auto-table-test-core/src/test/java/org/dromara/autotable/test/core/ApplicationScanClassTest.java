package org.dromara.autotable.test.core;

import org.dromara.autotable.core.AutoTableClassScanner;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.test.core.scan.a.A;
import org.dromara.autotable.test.core.scan.a.b.B;
import org.dromara.autotable.test.core.scan.a.b.c.C;
import org.dromara.autotable.test.core.scan.a.b.c.d.D;
import org.dromara.autotable.test.core.scan.a.b1.B1;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class ApplicationScanClassTest {

    @AfterEach
    void cleanup() {
        // 清除当前线程中的配置，防止下一个测试复用
        AutoTableGlobalConfig.clear();
    }

    @Test
    public void scan1() {
        AutoTableClassScanner autoTableClassScanner = AutoTableGlobalConfig.instance().getAutoTableClassScanner();
        Set<Class<?>> scan = autoTableClassScanner.scan(new String[]{"org.dromara.autotable.test.core.scan.a"});
        assert scan != null;
        assert scan.size() == 5;
        assert scan.contains(A.class);
        assert scan.contains(B.class);
        assert scan.contains(B1.class);
        assert scan.contains(C.class);
        assert scan.contains(D.class);
    }

    @Test
    public void scan2() {
        AutoTableClassScanner autoTableClassScanner = AutoTableGlobalConfig.instance().getAutoTableClassScanner();
        Set<Class<?>> scan = autoTableClassScanner.scan(new String[]{"org.dromara.autotable.test.core.scan.a.**"});
        assert scan != null;
        assert scan.size() == 4;
        assert scan.contains(B.class);
        assert scan.contains(B1.class);
        assert scan.contains(C.class);
        assert scan.contains(D.class);
    }

    @Test
    public void scan3() {
        AutoTableClassScanner autoTableClassScanner = AutoTableGlobalConfig.instance().getAutoTableClassScanner();
        Set<Class<?>> scan = autoTableClassScanner.scan(new String[]{"org.dromara.autotable.test.core.scan.a.b"});
        assert scan != null;
        assert scan.size() == 3;
        assert scan.contains(B.class);
        assert scan.contains(C.class);
        assert scan.contains(D.class);
    }

    @Test
    public void scan4() {
        AutoTableClassScanner autoTableClassScanner = AutoTableGlobalConfig.instance().getAutoTableClassScanner();
        Set<Class<?>> scan = autoTableClassScanner.scan(new String[]{"org.dromara.autotable.test.core.scan.*.b"});
        assert scan != null;
        assert scan.size() == 3;
        assert scan.contains(B.class);
        assert scan.contains(C.class);
        assert scan.contains(D.class);
    }

    @Test
    public void scan5() {
        AutoTableClassScanner autoTableClassScanner = AutoTableGlobalConfig.instance().getAutoTableClassScanner();
        Set<Class<?>> scan = autoTableClassScanner.scan(new String[]{"org.dromara.autotable.test.core.scan.*.b.*"});
        assert scan != null;
        assert scan.size() == 2;
        assert scan.contains(C.class);
        assert scan.contains(D.class);
    }

    @Test
    public void scan6() {
        AutoTableClassScanner autoTableClassScanner = AutoTableGlobalConfig.instance().getAutoTableClassScanner();
        Set<Class<?>> scan = autoTableClassScanner.scan(new String[]{"org.dromara.autotable.test.core.scan.*.b*"});
        assert scan != null;
        assert scan.size() == 1;
        assert scan.contains(B1.class);
    }
}
