package org.dromara.autotable.strategy.mysql.builder;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.annotation.mysql.MysqlCharset;
import org.dromara.autotable.annotation.mysql.MysqlEngine;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.builder.DefaultTableMetadataBuilder;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.utils.BeanClassUtil;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.strategy.mysql.data.MysqlTableMetadata;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author don
 */
@Slf4j
public class MysqlTableMetadataBuilder extends DefaultTableMetadataBuilder {

    public MysqlTableMetadataBuilder() {
        super(new MysqlColumnMetadataBuilder(), new MysqlIndexMetadataBuilder());
    }

    @Override
    public MysqlTableMetadata build(Class<?> clazz) {

        String tableName = getTableName(clazz);
        String tableSchema = getTableSchema(clazz);
        String tableComment = getTableComment(clazz);
        MysqlTableMetadata mysqlTableMetadata = new MysqlTableMetadata(clazz, tableName, tableSchema, tableComment);

        List<Field> fields = BeanClassUtil.sortAllFieldForColumn(clazz);

        fillColumnMetadataList(clazz, mysqlTableMetadata, fields);
        fillIndexMetadataList(clazz, mysqlTableMetadata, fields);

        fillMysqlTableProperties(clazz, mysqlTableMetadata);

        return mysqlTableMetadata;
    }

    private static void fillMysqlTableProperties(Class<?> clazz, MysqlTableMetadata mysqlTableMetadata) {

        // 设置表字符集
        String charset;
        String collate;
        MysqlCharset mysqlCharsetAnno = AutoTableGlobalConfig.instance().getAutoTableAnnotationFinder().find(clazz, MysqlCharset.class);
        if (mysqlCharsetAnno != null) {
            charset = mysqlCharsetAnno.charset();
            collate = mysqlCharsetAnno.collate();
        } else {
            PropertyConfig autoTableProperties = AutoTableGlobalConfig.instance().getAutoTableProperties();
            charset = autoTableProperties.getMysql().getTableDefaultCharset();
            collate = autoTableProperties.getMysql().getTableDefaultCollation();
        }
        if (StringUtils.hasText(charset)) {
            mysqlTableMetadata.setCharacterSet(charset);
        }
        if (StringUtils.hasText(collate)) {
            mysqlTableMetadata.setCollate(collate);
        }

        // 获取表引擎
        MysqlEngine mysqlEngine = AutoTableGlobalConfig.instance().getAutoTableAnnotationFinder().find(clazz, MysqlEngine.class);
        if (mysqlEngine != null) {
            mysqlTableMetadata.setEngine(mysqlEngine.value());
        }
    }
}
