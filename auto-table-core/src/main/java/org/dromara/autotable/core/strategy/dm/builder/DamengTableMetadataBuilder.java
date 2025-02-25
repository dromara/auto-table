package org.dromara.autotable.core.strategy.dm.builder;

/**
 * @author Min, Freddy
 * @date: 2025/2/25 23:00
 */

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.Configuration;
import org.dromara.autotable.core.builder.DefaultTableMetadataBuilder;
import org.dromara.autotable.core.builder.IndexMetadataBuilder;
import org.dromara.autotable.core.dynamicds.SqlSessionFactoryManager;
import org.dromara.autotable.core.utils.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 达梦表元数据构建器
 */
@Slf4j
public class DamengTableMetadataBuilder extends DefaultTableMetadataBuilder {

    public DamengTableMetadataBuilder() {
        super(new DamengColumnMetadataBuilder(), new IndexMetadataBuilder());
    }

    @Override
    protected String getTableSchema(Class<?> clazz) {
        String tableSchema = super.getTableSchema(clazz);
        if (StringUtils.noText(tableSchema)) {
            Configuration configuration = SqlSessionFactoryManager.getSqlSessionFactory().getConfiguration();
            try (Connection connection = configuration.getEnvironment().getDataSource().getConnection()) {
                // 达梦获取当前用户的默认schema
                String schema = connection.getMetaData().getUserName().toUpperCase();
                return schema.equals("SYSDBA") ? null : schema; // SYSDBA用户默认不指定schema
            } catch (SQLException e) {
                log.error("获取达梦数据库schema失败", e);
                return null; // 达梦不指定schema时使用默认用户空间
            }
        }
        return tableSchema;
    }

}