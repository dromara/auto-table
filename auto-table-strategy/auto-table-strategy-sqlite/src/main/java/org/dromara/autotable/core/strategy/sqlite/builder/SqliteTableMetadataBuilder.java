package org.dromara.autotable.core.strategy.sqlite.builder;

import org.dromara.autotable.core.builder.IndexMetadataBuilder;
import org.dromara.autotable.core.builder.DefaultTableMetadataBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author don
 */
@Slf4j
public class SqliteTableMetadataBuilder extends DefaultTableMetadataBuilder {

    public SqliteTableMetadataBuilder() {
        super(new SqliteColumnMetadataBuilder(), new IndexMetadataBuilder());
    }
}
