package org.dromara.autotable.test.springboot;

import org.junit.jupiter.api.Test;
import org.noear.solon.test.SolonTest;

//@EnableAutoTableTest
//@SpringBootTest
@SolonTest
public class MyTest {

    @Test
    public void test() {
        String dateTimeRegex = "(\\d+(.)?)+";
        System.out.println("2021/01/01 12:11:10:123".matches(dateTimeRegex));
    }
}
