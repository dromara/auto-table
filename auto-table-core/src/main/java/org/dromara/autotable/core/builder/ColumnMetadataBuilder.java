package org.dromara.autotable.core.builder;

import org.dromara.autotable.annotation.ColumnDefault;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.utils.TableMetadataHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 用于存放创建表的字段信息
 *
 * @author don
 */
@Slf4j
public class ColumnMetadataBuilder {

    protected final String databaseDialect;

    public ColumnMetadataBuilder(String databaseDialect) {
        this.databaseDialect = databaseDialect;
    }

    public <T extends ColumnMetadata> List<T> buildList(Class<?> clazz, List<Field> fields) {

        AtomicInteger index = new AtomicInteger(1);
        List<ColumnMetadata> columnMetadata = fields.stream()
                .filter(field -> TableMetadataHandler.isIncludeField(field, clazz))
                .map(field -> this.build(clazz, field, index.getAndIncrement()))
                .collect(Collectors.toList());

        if (columnMetadata.isEmpty()) {
            log.warn("扫描发现{}没有建表字段请注意！", clazz.getName());
        }

        return (List<T>) columnMetadata;
    }

    public ColumnMetadata build(Class<?> clazz, Field field, int position) {

        ColumnMetadata columnMetadata = newColumnMetadata();
        DatabaseTypeAndLength typeAndLength = getTypeAndLength(databaseDialect, clazz, field);
        columnMetadata.setName(TableMetadataHandler.getColumnName(clazz, field))
                .setComment(TableMetadataHandler.getColumnComment(field, clazz))
                .setType(typeAndLength)
                .setNotNull(TableMetadataHandler.isNotNull(field, clazz))
                .setPrimary(TableMetadataHandler.isPrimary(field, clazz))
                .setAutoIncrement(TableMetadataHandler.isAutoIncrement(field, clazz));
        ColumnDefault columnDefault = TableMetadataHandler.getColumnDefaultValue(field, clazz);
        if (columnDefault != null) {

            DefaultValueEnum defaultValueType = columnDefault.type();
            columnMetadata.setDefaultValueType(defaultValueType);

            String defaultValue = getDefaultValue(typeAndLength, columnDefault);
            columnMetadata.setDefaultValue(defaultValue);
        }

        // 预留填充逻辑
        customBuild(columnMetadata, clazz, field, position);

        return columnMetadata;
    }

    protected void customBuild(ColumnMetadata columnMetadata, Class<?> clazz, Field field, int position) {

    }

    protected DatabaseTypeAndLength getTypeAndLength(String databaseDialect, Class<?> clazz, Field field) {
        return AutoTableGlobalConfig.instance().getJavaTypeToDatabaseTypeConverter().convert(databaseDialect, clazz, field);
    }

    protected String getDefaultValue(DatabaseTypeAndLength typeAndLength, ColumnDefault columnDefault) {
        String defaultValue = columnDefault.value();
        // 因为空字符串，必须由DefaultValueEnum.EMPTY_STRING来表示，所以这里要特殊处理
        if (defaultValue != null && defaultValue.isEmpty()) {
            defaultValue = null;
        }
        return defaultValue;
    }

    protected ColumnMetadata newColumnMetadata() {
        return new ColumnMetadata();
    }
}
