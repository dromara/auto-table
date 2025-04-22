package org.dromara.autotable.core.strategy.h2.builder;

import org.dromara.autotable.core.builder.ColumnMetadataBuilder;
import org.dromara.autotable.core.builder.DefaultTableMetadataBuilder;
import org.dromara.autotable.core.builder.IndexMetadataBuilder;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.dynamicds.SqlSessionFactoryManager;
import org.dromara.autotable.core.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.Configuration;

import java.sql.Connection;

/**
 * @author don
 */
@Slf4j
public class H2TableMetadataBuilder extends DefaultTableMetadataBuilder {

    public H2TableMetadataBuilder() {
        super(new ColumnMetadataBuilder(DatabaseDialect.H2), new IndexMetadataBuilder());
    }

    // @Override
    // protected String getTableName(Class<?> clazz) {
    //     String tableName = super.getTableName(clazz);
    //     // 强调：必须大写
    //     return tableName.toUpperCase();
    // }

    @Override
    protected String getTableSchema(Class<?> clazz) {
        String tableSchema = super.getTableSchema(clazz);
        if (StringUtils.noText(tableSchema)) {
            // 获取Configuration对象
            Configuration configuration = SqlSessionFactoryManager.getSqlSessionFactory().getConfiguration();
            try (Connection connection = configuration.getEnvironment().getDataSource().getConnection()) {
                // 通过连接获取DatabaseMetaData对象
                return connection.getSchema();
            } catch (Exception e) {
                log.error("获取数据库信息失败", e);
            }
            return "PUBLIC";
        }
        return tableSchema;
    }
}
