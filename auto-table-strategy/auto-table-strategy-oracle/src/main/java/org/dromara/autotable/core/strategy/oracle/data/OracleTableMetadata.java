package org.dromara.autotable.core.strategy.oracle.data;

import org.dromara.autotable.core.strategy.TableMetadata;

public class OracleTableMetadata extends TableMetadata {
    public OracleTableMetadata(Class<?> entityClass, String tableName, String schema, String comment) {
        super(entityClass, tableName, schema, comment);
    }
}
