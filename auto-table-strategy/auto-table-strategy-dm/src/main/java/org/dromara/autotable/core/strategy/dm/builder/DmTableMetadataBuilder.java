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
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 达梦表元数据构建器
 */
@Slf4j
public class DmTableMetadataBuilder extends DefaultTableMetadataBuilder {

    public DmTableMetadataBuilder() {
        super(new DmColumnMetadataBuilder(), new IndexMetadataBuilder());
    }

    @Override
    protected String getTableSchema(Class<?> clazz) {
        String tableSchema = super.getTableSchema(clazz);
        if (StringUtils.noText(tableSchema)) {
            Configuration configuration = SqlSessionFactoryManager.getSqlSessionFactory().getConfiguration();
            try (Connection connection = configuration.getEnvironment().getDataSource().getConnection()) {
                // 优先从URL参数获取schema
                String url = connection.getMetaData().getURL();
                String schemaFromUrl = parseSchemaFromUrl(url);

                if (StringUtils.hasText(schemaFromUrl)) {
                    return schemaFromUrl.toLowerCase();
                }
                // 从连接元数据获取用户名作为schema
                return connection.getMetaData().getUserName().toUpperCase();
            } catch (SQLException e) {
                log.error("获取达梦数据库schema失败", e);
                return null;
            }
        }
        return tableSchema;
    }

    /**
     * 从JDBC URL解析schema参数
     * 支持格式：
     * jdbc:dm://host:port?schema=myschema
     * jdbc:dm://host:port?param1=value1&currentSchema=myschema
     */
    private String parseSchemaFromUrl(String url) {
        try {
            // 分离URL参数部分
            String[] urlParts = url.split("\\?");
            if (urlParts.length < 2) {
                return null;
            }

            // 解析参数键值对
            Map<String, String> params = Arrays.stream(urlParts[1].split("&"))
                    .map(param -> param.split("="))
                    .filter(pair -> pair.length == 2)
                    .collect(Collectors.toMap(
                            pair -> pair[0].toLowerCase(),
                            pair -> pair[1],
                            (oldVal, newVal) -> newVal));

            // 尝试从不同参数名获取schema
            return params.getOrDefault("schema",
                    params.getOrDefault("currentSchema", null));
        } catch (Exception e) {
            log.warn("解析JDBC URL参数失败: {}", url, e);
            return null;
        }
    }
}