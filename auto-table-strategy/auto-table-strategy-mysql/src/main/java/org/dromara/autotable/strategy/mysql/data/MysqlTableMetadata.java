package org.dromara.autotable.strategy.mysql.data;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.strategy.TableMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * @author don
 */
@Getter
@Setter
@Accessors(chain = true)
public class MysqlTableMetadata extends TableMetadata {

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
    /**
     * 所有列
     */
    private List<MysqlColumnMetadata> columnMetadataList = new ArrayList<>();
    /**
     * 索引
     */
    private List<IndexMetadata> indexMetadataList = new ArrayList<>();

    public MysqlTableMetadata(Class<?> entityClass, String tableName, String comment) {
        super(entityClass, tableName, "", comment);
    }
}
