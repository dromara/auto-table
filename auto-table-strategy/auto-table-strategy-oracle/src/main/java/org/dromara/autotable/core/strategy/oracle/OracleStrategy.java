package org.dromara.autotable.core.strategy.oracle;

import lombok.NonNull;
import org.dromara.autotable.annotation.enums.IndexSortTypeEnum;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.builder.ColumnMetadataBuilder;
import org.dromara.autotable.core.builder.DefaultTableMetadataBuilder;
import org.dromara.autotable.core.builder.IndexMetadataBuilder;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
import org.dromara.autotable.core.converter.TypeDefine;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.utils.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * oracle数据库策略实现
 */
public class OracleStrategy implements IStrategy<DefaultTableMetadata, OracleCompareTableInfo> {

    private static final DefaultTableMetadataBuilder tableMetadataBuilder =
            new DefaultTableMetadataBuilder(new ColumnMetadataBuilder(DatabaseDialect.Oracle), new IndexMetadataBuilder());

    @Override
    public String databaseDialect() {
        return DatabaseDialect.Oracle;
    }

    @Override
    public String sqlSeparator() {
        return "";
    }

    @Override
    public Map<Class<?>, DefaultTypeEnumInterface> typeMapping() {
        TypeDefine strType = TypeDefine.of("VARCHAR2", 255);
        TypeDefine boolType = TypeDefine.of("NUMBER", 1, 0);
        TypeDefine shortType = TypeDefine.of("NUMBER", 5, 0);
        TypeDefine intType = TypeDefine.of("NUMBER", 10, 0);
        TypeDefine longType = TypeDefine.of("NUMBER", 19, 0);
        TypeDefine floatType = TypeDefine.of("BINARY_FLOAT");
        TypeDefine doubleType = TypeDefine.of("BINARY_DOUBLE");
        TypeDefine bigDecimalType = TypeDefine.of("NUMBER", 38, 18);
        TypeDefine timestampType = TypeDefine.of("TIMESTAMP", 6);
        TypeDefine dateType = TypeDefine.of("DATE");
        return new HashMap<Class<?>, DefaultTypeEnumInterface>(32) {{
            put(String.class, strType);
            put(Character.class, strType);
            put(char.class, strType);

            put(Boolean.class, boolType);
            put(boolean.class, boolType);

            put(Short.class, shortType);
            put(short.class, shortType);

            put(Integer.class, intType);
            put(int.class, intType);

            put(BigInteger.class, longType);
            put(Long.class, longType);
            put(long.class, longType);

            put(Float.class, floatType);
            put(float.class, floatType);
            put(Double.class, doubleType);
            put(double.class, doubleType);
            put(BigDecimal.class, bigDecimalType);

            put(java.util.Date.class, timestampType);
            put(java.sql.Time.class, timestampType);
            put(java.sql.Date.class, dateType);
            put(java.sql.Timestamp.class, timestampType);
            put(java.time.LocalTime.class, timestampType);
            put(java.time.LocalDate.class, dateType);
            put(java.time.LocalDateTime.class, timestampType);
        }};
    }


    @Override
    public String dropTable(String schema, String tableName) {
        return String.format("DECLARE\n" +
                "    table_count INTEGER;\n" +
                "    seq_count   INTEGER;\n" +
                "BEGIN\n" +
                "    SELECT COUNT(*) INTO table_count FROM user_tables WHERE upper(table_name) = upper('%s');\n" +
                "    IF table_count > 0 THEN\n" +
                "        EXECUTE IMMEDIATE 'DROP TABLE %s';\n" +
                "    END IF;\n" +
                "    SELECT COUNT(*) INTO seq_count FROM user_sequences WHERE upper(sequence_name) = upper('seq_%s');\n" +
                "    IF seq_count > 0 THEN\n" +
                "        EXECUTE IMMEDIATE 'DROP SEQUENCE seq_%s';\n" +
                "    END IF;" +
                "END;", tableName, tableName, tableName, tableName);
    }

    @Override
    public @NonNull DefaultTableMetadata analyseClass(Class<?> beanClass) {
        return tableMetadataBuilder.build(beanClass);
    }

