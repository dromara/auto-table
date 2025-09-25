package org.dromara.autotable.strategy.h2.builder;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.builder.ColumnMetadataBuilder;
import org.dromara.autotable.core.builder.DefaultTableMetadataBuilder;
import org.dromara.autotable.core.builder.IndexMetadataBuilder;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.utils.StringUtils;

import java.sql.SQLException;

/**
 * @author don
 */
@Slf4j
public class H2TableMetadataBuilder extends DefaultTableMetadataBuilder {

    public H2TableMetadataBuilder() {
        super(new ColumnMetadataBuilder(DatabaseDialect.H2), new IndexMetadataBuilder());
    }

    @Override
    protected String getTableSchema(Class<?> clazz) {
        String tableSchema = super.getTableSchema(clazz);

        if (StringUtils.hasText(tableSchema)) {
            return tableSchema;
        }

        return DataSourceManager.useConnection(connection -> {
            try {
                return connection.getSchema();
            } catch (SQLException e) {
                log.error("获取数据库信息失败", e);
            }
            return "PUBLIC";
        });
    }
}
