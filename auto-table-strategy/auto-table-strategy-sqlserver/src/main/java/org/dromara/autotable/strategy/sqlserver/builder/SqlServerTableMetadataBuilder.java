package org.dromara.autotable.strategy.sqlserver.builder;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.core.builder.DefaultTableMetadataBuilder;
import org.dromara.autotable.core.builder.IndexMetadataBuilder;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.utils.StringUtils;

/**
 * SQLServer 表元数据构造器。
 *
 * <p>方言适配：未显式指定 schema 时，取连接当前 schema（SQLServer 默认 dbo）。</p>
 *
 * @author don
 */
@Slf4j
public class SqlServerTableMetadataBuilder extends DefaultTableMetadataBuilder {

    public SqlServerTableMetadataBuilder() {
        super(new SqlServerColumnMetadataBuilder(), new IndexMetadataBuilder());
    }

    @Override
    protected String getTableSchema(Class<?> clazz) {
        String tableSchema = super.getTableSchema(clazz);
        if (StringUtils.hasText(tableSchema)) {
            return tableSchema;
        }

        return DataSourceManager.useConnection(connection -> {
            try {
                // 通过连接获取当前 schema（SQLServer 默认 dbo）
                return connection.getSchema();
            } catch (Exception e) {
                log.error("获取数据库信息失败", e);
            }
            return "dbo";
        });
    }
}
