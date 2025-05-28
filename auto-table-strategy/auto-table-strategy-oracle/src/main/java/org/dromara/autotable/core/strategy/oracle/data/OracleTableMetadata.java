package org.dromara.autotable.core.strategy.oracle.data;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.strategy.TableMetadata;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
@Accessors(chain = true)
public class OracleTableMetadata extends TableMetadata {
    public OracleTableMetadata(Class<?> entityClass, String tableName, String schema, String comment) {
        super(entityClass, tableName, schema, comment);
    }

    /**
     * 所有列
     */
    private List<OracleColumnMetadata> columnMetadataList = new ArrayList<>();
    /**
     * 索引
     */
    private List<IndexMetadata> indexMetadataList = new ArrayList<>();

}
