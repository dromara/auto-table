package org.dromara.autotable.core.strategy.oracle.builder;

import lombok.NonNull;
import org.dromara.autotable.core.builder.ColumnMetadataBuilder;
import org.dromara.autotable.core.builder.IndexMetadataBuilder;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.strategy.oracle.data.OracleColumnMetadata;
import org.dromara.autotable.core.strategy.oracle.data.OracleTableMetadata;
import org.dromara.autotable.core.utils.BeanClassUtil;
import org.dromara.autotable.core.utils.TableMetadataHandler;

import java.lang.reflect.Field;
import java.util.List;

/**
 *  源数据构建器
 * @author KThirty
 * @since 2025/5/22 15:49
 */
public class TableMetadataBuilder {
    public static @NonNull OracleTableMetadata build(Class<?> clazz) {
        String tableName = TableMetadataHandler.getTableName(clazz);
        String tableComment = TableMetadataHandler.getTableComment(clazz);
        OracleTableMetadata oracleTableMetadata = new OracleTableMetadata(clazz, tableName, "", tableComment);

        // 字段
        ColumnMetadataBuilder columnMetadataBuilder = new OracleColumnMetadataBuilder();
        List<Field> fields = BeanClassUtil.sortAllFieldForColumn(clazz);
        List<OracleColumnMetadata> columnMetadata = columnMetadataBuilder.buildList(clazz, fields);
        oracleTableMetadata.setColumnMetadataList(columnMetadata);
        // 索引
        List<IndexMetadata> indexMetadataList = new IndexMetadataBuilder().buildList(clazz, fields);
        oracleTableMetadata.setIndexMetadataList(indexMetadataList);
        return oracleTableMetadata;
    }
}
