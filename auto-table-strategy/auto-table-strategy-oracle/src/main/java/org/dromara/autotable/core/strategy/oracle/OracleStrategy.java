package org.dromara.autotable.core.strategy.oracle;

import lombok.NonNull;
import org.dromara.autotable.annotation.enums.IndexSortTypeEnum;
import org.dromara.autotable.annotation.oracle.OracleTypeConstant;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.builder.ColumnMetadataBuilder;
import org.dromara.autotable.core.builder.DefaultTableMetadataBuilder;
import org.dromara.autotable.core.builder.IndexMetadataBuilder;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DatabaseTypeDefine;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
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

    /**
     * oracle数据库sql包装
     *
     * @param rawSql 原始sql
     * @return 包装后的sql
     */
    @Override
    public String wrapSql(String rawSql) {
        return rawSql;
    }

    /**
     * 重写typeMapping方法以提供自定义的数据库类型映射
     * 此方法定义了Java类型到数据库类型的映射规则，用于指导ORM框架如何处理不同类型的字段
     *
     * @return 返回一个映射，键是Java类型，值是对应的数据库类型定义
     */
    @Override
    public Map<Class<?>, DefaultTypeEnumInterface> typeMapping() {
        // 定义字符串类型映射，使用Oracle的VARCHAR2类型，长度为255
        DatabaseTypeDefine strType = DatabaseTypeDefine.of(OracleTypeConstant.VARCHAR2, 255);
        // 定义布尔类型映射，使用Oracle的NUMBER类型，长度为1，精度为0
        DatabaseTypeDefine boolType = DatabaseTypeDefine.of(OracleTypeConstant.NUMBER, 1, 0);
        // 定义短整型类型映射，使用Oracle的NUMBER类型，长度为5，精度为0
        DatabaseTypeDefine shortType = DatabaseTypeDefine.of(OracleTypeConstant.NUMBER, 5, 0);
        // 定义整型类型映射，使用Oracle的NUMBER类型，长度为10，精度为0
        DatabaseTypeDefine intType = DatabaseTypeDefine.of(OracleTypeConstant.NUMBER, 10, 0);
        // 定义长整型类型映射，使用Oracle的NUMBER类型，长度为19，精度为0
        DatabaseTypeDefine longType = DatabaseTypeDefine.of(OracleTypeConstant.NUMBER, 19, 0);
        // 定义浮点类型映射，使用Oracle的BINARY_FLOAT类型
        DatabaseTypeDefine floatType = DatabaseTypeDefine.of(OracleTypeConstant.BINARY_FLOAT);
        // 定义双精度浮点类型映射，使用Oracle的BINARY_DOUBLE类型
        DatabaseTypeDefine doubleType = DatabaseTypeDefine.of(OracleTypeConstant.BINARY_DOUBLE);
        // 定义大十进制数类型映射，使用Oracle的NUMBER类型，长度为38，精度为18
        DatabaseTypeDefine bigDecimalType = DatabaseTypeDefine.of(OracleTypeConstant.NUMBER, 38, 18);
        // 定义时间戳类型映射，使用Oracle的TIMESTAMP类型，精度为6
        DatabaseTypeDefine timestampType = DatabaseTypeDefine.of(OracleTypeConstant.TIMESTAMP, 6);
        // 定义日期类型映射，使用Oracle的DATE类型
        DatabaseTypeDefine dateType = DatabaseTypeDefine.of(OracleTypeConstant.DATE);

        // 创建并初始化一个HashMap，存储Java类型到数据库类型定义的映射
        // 初始容量设为32，根据经验估计的映射数量来减少哈希表的扩容操作
        return new HashMap<Class<?>, DefaultTypeEnumInterface>(32) {{
            // 映射Java字符串和字符类型到数据库的字符串类型
            put(String.class, strType);
            put(Character.class, strType);
            put(char.class, strType);

            // 映射Java布尔类型到数据库的布尔类型
            put(Boolean.class, boolType);
            put(boolean.class, boolType);

            // 映射Java短整型到数据库的短整型
            put(Short.class, shortType);
            put(short.class, shortType);

            // 映射Java整型到数据库的整型
            put(Integer.class, intType);
            put(int.class, intType);

            // 映射Java大整数和长整型到数据库的长整型
            put(BigInteger.class, longType);
            put(Long.class, longType);
            put(long.class, longType);

            // 映射Java浮点型到数据库的浮点型
            put(Float.class, floatType);
            put(float.class, floatType);
            // 映射Java双精度浮点型到数据库的双精度浮点型
            put(Double.class, doubleType);
            put(double.class, doubleType);
            // 映射Java大十进制数到数据库的大十进制数
            put(BigDecimal.class, bigDecimalType);

            // 映射Java的各种日期和时间类型到数据库的日期和时间类型
            put(java.util.Date.class, timestampType);
            put(java.sql.Time.class, timestampType);
            put(java.sql.Date.class, dateType);
            put(java.sql.Timestamp.class, timestampType);
            put(java.time.LocalTime.class, timestampType);
            put(java.time.LocalDate.class, dateType);
            put(java.time.LocalDateTime.class, timestampType);
        }};
    }


    /**
     * 生成删除指定表及其对应序列的PL/SQL代码
     * 此方法根据提供的模式和表名，生成一段PL/SQL代码，用于检查并删除表，以及检查并删除与表名对应的序列
     * 主要目的是在数据库中安全地删除表及其相关序列，避免手动删除时可能出现的错误
     *
     * @param schema 模式名称，本方法中未使用该参数，但保留以符合可能的接口要求
     * @param tableName 表名称，用于确定要删除的表和序列
     * @return 返回一段PL/SQL代码，用于删除指定的表和序列
     */
    @Override
    public String dropTable(String schema, String tableName) {
        // 生成一段PL/SQL代码，用于检查并删除指定的表和序列
        // 首先声明两个变量，用于存储表和序列的数量
        return String.format("DECLARE\n" +
                "    table_count INTEGER;\n" +
                "    seq_count   INTEGER;\n" +
                "BEGIN\n" +
                // 检查用户表中是否存在指定名称的表
                "    SELECT COUNT(*) INTO table_count FROM user_tables WHERE upper(table_name) = upper('%s');\n" +
                "    IF table_count > 0 THEN\n" +
                // 如果表存在，则执行删除操作
                "        EXECUTE IMMEDIATE 'DROP TABLE %s';\n" +
                "    END IF;\n" +
                // 检查用户序列中是否存在以'seq_'为前缀的指定名称的序列
                "    SELECT COUNT(*) INTO seq_count FROM user_sequences WHERE upper(sequence_name) = upper('seq_%s');\n" +
                "    IF seq_count > 0 THEN\n" +
                // 如果序列存在，则执行删除操作
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
