package org.dromara.autotable.strategy.oracle;

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
import java.util.stream.Collectors;

/**
 * oracle数据库策略实现
 */
public class OracleStrategy implements IStrategy<DefaultTableMetadata, OracleCompareTableInfo> {

    private static final DefaultTableMetadataBuilder tableMetadataBuilder =
            new DefaultTableMetadataBuilder(new ColumnMetadataBuilder(DatabaseDialect.Oracle), new IndexMetadataBuilder());

    private static final Map<Class<?>, DefaultTypeEnumInterface> TYPE_MAPPING;

    static {
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
        // 定义字节类型映射，使用Oracle的NUMBER类型，长度为3，精度为0
        DatabaseTypeDefine byteType = DatabaseTypeDefine.of(OracleTypeConstant.NUMBER, 3, 0);
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

        // 创建并初始化类型映射
        Map<Class<?>, DefaultTypeEnumInterface> typeMap = new HashMap<>(32);
        // 映射Java字符串和字符类型到数据库的字符串类型
        typeMap.put(String.class, strType);
        typeMap.put(Character.class, strType);
        typeMap.put(char.class, strType);

        // 映射Java布尔类型到数据库的布尔类型
        typeMap.put(Boolean.class, boolType);
        typeMap.put(boolean.class, boolType);

        // 映射Java字节类型到数据库的字节类型
        typeMap.put(Byte.class, byteType);
        typeMap.put(byte.class, byteType);

        // 映射Java短整型到数据库的短整型
        typeMap.put(Short.class, shortType);
        typeMap.put(short.class, shortType);

        // 映射Java整型到数据库的整型
        typeMap.put(Integer.class, intType);
        typeMap.put(int.class, intType);

        // 映射Java大整数和长整型到数据库的长整型
        typeMap.put(BigInteger.class, longType);
        typeMap.put(Long.class, longType);
        typeMap.put(long.class, longType);

        // 映射Java浮点型到数据库的浮点型
        typeMap.put(Float.class, floatType);
        typeMap.put(float.class, floatType);
        // 映射Java双精度浮点型到数据库的双精度浮点型
        typeMap.put(Double.class, doubleType);
        typeMap.put(double.class, doubleType);
        // 映射Java大十进制数到数据库的大十进制数
        typeMap.put(BigDecimal.class, bigDecimalType);

        // 映射Java的各种日期和时间类型到数据库的日期和时间类型
        typeMap.put(java.util.Date.class, timestampType);
        typeMap.put(java.sql.Time.class, timestampType);
        typeMap.put(java.sql.Date.class, dateType);
        typeMap.put(java.sql.Timestamp.class, timestampType);
        typeMap.put(java.time.LocalTime.class, timestampType);
        typeMap.put(java.time.LocalDate.class, dateType);
        typeMap.put(java.time.LocalDateTime.class, timestampType);

        TYPE_MAPPING = Collections.unmodifiableMap(typeMap);
    }

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
     * 索引名称最大长度: 考虑到大多数数据库，其中oracle的30最小，再就是pg的63了，所以这里取63，oracle自行处理
     *
     * @return 索引名称最大长度
     */
    @Override
    public int indexNameMaxLength() {
        return 30;
    }

    /**
     * 重写typeMapping方法以提供自定义的数据库类型映射
     * 此方法定义了Java类型到数据库类型的映射规则，用于指导ORM框架如何处理不同类型的字段
     *
     * @return 返回一个映射，键是Java类型，值是对应的数据库类型定义
     */
    @Override
    public Map<Class<?>, DefaultTypeEnumInterface> typeMapping() {
        return TYPE_MAPPING;
    }

    @Override
    public boolean checkTableNotExist(String schema, String tableName) {
        return resolveActualTableName(schema, tableName) == null;
    }

    private String resolveActualTableName(String schema, String tableName) {
        return OracleIdentifierUtils.resolveExistingName(listAllTables(schema), tableName);
    }


