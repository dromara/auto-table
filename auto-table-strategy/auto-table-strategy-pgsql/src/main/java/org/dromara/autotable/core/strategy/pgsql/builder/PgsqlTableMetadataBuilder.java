package org.dromara.autotable.core.strategy.pgsql.builder;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.builder.DefaultTableMetadataBuilder;
import org.dromara.autotable.core.builder.IndexMetadataBuilder;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.utils.StringUtils;

/**
 * @author don
 */
@Slf4j
public class PgsqlTableMetadataBuilder extends DefaultTableMetadataBuilder {

    public PgsqlTableMetadataBuilder() {
        super(new PgsqlColumnMetadataBuilder(), new IndexMetadataBuilder());
    }

    @Override
    protected String getTableSchema(Class<?> clazz) {
        String tableSchema = super.getTableSchema(clazz);
        if (StringUtils.hasText(tableSchema)) {
            return tableSchema;
        }

        return DataSourceManager.useConnection(connection -> {
            try {
                // 通过连接获取DatabaseMetaData对象
                return connection.getSchema();
            } catch (Exception e) {
                log.error("获取数据库信息失败", e);
            }
            return "public";
        });
    }
}
