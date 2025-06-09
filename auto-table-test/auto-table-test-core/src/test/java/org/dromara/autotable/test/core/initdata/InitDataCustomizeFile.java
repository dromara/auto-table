package org.dromara.autotable.test.core.initdata;

import org.dromara.autotable.annotation.AutoTable;

@AutoTable(comment = "自定义sql文件初始化数据", initSql = "classpath:customize_path/InitDataCustomizeFile.sql")
public class InitDataCustomizeFile {

    private String name;
}