    @Override
    public List<String> createTable(DefaultTableMetadata tableMetadata) {
        List<String> result = new ArrayList<>();
        String tableName = tableMetadata.getTableName();
        String tableComment = Optional.ofNullable(tableMetadata.getComment()).orElse("");
        List<ColumnMetadata> columnMetadataList = tableMetadata.getColumnMetadataList();
        List<IndexMetadata> indexMetadataList = tableMetadata.getIndexMetadataList();
        // 主键字段
        ColumnMetadata primaryKey = columnMetadataList.stream()
                .filter(ColumnMetadata::isPrimary)
                .findFirst()
                .orElse(null);

        // 构建主键自增序列
        if (primaryKey != null && primaryKey.isAutoIncrement()) {
            result.add(String.format("CREATE SEQUENCE seq_%s", tableName));
        }
        // 建表语句
        List<String> columnSqlList = columnMetadataList.stream()
                .map(it -> OracleHelper.SQL.toColumnSql(tableName, it))
                .collect(Collectors.toList());
        result.add(String.format("CREATE TABLE %s (%s)", tableName, String.join(",\n", columnSqlList)));

        // 构建主键约束
        if (primaryKey != null) {
            result.add(String.format("ALTER TABLE %s ADD CONSTRAINT pk_%s PRIMARY KEY(%s)", tableName, tableName, primaryKey.getName()));
        }

        // 表和字段注释
        result.add(String.format("COMMENT ON TABLE %s IS '%s'", tableName, tableComment));
        for (ColumnMetadata columnMetadata : columnMetadataList) {
            String columnName = columnMetadata.getName();
            String columnComment = Optional.ofNullable(columnMetadata.getComment()).orElse("");
            result.add(String.format("COMMENT ON COLUMN %s.%s IS '%s'", tableName, columnName, columnComment));
        }
        // 索引信息
        for (IndexMetadata indexMetadata : indexMetadataList) {
            result.add(OracleHelper.SQL.toIndexSql(tableName, indexMetadata));
        }
        return result;
    }