    /**
     * 生成删除指定表及其对应序列的PL/SQL代码
     * 此方法根据提供的模式和表名，生成一段PL/SQL代码，用于检查并删除表，以及检查并删除与表名对应的序列
     * 主要目的是在数据库中安全地删除表及其相关序列，避免手动删除时可能出现的错误
     *
     * @param schema    模式名称，本方法中未使用该参数，但保留以符合可能的接口要求
     * @param tableName 表名称，用于确定要删除的表和序列
     * @return 返回一段PL/SQL代码，用于删除指定的表和序列
     */
    @Override
    public String dropTable(String schema, String tableName) {
        String wrappedTableName = IStrategy.wrapIdentifiers(tableName);
        String legacyTableName = tableName.toUpperCase(Locale.ROOT);
        String wrappedLegacyTableName = IStrategy.wrapIdentifiers(legacyTableName);
        String sequenceName = OracleIdentifierUtils.sequenceName(tableName);
        String legacySequenceName = sequenceName.toUpperCase(Locale.ROOT);
        String wrappedSeqName = IStrategy.wrapIdentifiers(sequenceName);
        String wrappedLegacySeqName = IStrategy.wrapIdentifiers(legacySequenceName);
        // 生成一段PL/SQL代码，用于检查并删除指定的表和序列
        // 首先声明两个变量，用于存储表和序列的数量
        return String.format("DECLARE " +
                        "    table_count INTEGER; " +
                        "    auto_seq_count   INTEGER; " +
                        "BEGIN " +
                        "    SELECT COUNT(*) INTO table_count FROM user_tables WHERE table_name = '%s'; " +
                        "    IF table_count > 0 THEN " +
                        "        EXECUTE IMMEDIATE 'DROP TABLE %s'; " +
                        "    ELSE " +
                        "        SELECT COUNT(*) INTO table_count FROM user_tables WHERE table_name = '%s'; " +
                        "        IF table_count > 0 THEN " +
                        "            EXECUTE IMMEDIATE 'DROP TABLE %s'; " +
                        "        END IF; " +
                        "    END IF; " +
                        "    SELECT COUNT(*) INTO auto_seq_count FROM user_sequences WHERE sequence_name = '%s'; " +
                        "    IF auto_seq_count > 0 THEN " +
                        "        EXECUTE IMMEDIATE 'DROP SEQUENCE %s'; " +
                        "    ELSE " +
                        "        SELECT COUNT(*) INTO auto_seq_count FROM user_sequences WHERE sequence_name = '%s'; " +
                        "        IF auto_seq_count > 0 THEN " +
                        "            EXECUTE IMMEDIATE 'DROP SEQUENCE %s'; " +
                        "        END IF; " +
                        "    END IF;" +
                        "END;", tableName, wrappedTableName, legacyTableName, wrappedLegacyTableName,
                sequenceName, wrappedSeqName, legacySequenceName, wrappedLegacySeqName)
                .replaceAll("\\s+", " ");
    }

    @Override
    public @NonNull DefaultTableMetadata analyseClass(Class<?> beanClass) {
        DefaultTableMetadata tableMetadata = tableMetadataBuilder.build(beanClass);
        // 主键字段
        ColumnMetadata primaryKey = tableMetadata.getColumnMetadataList()
                .stream()
                .filter(ColumnMetadata::isPrimary)
                .findFirst()
                .orElse(null);
        if (primaryKey != null && primaryKey.isPrimary() && primaryKey.isAutoIncrement()) {
            TabVersion search = TabVersion.search();
            if (search.getMainVersion() < 12) {
                primaryKey.setAutoIncrement(false);
                log.warn("当前Oracle版本【{}】Oracle12以下版本不支持主键自增默认值, 将忽略【{}】主键的【autoIncrement = true】属性"
                        , search.getVersion()
                        , beanClass.getName()
                );
            }
        }
        return tableMetadata;
    }

    @Override
    public List<String> createTable(DefaultTableMetadata tableMetadata) {
        List<String> result = new ArrayList<>();
        String tableName = tableMetadata.getTableName();
        String wrappedTableName = IStrategy.wrapIdentifiers(tableName);
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
            result.add(String.format("CREATE SEQUENCE %s",
                    IStrategy.wrapIdentifiers(OracleIdentifierUtils.sequenceName(tableName))));
        }
        // 建表语句
        List<String> columnSqlList = columnMetadataList.stream()
                .map(it -> OracleHelper.SQL.toColumnSql(tableName, it))
                .collect(Collectors.toList());
        result.add(String.format("CREATE TABLE %s (%s)", wrappedTableName, String.join(", ", columnSqlList)));

