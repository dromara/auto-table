package org.dromara.autotable.strategy.mysql.data;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;

/**
 * @author don
 */
@Getter
@Setter
@Accessors(chain = true)
public class MysqlTableMetadata extends DefaultTableMetadata {

    /**
     * 引擎
     */
    private String engine;
    /**
     * 默认字符集
     */
    private String characterSet;
    /**
     * 默认排序规则
     */
    private String collate;

    public MysqlTableMetadata(Class<?> entityClass, String tableName, String schema, String comment) {
        super(entityClass, tableName, schema, comment);
    }
}
