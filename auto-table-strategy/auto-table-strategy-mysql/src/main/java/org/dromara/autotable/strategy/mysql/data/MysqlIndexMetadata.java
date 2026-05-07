package org.dromara.autotable.strategy.mysql.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.autotable.core.strategy.IndexMetadata;

/**
 * MySQL 索引元数据，继承自 {@link IndexMetadata}，用于存储 MySQL 独有的索引属性
 *
 * @author don
 */
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@Accessors(chain = true)
public class MysqlIndexMetadata extends IndexMetadata {

    /**
     * 是否为全文索引
     */
    private boolean fullText;

    /**
     * 全文索引的分词器，如 ngram
     */
    private String parser;

}
