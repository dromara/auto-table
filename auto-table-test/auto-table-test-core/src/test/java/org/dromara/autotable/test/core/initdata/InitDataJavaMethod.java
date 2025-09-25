package org.dromara.autotable.test.core.initdata;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.core.initdata.InitDataList;

import java.util.Arrays;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@AutoTable(comment = "用java方法初始化数据")
public class InitDataJavaMethod {

    private String name;

    @InitDataList
    public static List<InitDataJavaMethod> getInitData() {
        return Arrays.asList(new InitDataJavaMethod("zhang"), new InitDataJavaMethod("li"));
    }

    @InitDataList
    private static List<InitDataJavaMethod> getInitData2() {
        return Arrays.asList(new InitDataJavaMethod("wang"), new InitDataJavaMethod("liu"));
    }
}
