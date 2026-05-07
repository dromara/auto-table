package org.dromara.autotable.test.core.entity.mysql;

import lombok.Data;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.mysql.MysqlFullTextIndex;

/**
 * 测试 MySQL 全文索引
 *
 * @author don
 */
@Data
@AutoTable
public class TestFullTextIndex {

    private Long id;

    // 单字段全文索引
    @MysqlFullTextIndex
    private String content;

    // 单字段全文索引，指定分词器
    @MysqlFullTextIndex(parser = "ngram", comment = "中文全文索引")
    private String description;

}
