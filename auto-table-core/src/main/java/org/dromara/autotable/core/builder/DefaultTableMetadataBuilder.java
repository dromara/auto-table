package org.dromara.autotable.core.builder;

import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.utils.BeanClassUtil;
import org.dromara.autotable.core.utils.TableMetadataHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author don
 */
@Slf4j
public class DefaultTableMetadataBuilder {

    protected final ColumnMetadataBuilder columnMetadataBuilder;
    protected final IndexMetadataBuilder indexMetadataBuilder;

    public DefaultTableMetadataBuilder(ColumnMetadataBuilder columnMetadataBuilder, IndexMetadataBuilder indexMetadataBuilder) {
        this.columnMetadataBuilder = columnMetadataBuilder;
        this.indexMetadataBuilder = indexMetadataBuilder;
    }

    public DefaultTableMetadata build(Class<?> clazz) {

        String tableName = getTableName(clazz);
        String tableSchema = getTableSchema(clazz);
        String tableComment = getTableComment(clazz);
        DefaultTableMetadata tableMetadata = new DefaultTableMetadata(clazz, tableName, tableSchema, tableComment);

        List<Field> fields = BeanClassUtil.listAllFieldForColumn(clazz);

        // 填充字段
        fillColumnMetadataList(clazz, tableMetadata, fields);

        // 填充索引
        fillIndexMetadataList(clazz, tableMetadata, fields);

        return tableMetadata;
    }

    protected String getTableComment(Class<?> clazz) {
        return TableMetadataHandler.getTableComment(clazz);
    }

    protected String getTableSchema(Class<?> clazz) {
        return TableMetadataHandler.getTableSchema(clazz);
    }

    protected String getTableName(Class<?> clazz) {
        return TableMetadataHandler.getTableName(clazz);
    }

    protected void fillIndexMetadataList(Class<?> clazz, DefaultTableMetadata tableMetadata, List<Field> fields) {
        List<IndexMetadata> indexMetadataList = indexMetadataBuilder.buildList(clazz, fields);
        tableMetadata.setIndexMetadataList(indexMetadataList);
    }

    protected void fillColumnMetadataList(Class<?> clazz, DefaultTableMetadata tableMetadata, List<Field> fields) {
        List<ColumnMetadata> columnMetadataList = columnMetadataBuilder.buildList(clazz, fields);
        tableMetadata.setColumnMetadataList(columnMetadataList);
    }
}