    @Override
    public @NonNull OracleCompareTableInfo compareTable(DefaultTableMetadata tableMetadata) {
        OracleCompareTableInfo compareTableInfo = new OracleCompareTableInfo(tableMetadata.getTableName(), tableMetadata.getSchema());
        String tableName = tableMetadata.getTableName();
        String newTableComment = Optional.ofNullable(tableMetadata.getComment()).orElse("");

        // 实体主键
        ColumnMetadata newPrimaryKey = tableMetadata.getColumnMetadataList()
                .stream()
                .filter(ColumnMetadata::isPrimary)
                .findAny()
                .orElse(null);

        // 实体字段信息
        List<ColumnMetadata> newColumnList = tableMetadata.getColumnMetadataList();
        Set<String> newColumnNameSet = newColumnList.stream()
                .map(ColumnMetadata::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        // 实体索引信息
        List<IndexMetadata> newIndexList = tableMetadata.getIndexMetadataList();
        Set<String> newIndexNameSet = newIndexList.stream()
                .map(IndexMetadata::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        // 数据库字段信息
        String oldTableComment = Optional.of(TabComment.search(tableName))
                .map(TabComment::getComments)
                .orElse("");
        List<TabColumn> oldColumnList = TabColumn.search(tableName)
                .stream()
                .peek(it -> {
                    String dataDefaultVc = it.getData_default_vc();
                    String seqName = ".\"seq_" + tableName + "\".\"nextval\"";
                    if (StringUtils.hasText(dataDefaultVc)
                            && dataDefaultVc.toLowerCase().endsWith(seqName.toLowerCase())) {
                        it.setData_default_vc("seq_" + tableName + ".nextval".toLowerCase());
                        it.setData_default(it.getData_default_vc());
                    }
                })
                .collect(Collectors.toList());
        Map<String, TabColumn> oldColumnMap = oldColumnList
                .stream()
                .collect(Collectors.toMap(it -> it.getColumn_name().toLowerCase(), Function.identity()));
        // 数据库主键信息
        TabColumn oldPrimaryKey = oldColumnList.stream()
                .filter(it -> "P".equals(it.getConstraint_type()))
                .findAny()
                .orElse(null);
        // 数据库索引信息
        Map<String, List<TabIndex>> oldIndexMap = TabIndex.search(tableName)
                .stream()
                .peek(it -> {
                    String columnExpression = it.getColumn_expression();
                    if (columnExpression != null) {
                        it.setColumn_name(columnExpression.substring(1, columnExpression.length() - 1));
                    }
                })
                .collect(Collectors.groupingBy(it -> it.getIndex_name().toLowerCase()));

        // 记录序列信息
        compareTableInfo.setNeedSequence(newPrimaryKey != null && newPrimaryKey.isAutoIncrement());
        TabSequence oldSequence = TabSequence.search(tableName);
        compareTableInfo.setHasSequence(oldSequence != null);

        // 判断表注释
        if (!newTableComment.equals(oldTableComment)) {
            compareTableInfo.setTableComment(newTableComment);
        }

        // 是否需要删除旧主键
        if (oldPrimaryKey != null) {
            if (newPrimaryKey == null || !newPrimaryKey.getName().equalsIgnoreCase(oldPrimaryKey.getColumn_name())) {
                compareTableInfo.setDeletePrimaryKey(oldPrimaryKey);
            }
        }
        // 是否需要新增主键
        if (newPrimaryKey != null) {
            if (oldPrimaryKey == null || !newPrimaryKey.getName().equalsIgnoreCase(oldPrimaryKey.getColumn_name())) {
                compareTableInfo.setCreatePrimaryKey(newPrimaryKey);
            }
        }
        // 新增字段
        List<ColumnMetadata> createColumnList = newColumnList.stream()
                .filter(it -> !oldColumnMap.containsKey(it.getName().toLowerCase()))
                .collect(Collectors.toList());
        compareTableInfo.setCreateColumnList(createColumnList);

        // 删除字段
        List<String> deleteColumnList = oldColumnList.stream()
                .map(TabColumn::getColumn_name)
                .map(String::toLowerCase)
                .filter(columnName -> !newColumnNameSet.contains(columnName))
                .collect(Collectors.toList());
        compareTableInfo.setDeleteColumnList(deleteColumnList);


        // 记录需要修改的字段
        Set<String> updateColumnSet = new HashSet<>();
        // 修改字段
        List<String> updateColumnList = newColumnList.stream()
                .filter(it -> oldColumnMap.containsKey(it.getName().toLowerCase()))
                .map(newColumn -> {
                    TabColumn oldColumn = oldColumnMap.get(newColumn.getName().toLowerCase());
                    String newColumnSql = newColumn.getName();
                    boolean change = false;

                    // 类型是否修改
                    String newType = newColumn.getType().getDefaultFullType();
                    String oldType = oldColumn.getFullType();
                    if (!newType.equalsIgnoreCase(oldType)) {
                        change = true;
                        updateColumnSet.add(newColumn.getName());
                        newColumnSql += " " + newType;
                    }


                    // 默认值是否修改
                    String newDefaultValue = OracleHelper.SQL.formatDefaultValue(tableName, newColumn);
                    String oldDefaultValue = String.valueOf(oldColumn.getData_default_vc()).trim();
                    if (!newDefaultValue.equalsIgnoreCase(oldDefaultValue)) {
                        change = true;
                        updateColumnSet.add(newColumn.getName());
                        newColumnSql += " DEFAULT " + newDefaultValue;
                    }


                    // 可空配置是否修改
                    boolean newNullAble = !newColumn.isNotNull();
                    boolean oldNullAble = "Y".equals(oldColumn.getNullable());
                    if (newNullAble != oldNullAble) {
                        change = true;
                        updateColumnSet.add(newColumn.getName());
                        if (newNullAble) {
                            newColumnSql += " NULL";
                        } else {
                            newColumnSql += " NOT NULL";
                        }
                    }
                    return change ? newColumnSql : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        compareTableInfo.setUpdateColumnList(updateColumnList);

        // 字段注释是否修改
        List<ColumnMetadata> updateColumnCommentList = newColumnList.stream()
                .filter(it -> oldColumnMap.containsKey(it.getName().toLowerCase()))
                .filter(newColumn -> {
                    TabColumn oldColumn = oldColumnMap.get(newColumn.getName().toLowerCase());
                    String newComment = Optional.ofNullable(newColumn.getComment()).orElse("");
                    String oldComment = Optional.ofNullable(oldColumn.getComments()).orElse("");
                    return !newComment.equals(oldComment);
                })
                .collect(Collectors.toList());
        compareTableInfo.setUpdateColumnCommentList(updateColumnCommentList);

        // 删除索引列表
        Set<String> deleteIndexList = new HashSet<>();
        // 新增索引列表
        List<IndexMetadata> createIndexList = new ArrayList<>();

        // 遍历实体索引
        for (IndexMetadata newIndex : newIndexList) {
            // 索引名称
            String indexName = newIndex.getName().toLowerCase();
            List<IndexMetadata.IndexColumnParam> newIndexColumns = newIndex.getColumns();
            List<String> newIndexColumnNames = newIndex.getColumns()
                    .stream()
                    .map(IndexMetadata.IndexColumnParam::getColumn)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
            // 索引字段有更新操作,需要删除关联的索引+重建索引
            if (updateColumnSet.stream().anyMatch(newIndexColumnNames::contains)) {
                createIndexList.add(newIndex);
                if (oldIndexMap.containsKey(indexName)) {
                    deleteIndexList.add(indexName);
                }
                continue;
            }
            // 新增索引
            if (!oldIndexMap.containsKey(indexName)) {
                createIndexList.add(newIndex);
                continue;
            }
            // 存在同名索引,判断是否需要修改索引
            List<TabIndex> oldIndexColumns = oldIndexMap.get(newIndex.getName().toLowerCase());
            // 数量不同,需要删除索引+重建索引
            if (newIndexColumns.size() != oldIndexColumns.size()) {
                createIndexList.add(newIndex);
                deleteIndexList.add(indexName);
                continue;
            }
            for (int i = 0; i < newIndexColumns.size(); i++) {
                IndexMetadata.IndexColumnParam newIndexColumn = newIndexColumns.get(i);
                TabIndex oldIndexColumn = oldIndexColumns.get(i);
                // 字段顺序不同,需要删除索引+重建索引
                if (!newIndexColumn.getColumn().equalsIgnoreCase(oldIndexColumn.getColumn_name())) {
                    createIndexList.add(newIndex);
                    deleteIndexList.add(indexName);
                    continue;
                }
                String newSort = Optional.ofNullable(newIndexColumn.getSort())
                        .orElse(IndexSortTypeEnum.ASC)
                        .name().toLowerCase();
                String oldSort = Optional.ofNullable(oldIndexColumn.getDescend())
                        .orElse("ASC")
                        .toLowerCase();
                // 字段排序方式不同,需要删除索引+重建索引
                if (!newSort.equals(oldSort)) {
                    createIndexList.add(newIndex);
                    deleteIndexList.add(indexName);
                }
            }
        }

        // 不在定义中的旧索引,删除索引
        oldIndexMap.keySet()
                .stream()
                .filter(oldIndexName -> !newIndexNameSet.contains(oldIndexName.toLowerCase()))
                .forEach(deleteIndexList::add);
        compareTableInfo.setDeleteIndexList(deleteIndexList);
        compareTableInfo.setCreateIndexList(createIndexList);
        return compareTableInfo;
    }


    @Override
    public List<String> modifyTable(OracleCompareTableInfo compareTableInfo) {
        List<String> result = new ArrayList<>();
        PropertyConfig properties = AutoTableGlobalConfig.instance().getAutoTableProperties();
        String tableName = compareTableInfo.getName();
        // 先删除需要删除的索引,方便后续修改字段
        if (properties.getAutoDropIndex()) {
            String indexPrefix = properties.getIndexPrefix().toLowerCase();
            Boolean dropCustomIndex = properties.getAutoDropCustomIndex();
            for (String indexName : compareTableInfo.getDeleteIndexList()) {
                boolean isAutoIndex = indexName.startsWith(indexPrefix);
                if (isAutoIndex || dropCustomIndex) {
                    result.add(String.format("DROP INDEX %s", indexName));
                }
            }
        }
        // 先新增序列,方便后续修改主键默认值
        if (compareTableInfo.isNeedSequence() && !compareTableInfo.isHasSequence()) {
            result.add(String.format("CREATE SEQUENCE seq_%s", tableName));
        }

        // 删除字段
        if (properties.getAutoDropColumn()) {
            for (String column : compareTableInfo.getDeleteColumnList()) {
                result.add(String.format("ALTER TABLE %s DROP COLUMN %s", tableName, column));
            }
        }

        // 新增字段
        for (ColumnMetadata columnMetadata : compareTableInfo.getCreateColumnList()) {
            String columnSql = OracleHelper.SQL.toColumnSql(tableName, columnMetadata);
            result.add(String.format("ALTER TABLE %s ADD (%s)", tableName, columnSql));
        }

        // 修改字段
        for (String columnSql : compareTableInfo.getUpdateColumnList()) {
            result.add(String.format("ALTER TABLE %s MODIFY (%s)", tableName, columnSql));
        }

        // 删除序列
        if (!compareTableInfo.isNeedSequence() && compareTableInfo.isHasSequence()) {
            result.add(String.format("DROP SEQUENCE seq_%s", tableName));
        }


        // 删除主键
        TabColumn deletePrimaryKey = compareTableInfo.getDeletePrimaryKey();
        if (deletePrimaryKey != null) {
            result.add(String.format("ALTER TABLE %s DROP CONSTRAINT %s", tableName, deletePrimaryKey.getConstraint_name()));
        }

        // 新增主键
        ColumnMetadata createPrimaryKey = compareTableInfo.getCreatePrimaryKey();
        if (createPrimaryKey != null) {
            result.add(String.format("ALTER TABLE %s ADD CONSTRAINT pk_%s PRIMARY KEY(%s)", tableName, tableName, createPrimaryKey.getName()));
        }


        // 新建/重建索引
        for (IndexMetadata indexMetadata : compareTableInfo.getCreateIndexList()) {
            String indexSql = OracleHelper.SQL.toIndexSql(tableName, indexMetadata);
            result.add(indexSql);
        }

        // 修改表注释
        if (compareTableInfo.getTableComment() != null) {
            result.add(String.format("COMMENT ON TABLE %s IS '%s'", tableName, compareTableInfo.getTableComment()));
        }

        // 修改字段注释
        for (ColumnMetadata columnMetadata : compareTableInfo.getUpdateColumnCommentList()) {
            result.add(String.format("COMMENT ON COLUMN %s.%s IS '%s'", tableName, columnMetadata.getName(), columnMetadata.getComment()));
        }
        return result;
    }
}