        // 构建主键约束
        if (primaryKey != null) {
            String constraintName = IStrategy.wrapIdentifiers("auto_pk_" + tableName);
            String wrappedColumnName = IStrategy.wrapIdentifiers(primaryKey.getName());
            result.add(String.format("ALTER TABLE %s ADD CONSTRAINT %s PRIMARY KEY(%s)", wrappedTableName, constraintName, wrappedColumnName));
        }

        // 表和字段注释
        String escapedTableComment = tableComment.replace("'", "''");
        result.add(String.format("COMMENT ON TABLE %s IS '%s'", wrappedTableName, escapedTableComment));
        for (ColumnMetadata columnMetadata : columnMetadataList) {
            String columnName = columnMetadata.getName();
            String wrappedColumnName = IStrategy.wrapIdentifiers(columnName);
            String columnComment = Optional.ofNullable(columnMetadata.getComment()).orElse("");
            String escapedColumnComment = columnComment.replace("'", "''");
            result.add(String.format("COMMENT ON COLUMN %s.%s IS '%s'", wrappedTableName, wrappedColumnName, escapedColumnComment));
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
        String actualTableName = Optional.ofNullable(resolveActualTableName(tableMetadata.getSchema(), tableName))
                .orElse(tableName);
        compareTableInfo.setActualTableName(actualTableName);
        String newTableComment = Optional.ofNullable(tableMetadata.getComment()).orElse("");

        // 实体主键
        ColumnMetadata newPrimaryKey = tableMetadata.getColumnMetadataList()
                .stream()
                .filter(ColumnMetadata::isPrimary)
                .findAny()
                .orElse(null);

        // 实体字段信息
        List<ColumnMetadata> newColumnList = tableMetadata.getColumnMetadataList();
        // 实体索引信息
        List<IndexMetadata> newIndexList = tableMetadata.getIndexMetadataList();
        // 数据库字段信息
        String oldTableComment = Optional.of(TabComment.search(actualTableName))
                .map(TabComment::getComments)
                .orElse("");
        List<TabColumn> oldColumnList = TabColumn.search(actualTableName);
        Map<ColumnMetadata, TabColumn> matchedColumns = new LinkedHashMap<>();
        Set<TabColumn> matchedOldColumns = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ColumnMetadata newColumn : newColumnList) {
            TabColumn oldColumn = oldColumnList.stream()
                    .filter(column -> !matchedOldColumns.contains(column))
                    .filter(column -> newColumn.getName().equals(column.getColumn_name()))
                    .findFirst()
                    .orElse(null);
            if (oldColumn != null) {
                matchedColumns.put(newColumn, oldColumn);
                matchedOldColumns.add(oldColumn);
            }
        }
        for (ColumnMetadata newColumn : newColumnList) {
            if (matchedColumns.containsKey(newColumn)) {
                continue;
            }
            String legacyColumnName = newColumn.getName().toUpperCase(Locale.ROOT);
            TabColumn oldColumn = oldColumnList.stream()
                    .filter(column -> !matchedOldColumns.contains(column))
                    .filter(column -> legacyColumnName.equals(column.getColumn_name()))
                    .findFirst()
                    .orElse(null);
            if (oldColumn != null) {
                matchedColumns.put(newColumn, oldColumn);
                matchedOldColumns.add(oldColumn);
            }
        }
        // 数据库主键信息
        TabColumn oldPrimaryKey = oldColumnList.stream()
                .filter(it -> "P".equals(it.getConstraint_type()))
                .findAny()
                .orElse(null);
        // 数据库索引信息
        Map<String, List<TabIndex>> oldIndexMap = TabIndex.search(actualTableName)
                .stream()
                .peek(it -> {
                    String columnExpression = it.getColumn_expression();
                    if (columnExpression != null) {
                        it.setColumn_name(columnExpression.substring(1, columnExpression.length() - 1));
                    }
                })
                .collect(Collectors.groupingBy(TabIndex::getIndex_name, LinkedHashMap::new, Collectors.toList()));

