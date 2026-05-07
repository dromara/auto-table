package org.dromara.autotable.test.core.entity.mysql;

import lombok.Data;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.mysql.MysqlTableFullTextIndex;

/**
 * 测试 MySQL 类级别全文索引
 *
 * @author don
 */
@Data
@AutoTable
@MysqlTableFullTextIndex(fields = {"content", "description"}, comment = "内容全文索引")
public class TestTableFullTextIndex {

    private Long id;

    private String content;

    private String description;
}
