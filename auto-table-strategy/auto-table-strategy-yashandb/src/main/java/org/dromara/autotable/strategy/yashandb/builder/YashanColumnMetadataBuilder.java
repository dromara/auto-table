package org.dromara.autotable.strategy.yashandb.builder;

import org.dromara.autotable.core.builder.ColumnMetadataBuilder;
import org.dromara.autotable.core.constants.DatabaseDialect;

/**
 * Builds AutoTable column metadata using the YashanDB MySQL-compatible dialect.
 */
public class YashanColumnMetadataBuilder extends ColumnMetadataBuilder {
    public YashanColumnMetadataBuilder() {
        super(DatabaseDialect.YashanDB);
    }
}
