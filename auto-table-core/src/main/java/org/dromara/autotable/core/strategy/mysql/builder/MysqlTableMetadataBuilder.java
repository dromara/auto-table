package org.dromara.autotable.core.strategy.mysql.builder;

import org.dromara.autotable.annotation.mysql.MysqlCharset;
import org.dromara.autotable.annotation.mysql.MysqlEngine;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.builder.IndexMetadataBuilder;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.strategy.mysql.data.MysqlColumnMetadata;
import org.dromara.autotable.core.strategy.mysql.data.MysqlTableMetadata;
import org.dromara.autotable.core.utils.BeanClassUtil;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.core.utils.TableMetadataHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author don
 */
@Slf4j
public class MysqlTableMetadataBuilder {

    public static MysqlTableMetadata build(Class<?> clazz) {

        String tableName = TableMetadataHandler.getTableName(clazz);
        String tableComment = TableMetadataHandler.getTableComment(clazz);
        MysqlTableMetadata mysqlTableMetadata = new MysqlTableMetadata(clazz, tableName, tableComment);

        // 设置表字符集
        String charset;
        String collate;
        MysqlCharset mysqlCharsetAnno = AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(clazz, MysqlCharset.class);
        if (mysqlCharsetAnno != null) {
            charset = mysqlCharsetAnno.charset();
            collate = mysqlCharsetAnno.collate();
        } else {
            PropertyConfig autoTableProperties = AutoTableGlobalConfig.getAutoTableProperties();
            charset = autoTableProperties.getMysql().getTableDefaultCharset();
            collate = autoTableProperties.getMysql().getTableDefaultCollation();
        }
        if (StringUtils.hasText(charset) && StringUtils.hasText(collate)) {
            // 获取表字符集
            mysqlTableMetadata.setCharacterSet(charset);
            // 字符排序
            mysqlTableMetadata.setCollate(collate);
        }

        // 获取表引擎
        MysqlEngine mysqlEngine = AutoTableGlobalConfig.getAutoTableAnnotationFinder().find(clazz, MysqlEngine.class);
        if (mysqlEngine != null) {
            mysqlTableMetadata.setEngine(mysqlEngine.value());
        }

        List<Field> fields = BeanClassUtil.sortAllFieldForColumn(clazz);

        List<MysqlColumnMetadata> columnMetadataList = new MysqlColumnMetadataBuilder().buildList(clazz, fields);
        mysqlTableMetadata.setColumnMetadataList(columnMetadataList);
        List<IndexMetadata> indexMetadataList = new IndexMetadataBuilder().buildList(clazz, fields);
        mysqlTableMetadata.setIndexMetadataList(indexMetadataList);

        return mysqlTableMetadata;
    }
}
