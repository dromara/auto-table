package org.dromara.autotable.test.core;

import org.dromara.autotable.core.AutoTableClassScanner;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.test.core.scan.a.A;
import org.dromara.autotable.test.core.scan.a.b.B;
import org.dromara.autotable.test.core.scan.a.b.c.C;
import org.dromara.autotable.test.core.scan.a.b.c.d.D;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.Set;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ApplicationScanClassTest {

    @Test
    public void scan1() {
        AutoTableClassScanner autoTableClassScanner = AutoTableGlobalConfig.getAutoTableClassScanner();
        Set<Class<?>> scan = autoTableClassScanner.scan(new String[]{"org.dromara.autotable.test.core.scan.a"});
        assert scan != null
                && scan.size() == 4
                && scan.contains(A.class)
                && scan.contains(B.class)
                && scan.contains(C.class)
                && scan.contains(D.class);
    }

    @Test
    public void scan2() {
        AutoTableClassScanner autoTableClassScanner = AutoTableGlobalConfig.getAutoTableClassScanner();
        Set<Class<?>> scan = autoTableClassScanner.scan(new String[]{"org.dromara.autotable.test.core.scan.a.**"});
        assert scan != null
                && scan.size() == 3
                && scan.contains(B.class)
                && scan.contains(C.class)
                && scan.contains(D.class);
    }

    @Test
    public void scan3() {
        AutoTableClassScanner autoTableClassScanner = AutoTableGlobalConfig.getAutoTableClassScanner();
        Set<Class<?>> scan = autoTableClassScanner.scan(new String[]{"org.dromara.autotable.test.core.scan.a.b"});
        assert scan != null
                && scan.size() == 3
                && scan.contains(B.class)
                && scan.contains(C.class)
                && scan.contains(D.class);
    }

    @Test
    public void scan4() {
        AutoTableClassScanner autoTableClassScanner = AutoTableGlobalConfig.getAutoTableClassScanner();
        Set<Class<?>> scan = autoTableClassScanner.scan(new String[]{"org.dromara.autotable.test.core.scan.*.b"});
        assert scan != null
                && scan.size() == 2
                && scan.contains(C.class)
                && scan.contains(D.class);
    }
}
