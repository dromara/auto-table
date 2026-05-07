package org.dromara.autotable.strategy.mysql.builder;

import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.annotation.TableIndex;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.annotation.mysql.MysqlFullTextIndex;
import org.dromara.autotable.annotation.mysql.MysqlTableFullTextIndex;
import org.dromara.autotable.annotation.mysql.MysqlTableFullTextIndexes;
import org.dromara.autotable.core.AutoTableAnnotationFinder;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.builder.IndexMetadataBuilder;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.core.utils.TableMetadataHandler;
import org.dromara.autotable.strategy.mysql.data.MysqlIndexMetadata;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * MySQL 索引元数据构建器
 *
 * @author don
 */
@Slf4j
public class MysqlIndexMetadataBuilder extends IndexMetadataBuilder {

    @Override
    protected IndexMetadata newIndexMetadata() {
        return new MysqlIndexMetadata();
    }

    @Override
    protected List<IndexMetadata> buildFromClass(Class<?> clazz, org.dromara.autotable.core.utils.IndexRepeatChecker indexRepeatChecker) {
        // 先获取父类处理的普通索引
        List<IndexMetadata> result = new ArrayList<>(super.buildFromClass(clazz, indexRepeatChecker));

        // 处理 @MysqlTableFullTextIndex
        List<MysqlTableFullTextIndex> fullTextIndexes = getMysqlTableFullTextIndexes(clazz);
        for (MysqlTableFullTextIndex fullTextIndex : fullTextIndexes) {
            IndexMetadata indexMetadata = buildIndexMetadata(clazz, fullTextIndex);
            if (indexMetadata != null) {
                indexRepeatChecker.filter(indexMetadata.getName());
                result.add(indexMetadata);
            }
        }

        return result;
    }

    private List<MysqlTableFullTextIndex> getMysqlTableFullTextIndexes(Class<?> clazz) {
        List<MysqlTableFullTextIndex> result = new ArrayList<>();
        AutoTableAnnotationFinder autoTableAnnotationFinder = AutoTableGlobalConfig.instance().getAutoTableAnnotationFinder();
        MysqlTableFullTextIndexes mysqlTableFullTextIndexes = autoTableAnnotationFinder.find(clazz, MysqlTableFullTextIndexes.class);
        if (mysqlTableFullTextIndexes != null) {
            Collections.addAll(result, mysqlTableFullTextIndexes.value());
        }
        MysqlTableFullTextIndex mysqlTableFullTextIndex = autoTableAnnotationFinder.find(clazz, MysqlTableFullTextIndex.class);
        if (mysqlTableFullTextIndex != null) {
            result.add(mysqlTableFullTextIndex);
        }
        return result;
    }

    private IndexMetadata buildIndexMetadata(Class<?> clazz, MysqlTableFullTextIndex fullTextIndex) {
        String[] fieldNames = fullTextIndex.fields();
        if (fieldNames.length == 0) {
            return null;
        }

        List<IndexMetadata.IndexColumnParam> columnParams = Arrays.stream(fieldNames)
                .map(fieldName -> TableMetadataHandler.getColumnName(clazz, fieldName))
                .filter(Objects::nonNull)
                .map(columnName -> IndexMetadata.IndexColumnParam.newInstance(columnName, null))
                .collect(Collectors.toList());

        if (columnParams.isEmpty()) {
            return null;
        }

        MysqlIndexMetadata indexMetadata = (MysqlIndexMetadata) newIndexMetadata();
        String indexName = fullTextIndex.name();
        if (StringUtils.hasText(indexName)) {
            indexName = getIndexNameWithPrefix(indexName);
        } else {
            String fieldNamesStr = String.join("_", fieldNames);
            String tableName = TableMetadataHandler.getTableName(clazz);
            indexName = getEncryptIndexName(tableName, fieldNamesStr);
        }
        indexMetadata.setName(indexName);
        indexMetadata.setType(org.dromara.autotable.annotation.enums.IndexTypeEnum.NORMAL);
        indexMetadata.setFullText(true);
        indexMetadata.setParser(fullTextIndex.parser());
        indexMetadata.setComment(fullTextIndex.comment());
        indexMetadata.setColumns(columnParams);
        return indexMetadata;
    }

    @Override
    protected IndexMetadata buildIndexMetadata(Class<?> clazz, Field field) {
        MysqlFullTextIndex fullTextIndex = AutoTableGlobalConfig.instance().getAutoTableAnnotationFinder().find(field, MysqlFullTextIndex.class);
        if (fullTextIndex != null) {
            // 存在 @MysqlFullTextIndex，创建全文索引
            String realColumnName = TableMetadataHandler.getColumnName(clazz, field);
            MysqlIndexMetadata indexMetadata = (MysqlIndexMetadata) newIndexMetadata();
            String indexName = fullTextIndex.name();
            if (StringUtils.hasText(indexName)) {
                indexName = getIndexNameWithPrefix(indexName);
            } else {
                String tableName = TableMetadataHandler.getTableName(clazz);
                indexName = getEncryptIndexName(tableName, realColumnName);
            }
            indexMetadata.setName(indexName);
            indexMetadata.setType(IndexTypeEnum.NORMAL);
            indexMetadata.setFullText(true);
            indexMetadata.setParser(fullTextIndex.parser());
            indexMetadata.setComment(fullTextIndex.comment());
            indexMetadata.getColumns().add(IndexMetadata.IndexColumnParam.newInstance(realColumnName, null));
            return indexMetadata;
        }

        // 不存在 @MysqlFullTextIndex，走默认逻辑
        return super.buildIndexMetadata(clazz, field);
    }

    @Override
    protected void customBuild(IndexMetadata indexMetadata, Class<?> clazz, Field field) {
        // 全文索引在 buildIndexMetadata 中已经处理，这里不需要额外操作
    }

    @Override
    protected void customBuild(IndexMetadata indexMetadata, Class<?> clazz, TableIndex tableIndex) {
        // 类级别的 @TableIndex 不支持 @MysqlFullTextIndex
    }
}
