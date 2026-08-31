package org.dromara.autotable.strategy.yashandb.builder;

import org.dromara.autotable.core.builder.DefaultTableMetadataBuilder;
import org.dromara.autotable.core.builder.IndexMetadataBuilder;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.utils.StringUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Builds table metadata and resolves the active YashanDB schema when omitted.
 */
public class YashanTableMetadataBuilder extends DefaultTableMetadataBuilder {
    public YashanTableMetadataBuilder() {
        super(new YashanColumnMetadataBuilder(), new IndexMetadataBuilder());
    }

    /**
     * Uses the configured schema or resolves the current schema from YashanDB.
     */
    @Override
    protected String getTableSchema(Class<?> clazz) {
        String configured = super.getTableSchema(clazz);
        if (StringUtils.hasText(configured)) {
            return configured;
        }
        // The JDBC connection schema is the fallback when SYS_CONTEXT returns no value.
        return DataSourceManager.useConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT SYS_CONTEXT('USERENV','CURRENT_SCHEMA') FROM DUAL");
                 ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && StringUtils.hasText(resultSet.getString(1))) {
                    return resultSet.getString(1);
                }
                return connection.getSchema();
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to resolve the current YashanDB schema", exception);
            }
        });
    }
}
