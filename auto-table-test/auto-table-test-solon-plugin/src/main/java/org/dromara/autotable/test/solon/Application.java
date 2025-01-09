package org.dromara.autotable.test.solon;

import org.dromara.autotable.solon.annotation.EnableAutoTable;
import org.noear.solon.Solon;
import org.noear.solon.annotation.SolonMain;

/**
 * 启动类
 * @author don
 */
@SolonMain
@EnableAutoTable
public class Application {

    public static void main(String[] args) {
        Solon.start(Application.class, args);
    }
}
