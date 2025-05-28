package org.dromara.autotable.core.strategy.oracle.builder;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.builder.ColumnMetadataBuilder;
import org.dromara.autotable.core.builder.DefaultTableMetadataBuilder;
import org.dromara.autotable.core.builder.IndexMetadataBuilder;
import org.dromara.autotable.core.constants.DatabaseDialect;


@Slf4j
public class OracleMetadataBuilder extends DefaultTableMetadataBuilder {

    public OracleMetadataBuilder() {
        super(new ColumnMetadataBuilder(DatabaseDialect.Oracle), new IndexMetadataBuilder());
    }



}
