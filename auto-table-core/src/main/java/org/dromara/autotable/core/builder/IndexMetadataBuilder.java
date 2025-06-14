package org.dromara.autotable.core.builder;

import lombok.SneakyThrows;
import org.dromara.autotable.annotation.Index;
import org.dromara.autotable.annotation.IndexField;
import org.dromara.autotable.annotation.TableIndex;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.utils.IndexRepeatChecker;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.core.utils.TableMetadataHandler;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author don
 */
public class IndexMetadataBuilder {

    public <T extends IndexMetadata> List<T> buildList(Class<?> clazz, List<Field> fields) {

        IndexRepeatChecker indexRepeatChecker = IndexRepeatChecker.of();

        List<IndexMetadata> indexMetadataList = new ArrayList<>(16);

        // 类上的索引注解
        List<IndexMetadata> onClassIndexMetadata = buildFromClass(clazz, indexRepeatChecker);
        indexMetadataList.addAll(onClassIndexMetadata);

        // 字段上的索引注解
        List<IndexMetadata> onFieldIndexMetadata = buildFromField(clazz, fields, indexRepeatChecker);
        indexMetadataList.addAll(onFieldIndexMetadata);

        return (List<T>) indexMetadataList;
    }

    protected List<IndexMetadata> buildFromField(Class<?> clazz, List<Field> fields, IndexRepeatChecker indexRepeatChecker) {
        return fields.stream()
                .filter(field -> TableMetadataHandler.isIncludeField(field, clazz))
                .map(field -> buildIndexMetadata(clazz, field))
                .filter(Objects::nonNull)
                .peek(indexMetadata -> indexRepeatChecker.filter(indexMetadata.getName()))
                .collect(Collectors.toList());
    }

    protected List<IndexMetadata> buildFromClass(Class<?> clazz, IndexRepeatChecker indexRepeatChecker) {
        List<TableIndex> tableIndexes = TableMetadataHandler.getTableIndexes(clazz);
        return tableIndexes.stream()
                .map(tableIndex -> buildIndexMetadata(clazz, tableIndex))
                .filter(Objects::nonNull)
                .peek(indexMetadata -> indexRepeatChecker.filter(indexMetadata.getName()))
                .collect(Collectors.toList());
    }

    protected IndexMetadata buildIndexMetadata(Class<?> clazz, Field field) {
        // 获取当前字段的@Index注解
        Index index = TableMetadataHandler.getIndex(field);
        if (null != index) {
            String realColumnName = TableMetadataHandler.getColumnName(clazz, field);
            IndexMetadata indexMetadata = newIndexMetadata();
            String indexName = getIndexName(clazz, field, index);
            indexMetadata.setName(indexName);
            indexMetadata.setType(index.type());
            indexMetadata.setMethod(index.method());
            indexMetadata.setComment(index.comment());
            indexMetadata.getColumns().add(IndexMetadata.IndexColumnParam.newInstance(realColumnName, null));
            return indexMetadata;
        }
        return null;
    }

    protected String getIndexName(Class<?> clazz, TableIndex tableIndex) {

        String indexName = tableIndex.name();

        if (StringUtils.hasText(indexName)) {
            // 手动指定了索引名
            return getIndexNameWithPrefix(indexName);
        } else {
            // 自动生成索引名
            String filedNames = Stream.concat(Arrays.stream(tableIndex.indexFields()).map(IndexField::field), Arrays.stream(tableIndex.fields()))
                    .map(fieldName -> TableMetadataHandler.getColumnName(clazz, fieldName))
                    .collect(Collectors.joining("_"));
            String tableName = TableMetadataHandler.getTableName(clazz);
            return getEncryptIndexName(tableName, filedNames);
        }
    }

    protected String getIndexName(Class<?> clazz, Field field, Index index) {

        String indexName = index.name();

        if (StringUtils.hasText(indexName)) {
            // 手动指定了索引名
            return getIndexNameWithPrefix(indexName);
        } else {
            // 自动生成索引名
            String realColumnName = TableMetadataHandler.getColumnName(clazz, field);
            String tableName = TableMetadataHandler.getTableName(clazz);
            return getEncryptIndexName(tableName, realColumnName);
        }
    }

