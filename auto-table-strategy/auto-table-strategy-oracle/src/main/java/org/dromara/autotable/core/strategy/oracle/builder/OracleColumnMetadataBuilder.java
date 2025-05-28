package org.dromara.autotable.core.strategy.oracle.builder;

import org.dromara.autotable.core.builder.ColumnMetadataBuilder;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.oracle.data.OracleColumnMetadata;

import java.lang.reflect.Field;

public class OracleColumnMetadataBuilder extends ColumnMetadataBuilder {
    public OracleColumnMetadataBuilder() {
        super(DatabaseDialect.Oracle);
    }

    @Override
    protected OracleColumnMetadata newColumnMetadata() {
        return new OracleColumnMetadata();
    }

    @Override
    protected void customBuild(ColumnMetadata columnMetadata, Class<?> clazz, Field field, int position) {
        OracleColumnMetadata mysqlColumnMetadata = (OracleColumnMetadata) columnMetadata;
        // 列顺序位置
        mysqlColumnMetadata.setPosition(position);
        mysqlColumnMetadata.setFieldType(field.getType());
    }
}