        // 记录序列信息
        compareTableInfo.setNeedSequence(newPrimaryKey != null && newPrimaryKey.isAutoIncrement());
        TabSequence oldSequence = TabSequence.search(tableName);
        compareTableInfo.setHasSequence(oldSequence != null);
        compareTableInfo.setActualSequenceName(oldSequence == null ? null : oldSequence.getSequence_name());

        // 判断表注释
        if (!newTableComment.equals(oldTableComment)) {
            compareTableInfo.setTableComment(newTableComment);
        }

        // 是否需要删除旧主键
        if (oldPrimaryKey != null) {
            TabColumn matchedPrimaryKey = matchedColumns.get(newPrimaryKey);
            if (matchedPrimaryKey == null || !matchedPrimaryKey.getColumn_name().equals(oldPrimaryKey.getColumn_name())) {
                compareTableInfo.setDeletePrimaryKey(oldPrimaryKey);
            }
        }
        // 是否需要新增主键
        if (newPrimaryKey != null) {
            TabColumn matchedPrimaryKey = matchedColumns.get(newPrimaryKey);
            if (oldPrimaryKey == null || matchedPrimaryKey == null
                    || !matchedPrimaryKey.getColumn_name().equals(oldPrimaryKey.getColumn_name())) {
                String primaryKeyName = matchedPrimaryKey == null
                        ? newPrimaryKey.getName()
                        : matchedPrimaryKey.getColumn_name();
                compareTableInfo.setCreatePrimaryKey(copyColumnWithName(newPrimaryKey, primaryKeyName));
            }
        }
        // 新增字段
        List<ColumnMetadata> createColumnList = newColumnList.stream()
                .filter(it -> !matchedColumns.containsKey(it))
                .collect(Collectors.toList());
        compareTableInfo.setCreateColumnList(createColumnList);

        // 删除字段
        PropertyConfig properties = AutoTableGlobalConfig.instance().getAutoTableProperties();
        String logicDropColumnPrefix = properties.getLogicDropColumnPrefix();
        List<String> deleteColumnList = oldColumnList.stream()
                .filter(column -> !matchedOldColumns.contains(column))
                .map(TabColumn::getColumn_name)
                .filter(columnName -> !(StringUtils.hasText(logicDropColumnPrefix) && columnName.startsWith(logicDropColumnPrefix)))
                .collect(Collectors.toList());
        compareTableInfo.setDeleteColumnList(deleteColumnList);

        // 重命名字段（逻辑删除）
        if (StringUtils.hasText(logicDropColumnPrefix)) {
            oldColumnList.stream()
                    .filter(column -> !matchedOldColumns.contains(column))
                    .map(TabColumn::getColumn_name)
                    .filter(columnName -> !columnName.startsWith(logicDropColumnPrefix))
                    .forEach(columnName -> compareTableInfo.getRenameColumnMap().put(columnName, logicDropColumnPrefix + columnName));
        }