    private static String getIndexNameWithPrefix(String indexName) {
        String indexPrefix = AutoTableGlobalConfig.instance().getAutoTableProperties().getIndexPrefix();
        String fullIndexName = indexPrefix + indexName;
        return replaceDoubleQuote(fullIndexName);
    }

    protected String getEncryptIndexName(String tableNamePart, String filedNamePart) {
        String prefix = AutoTableGlobalConfig.instance().getAutoTableProperties().getIndexPrefix();
        // 获取当前数据库策略的最大索引名称长度
        int maxLength = IStrategy.getCurrentStrategy().indexNameMaxLength();
        if (prefix.length() > maxLength) {
            throw new RuntimeException("索引名前缀[" + prefix + "]超出索引名最大长度（" + maxLength + "），请手动指定索引名称");
        }

        String main = tableNamePart + "_" + filedNamePart;
        String fullIndexName = replaceDoubleQuote(prefix + main);
        if (fullIndexName.length() > maxLength) {
            // 索引名称长度超出限制，使用22位MD5进行截取
            String md5 = generateShortMD5(main);

            if (prefix.length() + md5.length() > maxLength) {
                // 注意此种情况下，裁切后的md5唯一性降低
                return prefix + md5.substring(0, maxLength - prefix.length());
            }

            // 截取原名称前半部分长度的字符，空余足够的位置，给“_”和MD5值
            String prePart = fullIndexName.substring(0, maxLength - md5.length() - 1);
            return prePart + "_" + md5;
        }

        return fullIndexName;
    }

    /**
     * 替换字符串中的双引号为两个双引号
     */
    public static String replaceDoubleQuote(String input) {

        if (input == null || input.isEmpty()) {
            return input; // 空字符串或null直接返回
        }

        //
        return input.replace("\"", "\"\"");
    }

    @SneakyThrows
    private String generateMD5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }
    }

    private String generateShortMD5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    protected IndexMetadata buildIndexMetadata(Class<?> clazz, TableIndex tableIndex) {

        // 获取当前字段的@Index注解
        if (null != tableIndex && (tableIndex.fields().length > 0 || tableIndex.indexFields().length > 0)) {

            List<IndexMetadata.IndexColumnParam> columnParams = getColumnParams(clazz, tableIndex);

            IndexMetadata indexMetadata = newIndexMetadata();
            indexMetadata.setName(getIndexName(clazz, tableIndex));
            indexMetadata.setType(tableIndex.type());
            indexMetadata.setComment(tableIndex.comment());
            indexMetadata.setColumns(columnParams);
            return indexMetadata;
        }
        return null;
    }

    protected IndexMetadata newIndexMetadata() {
        return new IndexMetadata();
    }

    protected List<IndexMetadata.IndexColumnParam> getColumnParams(Class<?> clazz, final TableIndex tableIndex) {
        List<IndexMetadata.IndexColumnParam> columnParams = new ArrayList<>();
        // 防止 两种模式设置的字段有冲突
        Set<String> exitsColumns = new HashSet<>();
        // 优先获取 带排序方式的字段
        IndexField[] sortFields = tableIndex.indexFields();
        if (sortFields.length > 0) {
            columnParams.addAll(
                    Arrays.stream(sortFields)
                            .map(sortField -> {
                                String realColumnName = TableMetadataHandler.getColumnName(clazz, sortField.field());
                                // 重复字段，自动排除忽略掉
                                if (exitsColumns.contains(realColumnName)) {
                                    return null;
                                }
                                exitsColumns.add(realColumnName);
                                return IndexMetadata.IndexColumnParam.newInstance(realColumnName, sortField.sort());
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
            );
        }
        // 其次获取 简单模式的字段，如果重复了，跳过，以带排序方式的为准
        String[] fields = tableIndex.fields();
        if (fields.length > 0) {
            columnParams.addAll(
                    Arrays.stream(fields)
                            .map(field -> {
                                String realColumnName = TableMetadataHandler.getColumnName(clazz, field);
                                // 重复字段，自动排除忽略掉
                                if (exitsColumns.contains(realColumnName)) {
                                    return null;
                                }
                                exitsColumns.add(realColumnName);
                                return IndexMetadata.IndexColumnParam.newInstance(realColumnName, null);
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
            );
        }

        return columnParams;
    }
}
