package org.dromara.autotable.test.core.unit.scanner;

import org.dromara.autotable.core.AutoTableClassScanner;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.test.core.base.AbstractAutoTableTest;
import org.dromara.autotable.test.core.scan.a.A;
import org.dromara.autotable.test.core.scan.a.b.B;
import org.dromara.autotable.test.core.scan.a.b.c.C;
import org.dromara.autotable.test.core.scan.a.b.c.d.D;
import org.dromara.autotable.test.core.scan.a.b1.B1;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicationScanClassTest extends AbstractAutoTableTest {

    @Test
    public void scan1() {
        AutoTableClassScanner autoTableClassScanner = AutoTableGlobalConfig.instance().getAutoTableClassScanner();
        Set<Class<?>> scan = autoTableClassScanner.scan(new String[]{"org.dromara.autotable.test.core.scan.a"});
        assertNotNull(scan);
        assertEquals(5, scan.size());
        assertTrue(scan.contains(A.class));
        assertTrue(scan.contains(B.class));
        assertTrue(scan.contains(B1.class));
        assertTrue(scan.contains(C.class));
        assertTrue(scan.contains(D.class));
    }

    @Test
    public void scan2() {
        AutoTableClassScanner autoTableClassScanner = AutoTableGlobalConfig.instance().getAutoTableClassScanner();
        Set<Class<?>> scan = autoTableClassScanner.scan(new String[]{"org.dromara.autotable.test.core.scan.a.**"});
        assertNotNull(scan);
        assertEquals(4, scan.size());
        assertTrue(scan.contains(B.class));
        assertTrue(scan.contains(B1.class));
        assertTrue(scan.contains(C.class));
        assertTrue(scan.contains(D.class));
    }

    @Test
    public void scan3() {
        AutoTableClassScanner autoTableClassScanner = AutoTableGlobalConfig.instance().getAutoTableClassScanner();
        Set<Class<?>> scan = autoTableClassScanner.scan(new String[]{"org.dromara.autotable.test.core.scan.a.b"});
        assertNotNull(scan);
        assertEquals(3, scan.size());
        assertTrue(scan.contains(B.class));
        assertTrue(scan.contains(C.class));
        assertTrue(scan.contains(D.class));
    }

    @Test
    public void scan4() {
        AutoTableClassScanner autoTableClassScanner = AutoTableGlobalConfig.instance().getAutoTableClassScanner();
        Set<Class<?>> scan = autoTableClassScanner.scan(new String[]{"org.dromara.autotable.test.core.scan.*.b"});
        assertNotNull(scan);
        assertEquals(3, scan.size());
        assertTrue(scan.contains(B.class));
        assertTrue(scan.contains(C.class));
        assertTrue(scan.contains(D.class));
    }

    @Test
    public void scan5() {
        AutoTableClassScanner autoTableClassScanner = AutoTableGlobalConfig.instance().getAutoTableClassScanner();
        Set<Class<?>> scan = autoTableClassScanner.scan(new String[]{"org.dromara.autotable.test.core.scan.*.b.*"});
        assertNotNull(scan);
        assertEquals(2, scan.size());
        assertTrue(scan.contains(C.class));
        assertTrue(scan.contains(D.class));
    }

    @Test
    public void scan6() {
        AutoTableClassScanner autoTableClassScanner = AutoTableGlobalConfig.instance().getAutoTableClassScanner();
        Set<Class<?>> scan = autoTableClassScanner.scan(new String[]{"org.dromara.autotable.test.core.scan.*.b*"});
        assertNotNull(scan);
        assertEquals(1, scan.size());
        assertTrue(scan.contains(B1.class));
    }
}