        // 记录需要修改的字段
        Set<String> updateColumnSet = new HashSet<>();
        String effectiveSequenceName = oldSequence == null
                ? OracleIdentifierUtils.sequenceName(tableName)
                : oldSequence.getSequence_name();
        // 修改字段
        List<String> updateColumnList = newColumnList.stream()
                .filter(matchedColumns::containsKey)
                .map(newColumn -> {
                    TabColumn oldColumn = matchedColumns.get(newColumn);
                    String newColumnSql = IStrategy.wrapIdentifiers(oldColumn.getColumn_name());
                    boolean change = false;

                    // 类型是否修改
                    String newType = newColumn.getType().getDefaultFullType();
                    String oldType = oldColumn.getFullType();
                    if (!isSameType(newType, oldType)) {
                        change = true;
                        updateColumnSet.add(oldColumn.getColumn_name());
                        newColumnSql += " " + newType;
                    }


                    // 默认值是否修改
                    String newDefaultValue = OracleHelper.SQL.formatDefaultValue(tableName, newColumn, effectiveSequenceName);
                    String oldDefaultValue = String.valueOf(oldColumn.getData_default()).trim();
                    boolean sameDefaultValue = newColumn.isPrimary() && newColumn.isAutoIncrement()
                            ? OracleIdentifierUtils.isSequenceNextVal(oldDefaultValue, effectiveSequenceName)
                            : newDefaultValue.equalsIgnoreCase(oldDefaultValue);
                    if (!sameDefaultValue) {
                        change = true;
                        updateColumnSet.add(oldColumn.getColumn_name());
                        newColumnSql += " DEFAULT " + newDefaultValue;
                    }


                    // 可空配置是否修改
                    boolean newNullAble = !newColumn.isNotNull();
                    boolean oldNullAble = "Y".equals(oldColumn.getNullable());
                    if (newNullAble != oldNullAble) {
                        change = true;
                        updateColumnSet.add(oldColumn.getColumn_name());
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
                .filter(matchedColumns::containsKey)
                .filter(newColumn -> {
                    TabColumn oldColumn = matchedColumns.get(newColumn);
                    String newComment = Optional.ofNullable(newColumn.getComment()).orElse("");
                    String oldComment = Optional.ofNullable(oldColumn.getComments()).orElse("");
                    return !newComment.equals(oldComment);
                })
                .map(newColumn -> copyColumnWithName(newColumn, matchedColumns.get(newColumn).getColumn_name()))
                .collect(Collectors.toList());
        compareTableInfo.setUpdateColumnCommentList(updateColumnCommentList);

        // 删除索引列表
        Set<String> deleteIndexList = new HashSet<>();
        // 新增索引列表
        List<IndexMetadata> createIndexList = new ArrayList<>();
        Set<String> matchedOldIndexNames = new HashSet<>();
        Map<IndexMetadata, String> matchedIndexNames = new IdentityHashMap<>();
        for (IndexMetadata newIndex : newIndexList) {
            if (oldIndexMap.containsKey(newIndex.getName())) {
                matchedIndexNames.put(newIndex, newIndex.getName());
                matchedOldIndexNames.add(newIndex.getName());
            }
        }
        for (IndexMetadata newIndex : newIndexList) {
            if (matchedIndexNames.containsKey(newIndex)) {
                continue;
            }
            String legacyIndexName = newIndex.getName().toUpperCase(Locale.ROOT);
            if (oldIndexMap.containsKey(legacyIndexName) && !matchedOldIndexNames.contains(legacyIndexName)) {
                matchedIndexNames.put(newIndex, legacyIndexName);
                matchedOldIndexNames.add(legacyIndexName);
            }
        }

        // 遍历实体索引
        for (IndexMetadata newIndex : newIndexList) {
            String actualIndexName = matchedIndexNames.get(newIndex);
            IndexMetadata indexForCreate = copyIndexWithActualColumnNames(newIndex, matchedColumns);
            List<IndexMetadata.IndexColumnParam> newIndexColumns = newIndex.getColumns();
            // 索引字段有更新操作,需要删除关联的索引+重建索引
            boolean relatedColumnChanged = newIndexColumns.stream()
                    .map(IndexMetadata.IndexColumnParam::getColumn)
                    .map(columnName -> resolveActualColumnName(columnName, matchedColumns))
                    .anyMatch(updateColumnSet::contains);
            if (relatedColumnChanged) {
                createIndexList.add(indexForCreate);
                if (actualIndexName != null) {
                    deleteIndexList.add(actualIndexName);
                }
                continue;
            }
            // 新增索引
            if (actualIndexName == null) {
                createIndexList.add(indexForCreate);
                continue;
            }
            // 存在同名索引,判断是否需要修改索引
            List<TabIndex> oldIndexColumns = oldIndexMap.get(actualIndexName);
            // 数量不同,需要删除索引+重建索引
            if (newIndexColumns.size() != oldIndexColumns.size()) {
                createIndexList.add(indexForCreate);
                deleteIndexList.add(actualIndexName);
                continue;
            }
            boolean needRebuild = false;
            for (int i = 0; i < newIndexColumns.size(); i++) {
                IndexMetadata.IndexColumnParam newIndexColumn = newIndexColumns.get(i);
                TabIndex oldIndexColumn = oldIndexColumns.get(i);
                // 字段顺序不同,需要删除索引+重建索引
                String actualColumnName = resolveActualColumnName(newIndexColumn.getColumn(), matchedColumns);
                if (!actualColumnName.equals(oldIndexColumn.getColumn_name())) {
                    needRebuild = true;
                    break;
                }
                String newSort = Optional.ofNullable(newIndexColumn.getSort())
                        .orElse(IndexSortTypeEnum.ASC)
                        .name().toLowerCase();
                String oldSort = Optional.ofNullable(oldIndexColumn.getDescend())
                        .orElse("ASC")
                        .toLowerCase();
                // 字段排序方式不同,需要删除索引+重建索引
                if (!newSort.equals(oldSort)) {
                    needRebuild = true;
                    break;
                }
            }
            if (needRebuild) {
                createIndexList.add(indexForCreate);
                deleteIndexList.add(actualIndexName);
            }
        }

        // 不在定义中的旧索引,删除索引
        oldIndexMap.keySet()
                .stream()
                .filter(oldIndexName -> !matchedOldIndexNames.contains(oldIndexName))
                .forEach(deleteIndexList::add);
        compareTableInfo.setDeleteIndexList(deleteIndexList);
        compareTableInfo.setCreateIndexList(createIndexList);
        return compareTableInfo;
    }

    private static String resolveActualColumnName(String expectedColumnName,
                                                  Map<ColumnMetadata, TabColumn> matchedColumns) {
        ColumnMetadata columnMetadata = OracleIdentifierUtils.resolveExisting(
                matchedColumns.keySet(), expectedColumnName, ColumnMetadata::getName);
        if (columnMetadata == null) {
            return expectedColumnName;
        }
        return matchedColumns.get(columnMetadata).getColumn_name();
    }

    private static ColumnMetadata copyColumnWithName(ColumnMetadata source, String name) {
        return new ColumnMetadata()
                .setName(name)
                .setComment(source.getComment())
                .setType(source.getType())
                .setNotNull(source.isNotNull())
                .setPrimary(source.isPrimary())
                .setAutoIncrement(source.isAutoIncrement())
                .setDefaultValueType(source.getDefaultValueType())
                .setDefaultValue(source.getDefaultValue());
    }

    private static IndexMetadata copyIndexWithActualColumnNames(IndexMetadata source,
                                                                 Map<ColumnMetadata, TabColumn> matchedColumns) {
        List<IndexMetadata.IndexColumnParam> columns = source.getColumns().stream()
                .map(column -> IndexMetadata.IndexColumnParam.newInstance(
                        resolveActualColumnName(column.getColumn(), matchedColumns), column.getSort()))
                .collect(Collectors.toList());
        return new IndexMetadata()
                .setName(source.getName())
                .setColumns(columns)
                .setType(source.getType())
                .setMethod(source.getMethod())
                .setComment(source.getComment());
    }

    private boolean isSameType(String newType, String oldType) {
        if (newType.equalsIgnoreCase(oldType)) {
            return true;
        }
        String newTypeReplace = newType.replace(",0)", ")");
        String oldTypeReplace = oldType.replace(",0)", ")");
        return newTypeReplace.equalsIgnoreCase(oldTypeReplace);
    }


    @Override
    public List<String> modifyTable(OracleCompareTableInfo compareTableInfo) {
        List<String> result = new ArrayList<>();
        PropertyConfig properties = AutoTableGlobalConfig.instance().getAutoTableProperties();
        String tableName = compareTableInfo.getName();
        String actualTableName = Optional.ofNullable(compareTableInfo.getActualTableName()).orElse(tableName);
        String wrappedTableName = IStrategy.wrapIdentifiers(actualTableName);
        String effectiveSequenceName = Optional.ofNullable(compareTableInfo.getActualSequenceName())
                .orElse(OracleIdentifierUtils.sequenceName(tableName));
        // 先删除需要删除的索引,方便后续修改字段
        if (properties.getAutoDropIndex()) {
            String indexPrefix = properties.getIndexPrefix();
            Boolean dropCustomIndex = properties.getAutoDropCustomIndex();
            for (String indexName : compareTableInfo.getDeleteIndexList()) {
                boolean isAutoIndex = indexName.regionMatches(true, 0, indexPrefix, 0, indexPrefix.length());
                if (isAutoIndex || dropCustomIndex) {
                    result.add(String.format("DROP INDEX %s", IStrategy.wrapIdentifiers(indexName)));
                }
            }
        }
        // 先新增序列,方便后续修改主键默认值
        if (compareTableInfo.isNeedSequence() && !compareTableInfo.isHasSequence()) {
            result.add(String.format("CREATE SEQUENCE %s", IStrategy.wrapIdentifiers(effectiveSequenceName)));
        }

        // 删除字段
        if (properties.getAutoDropColumn()) {
            for (String column : compareTableInfo.getDeleteColumnList()) {
                result.add(String.format("ALTER TABLE %s DROP COLUMN %s", wrappedTableName, IStrategy.wrapIdentifiers(column)));
            }
        }

        // 重命名字段（逻辑删除）
        for (Map.Entry<String, String> entry : compareTableInfo.getRenameColumnMap().entrySet()) {
            result.add(String.format("ALTER TABLE %s RENAME COLUMN %s TO %s", wrappedTableName, IStrategy.wrapIdentifiers(entry.getKey()), IStrategy.wrapIdentifiers(entry.getValue())));
        }

        // 新增字段
        for (ColumnMetadata columnMetadata : compareTableInfo.getCreateColumnList()) {
            String columnSql = OracleHelper.SQL.toColumnSql(tableName, columnMetadata, effectiveSequenceName);
            result.add(String.format("ALTER TABLE %s ADD (%s)", wrappedTableName, columnSql));
        }

        // 修改字段
        for (String columnSql : compareTableInfo.getUpdateColumnList()) {
            result.add(String.format("ALTER TABLE %s MODIFY (%s)", wrappedTableName, columnSql));
        }

        // 删除序列
        if (!compareTableInfo.isNeedSequence() && compareTableInfo.isHasSequence()) {
            result.add(String.format("DROP SEQUENCE %s", IStrategy.wrapIdentifiers(effectiveSequenceName)));
        }


        // 删除主键
        TabColumn deletePrimaryKey = compareTableInfo.getDeletePrimaryKey();
        if (deletePrimaryKey != null) {
            result.add(String.format("ALTER TABLE %s DROP CONSTRAINT %s", wrappedTableName, IStrategy.wrapIdentifiers(deletePrimaryKey.getConstraint_name())));
        }

        // 新增主键
        ColumnMetadata createPrimaryKey = compareTableInfo.getCreatePrimaryKey();
        if (createPrimaryKey != null) {
            String constraintName = IStrategy.wrapIdentifiers("auto_pk_" + tableName);
            String wrappedColumnName = IStrategy.wrapIdentifiers(createPrimaryKey.getName());
            result.add(String.format("ALTER TABLE %s ADD CONSTRAINT %s PRIMARY KEY(%s)", wrappedTableName, constraintName, wrappedColumnName));
        }


        // 新建/重建索引
        for (IndexMetadata indexMetadata : compareTableInfo.getCreateIndexList()) {
            String indexSql = OracleHelper.SQL.toIndexSql(actualTableName, indexMetadata);
            result.add(indexSql);
        }

        // 修改表注释
        if (compareTableInfo.getTableComment() != null) {
            String escapedTableComment = compareTableInfo.getTableComment().replace("'", "''");
            result.add(String.format("COMMENT ON TABLE %s IS '%s'", wrappedTableName, escapedTableComment));
        }

        // 修改字段注释
        for (ColumnMetadata columnMetadata : compareTableInfo.getUpdateColumnCommentList()) {
            String wrappedColumnName = IStrategy.wrapIdentifiers(columnMetadata.getName());
            String escapedColumnComment = Optional.ofNullable(columnMetadata.getComment()).orElse("").replace("'", "''");
            result.add(String.format("COMMENT ON COLUMN %s.%s IS '%s'", wrappedTableName, wrappedColumnName, escapedColumnComment));
        }
        return result;
    }
}
