package org.dromara.autotable.strategy.sqlserver;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.Utils;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.strategy.sqlserver.builder.CreateTableSqlBuilder;
import org.dromara.autotable.strategy.sqlserver.builder.ModifyTableSqlBuilder;
import org.dromara.autotable.strategy.sqlserver.builder.SqlServerTableMetadataBuilder;
import org.dromara.autotable.strategy.sqlserver.data.SqlServerCompareTableInfo;
import org.dromara.autotable.strategy.sqlserver.data.SqlServerDefaultTypeEnum;
import org.dromara.autotable.strategy.sqlserver.data.dbdata.SqlServerDbColumn;
import org.dromara.autotable.strategy.sqlserver.data.dbdata.SqlServerDbIndex;
import org.dromara.autotable.strategy.sqlserver.data.dbdata.SqlServerDbPrimary;
import org.dromara.autotable.strategy.sqlserver.mapper.SqlServerTablesMapper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SQLServer 数据库策略。
 *
 * <p>标识符使用方括号 {@code [name]} 包裹；自增列使用 {@code IDENTITY(1,1)}；
 * 注释通过 {@code sp_addextendedproperty} 写入。</p>
 *
 * @author don
 */
@Slf4j
public class SqlServerStrategy implements IStrategy<DefaultTableMetadata, SqlServerCompareTableInfo> {

    private final SqlServerTablesMapper mapper = new SqlServerTablesMapper();

    @Override
    public String databaseDialect() {
        return DatabaseDialect.SQLServer;
    }

    /**
     * SQLServer 标识符使用方括号包裹（前缀 [ 后缀 ]，与默认的单字符 identifier 不同，故重写 wrapIdentifier）。
     * <p>名称内含 {@code ]} 时转义为 {@code ]]}，与 {@code SqlServerDatabaseBuilder} 建库的转义规则保持一致。</p>
     */
    @Override
    public String wrapIdentifier(String name) {
        if (name == null) {
            return null;
        }
        if (name.startsWith("[") && name.endsWith("]")) {
            return name;
        }
        return "[" + name.replace("]", "]]") + "]";
    }

    @Override
    public int indexNameMaxLength() {
        // SQLServer 标识符最大 128
        return 128;
    }

    private static final Map<Class<?>, DefaultTypeEnumInterface> TYPE_MAPPING = new HashMap<>(32);

    static {
        TYPE_MAPPING.put(String.class, SqlServerDefaultTypeEnum.VARCHAR);
        TYPE_MAPPING.put(Character.class, SqlServerDefaultTypeEnum.CHAR);
        TYPE_MAPPING.put(char.class, SqlServerDefaultTypeEnum.CHAR);

        TYPE_MAPPING.put(Short.class, SqlServerDefaultTypeEnum.SMALLINT);
        TYPE_MAPPING.put(short.class, SqlServerDefaultTypeEnum.SMALLINT);
        TYPE_MAPPING.put(Byte.class, SqlServerDefaultTypeEnum.TINYINT);
        TYPE_MAPPING.put(byte.class, SqlServerDefaultTypeEnum.TINYINT);
        TYPE_MAPPING.put(int.class, SqlServerDefaultTypeEnum.INT);
        TYPE_MAPPING.put(Integer.class, SqlServerDefaultTypeEnum.INT);
        TYPE_MAPPING.put(Long.class, SqlServerDefaultTypeEnum.BIGINT);
        TYPE_MAPPING.put(long.class, SqlServerDefaultTypeEnum.BIGINT);
        TYPE_MAPPING.put(BigInteger.class, SqlServerDefaultTypeEnum.BIGINT);

        TYPE_MAPPING.put(Boolean.class, SqlServerDefaultTypeEnum.BIT);
        TYPE_MAPPING.put(boolean.class, SqlServerDefaultTypeEnum.BIT);

        TYPE_MAPPING.put(Float.class, SqlServerDefaultTypeEnum.REAL);
        TYPE_MAPPING.put(float.class, SqlServerDefaultTypeEnum.REAL);
        TYPE_MAPPING.put(Double.class, SqlServerDefaultTypeEnum.FLOAT);
        TYPE_MAPPING.put(double.class, SqlServerDefaultTypeEnum.FLOAT);
        TYPE_MAPPING.put(BigDecimal.class, SqlServerDefaultTypeEnum.DECIMAL);

        TYPE_MAPPING.put(Date.class, SqlServerDefaultTypeEnum.DATETIME2);
        TYPE_MAPPING.put(java.sql.Date.class, SqlServerDefaultTypeEnum.DATE);
        TYPE_MAPPING.put(java.sql.Timestamp.class, SqlServerDefaultTypeEnum.DATETIME2);
        TYPE_MAPPING.put(LocalDateTime.class, SqlServerDefaultTypeEnum.DATETIME2);
        TYPE_MAPPING.put(LocalDate.class, SqlServerDefaultTypeEnum.DATE);
        TYPE_MAPPING.put(LocalTime.class, SqlServerDefaultTypeEnum.TIME);
        TYPE_MAPPING.put(java.sql.Time.class, SqlServerDefaultTypeEnum.TIME);
        TYPE_MAPPING.put(OffsetTime.class, SqlServerDefaultTypeEnum.TIME);
    }

    @Override
    public Map<Class<?>, DefaultTypeEnumInterface> typeMapping() {
        return Collections.unmodifiableMap(TYPE_MAPPING);
    }

    @Override
    public DefaultTableMetadata analyseClass(Class<?> beanClass) {
        return new SqlServerTableMetadataBuilder().build(beanClass);
    }

    @Override
    public String dropTable(String schema, String tableName) {
        // SQLServer 2016+ 支持 IF EXISTS
        return "DROP TABLE IF EXISTS " + IStrategy.concatWrapIdentifiers(schema, tableName);
    }

    @Override
    public void createSchema(String schema) {
        if (!StringUtils.hasText(schema)) {
            return;
        }
        DataSourceManager.useConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(1) FROM sys.schemas WHERE name = ?")) {
                ps.setString(1, schema);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        try (Statement stmt = connection.createStatement()) {
                            stmt.executeUpdate("CREATE SCHEMA " + wrapIdentifier(schema));
                            log.info("成功创建 SQLServer schema [{}]", schema);
                        }
                    }
                }
            } catch (SQLException e) {
                log.error("检查或创建 schema [{}] 时出错", schema, e);
            }
        });
    }

    @Override
    public List<String> createTable(DefaultTableMetadata tableMetadata) {
        return CreateTableSqlBuilder.buildSql(tableMetadata);
    }

    @Override
    public SqlServerCompareTableInfo compareTable(DefaultTableMetadata tableMetadata) {

        SqlServerCompareTableInfo compareTableInfo = new SqlServerCompareTableInfo(tableMetadata.getTableName(), tableMetadata.getSchema());

        // 表注释差异
        compareTableInfo(tableMetadata, compareTableInfo);
        // 列差异
        compareColumnInfo(tableMetadata, compareTableInfo);
        // 索引差异
        compareIndexInfo(tableMetadata, compareTableInfo);

        return compareTableInfo;
    }

    @Override
    public List<String> modifyTable(SqlServerCompareTableInfo compareTableInfo) {
        return ModifyTableSqlBuilder.buildSql(compareTableInfo);
    }

    // ==================== 比较逻辑 ====================

    private void compareTableInfo(DefaultTableMetadata tableMetadata, SqlServerCompareTableInfo compareTableInfo) {

        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();

        String tableDescription = mapper.selectTableDescription(schema, tableName);
        if (StringUtils.hasText(tableMetadata.getComment()) && !tableMetadata.getComment().equals(tableDescription)) {
            compareTableInfo.setComment(tableMetadata.getComment());
            // 记录 DB 当前是否存在表注释，供后续生成 sp_add/sp_updateextendedproperty 复用，避免重复查询
            compareTableInfo.setTableCommentExists(StringUtils.hasText(tableDescription));
        }
    }

    private void compareColumnInfo(DefaultTableMetadata tableMetadata, SqlServerCompareTableInfo compareTableInfo) {

        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();
        // 数据库字段元信息
        List<SqlServerDbColumn> dbColumns = mapper.selectTableFieldDetail(schema, tableName);
        Map<String, SqlServerDbColumn> dbFieldDetailMap = dbColumns.stream()
                .collect(Collectors.toMap(SqlServerDbColumn::getColumnName, Function.identity()));
        // 当前字段信息
        List<ColumnMetadata> columnMetadataList = tableMetadata.getColumnMetadataList();

        PropertyConfig properties = AutoTableGlobalConfig.instance().getAutoTableProperties();
        String logicDropColumnPrefix = properties.getLogicDropColumnPrefix();

        for (ColumnMetadata columnMetadata : columnMetadataList) {
            String columnName = columnMetadata.getName();
            SqlServerDbColumn dbColumn = dbFieldDetailMap.remove(columnName);
            // 新增字段
            String fieldComment = columnMetadata.getComment();
            if (dbColumn == null) {
                // 标记注释（新增列，DB 必然无注释）
                if (StringUtils.hasText(fieldComment)) {
                    compareTableInfo.addColumnComment(columnMetadata.getName(), fieldComment, false);
                }
                // 标记字段信息
                compareTableInfo.addNewColumn(columnMetadata);
                continue;
            }
            /* 修改的字段 */
            // 自增属性不一致：SQLServer 不支持通过 ALTER COLUMN 将普通列改为/去掉 IDENTITY，仅 warn 提示，需手动处理
            boolean dbIdentity = "YES".equals(dbColumn.getIsIdentity());
            if (columnMetadata.isAutoIncrement() != dbIdentity) {
                log.warn("列[{}]自增属性不一致（实体={}, 数据库={}），SQLServer 不支持 ALTER 修改 IDENTITY，请手动调整",
                        columnName, columnMetadata.isAutoIncrement(), dbIdentity);
            }
            // 修改了字段注释
            String dbColumnComment = dbColumn.getDescription();
            if ((StringUtils.hasText(dbColumnComment) || StringUtils.hasText(fieldComment)) && !Objects.equals(dbColumnComment, fieldComment)) {
                compareTableInfo.addColumnComment(columnName, fieldComment, StringUtils.hasText(dbColumnComment));
            }
            // 主键忽略判断，单独处理
            if (!columnMetadata.isPrimary()) {
                // 字段类型不同
                boolean isTypeDiff = isTypeDiff(columnMetadata, dbColumn);
                // 非null不同
                boolean isNotnullDiff = columnMetadata.isNotNull() != Objects.equals(dbColumn.getIsNullable(), "NO");
                // 默认值不同
                boolean isDefaultDiff = isDefaultDiff(columnMetadata, dbColumn);
                if (isTypeDiff || isNotnullDiff || isDefaultDiff) {
                    compareTableInfo.addModifyColumn(columnMetadata, isTypeDiff, isNotnullDiff, isDefaultDiff, dbColumn.getDefaultConstraintName());
                }
            }
        }
        // 需要删除的字段
        Set<String> needRemoveColumns = dbFieldDetailMap.keySet();
        // 过滤掉已逻辑删除的字段
        if (StringUtils.hasText(logicDropColumnPrefix)) {
            needRemoveColumns = needRemoveColumns.stream()
                    .filter(col -> !col.startsWith(logicDropColumnPrefix))
                    .collect(Collectors.toSet());
        }
        if (!needRemoveColumns.isEmpty()) {
            // 根据配置，决定是否删除库上的多余字段
            if (properties.getAutoDropColumn()) {
                compareTableInfo.addDropColumns(needRemoveColumns);
            } else if (StringUtils.hasText(logicDropColumnPrefix)) {
                compareTableInfo.addRenameColumns(needRemoveColumns, logicDropColumnPrefix);
            }
        }

        /* 处理主键 */
        List<ColumnMetadata> primaryColumnList = columnMetadataList.stream().filter(ColumnMetadata::isPrimary).collect(Collectors.toList());
        Set<String> newPrimaryColumns = primaryColumnList.stream().map(ColumnMetadata::getName).collect(Collectors.toSet());
        // 查询数据库主键信息
        SqlServerDbPrimary dbPrimary = mapper.selectPrimaryKeyName(schema, tableName);
        HashSet<String> dbPrimaryColumns = dbPrimary == null || !StringUtils.hasText(dbPrimary.getColumns())
                ? new HashSet<>()
                : new HashSet<>(Arrays.asList(dbPrimary.getColumns().split(",")));

        boolean primaryChange = !dbPrimaryColumns.equals(newPrimaryColumns);
        if (primaryChange) {
            // 如果数据库存在主键，标记待删除的主键约束名
            if (dbPrimary != null && StringUtils.hasText(dbPrimary.getPrimaryName())) {
                compareTableInfo.setDropPrimaryKeyName(dbPrimary.getPrimaryName());
            }
        }
        boolean newPrimary = !primaryColumnList.isEmpty() && dbPrimary == null;
        if (newPrimary || primaryChange) {
            // 标记新创建的主键
            compareTableInfo.addNewPrimary(primaryColumnList);
        }
    }

    private boolean isTypeDiff(ColumnMetadata columnMetadata, SqlServerDbColumn dbColumn) {
        String fullType = columnMetadata.getType().getDefaultFullType().toLowerCase();
        String dataTypeFormat = dbColumn.getDataTypeFormat();
        if (dataTypeFormat == null) {
            return true;
        }
        // 提取类型名（去掉括号内的长度/精度部分）
        int fParen = fullType.indexOf('(');
        int dParen = dataTypeFormat.indexOf('(');
        String fBase = fParen < 0 ? fullType : fullType.substring(0, fParen);
        String dBase = dParen < 0 ? dataTypeFormat : dataTypeFormat.substring(0, dParen);
        // 类型名不同 → 必然变更
        if (!fBase.equals(dBase)) {
            return true;
        }
        // 类型名相同：
        //  - 实体未指定长度/精度（无括号）→ 视为"使用数据库默认"，忽略 DB 侧精度差异，
        //    避免实体 datetime2 与 DB datetime2(7) 反复误判触发无意义 ALTER
        //  - 实体指定了长度/精度 → 严格比较括号内参数
        if (fParen < 0) {
            return false;
        }
        if (dParen < 0) {
            // 实体有长度/精度但 DB 无（如实体 nvarchar(255) vs DB nvarchar(MAX 裸类型）→ 变更
            return true;
        }
        String fArgs = fullType.substring(fParen + 1, fullType.length() - 1);
        String dArgs = dataTypeFormat.substring(dParen + 1, dataTypeFormat.length() - 1);
        // decimal/numeric：实体只给 precision 不给 scale（无逗号）时，只比较 precision，
        // 避免实体 decimal(10) 与 DB decimal(10,0) 误判
        if (fArgs.indexOf(',') < 0 && dArgs.indexOf(',') >= 0) {
            return !fArgs.equals(dArgs.substring(0, dArgs.indexOf(',')));
        }
        return !fArgs.equals(dArgs);
    }

    private boolean isDefaultDiff(ColumnMetadata columnMetadata, SqlServerDbColumn dbColumn) {

        // SQLServer 默认值表达式带外层括号，去除后比较
        String columnDefault = dbColumn.getColumnDefaultWithoutParen();

        DefaultValueEnum defaultValueType = columnMetadata.getDefaultValueType();

        if (DefaultValueEnum.isValid(defaultValueType)) {
            if (defaultValueType == DefaultValueEnum.EMPTY_STRING) {
                return !"''".equals(columnDefault);
            }
            if (defaultValueType == DefaultValueEnum.NULL) {
                return columnDefault != null && !"NULL".equalsIgnoreCase(columnDefault);
            }
        } else {
            String defaultValue = columnMetadata.getDefaultValue();
            return !Objects.equals(defaultValue, columnDefault);
        }
        return false;
    }

    private void compareIndexInfo(DefaultTableMetadata tableMetadata, SqlServerCompareTableInfo compareTableInfo) {

        String schema = tableMetadata.getSchema();
        String tableName = tableMetadata.getTableName();

        List<SqlServerDbIndex> dbIndexes = mapper.selectTableIndexesDetail(schema, tableName);
        Map<String, SqlServerDbIndex> dbIndexMap = dbIndexes.stream()
                .collect(Collectors.toMap(SqlServerDbIndex::getIndexName, Function.identity()));

        List<IndexMetadata> indexMetadataList = tableMetadata.getIndexMetadataList();
        for (IndexMetadata indexMetadata : indexMetadataList) {
            String indexName = indexMetadata.getName();
            String comment = indexMetadata.getComment();
            // 尝试从索引标记集合中删除索引
            SqlServerDbIndex dbIndex = dbIndexMap.remove(indexName);
            // 删除失败，表示是新增的索引
            boolean isNewIndex = dbIndex == null;
            if (isNewIndex) {
                // 标记注释（新索引，DB 必然无注释）
                if (StringUtils.hasText(comment)) {
                    compareTableInfo.addIndexComment(indexMetadata.getName(), comment, false);
                }
                // 标记索引信息
                compareTableInfo.addNewIndex(indexMetadata);
                continue;
            }
            // 修改索引注释
            boolean anyOneIsValid = StringUtils.hasText(dbIndex.getDescription()) || StringUtils.hasText(comment);
            if (anyOneIsValid && !Objects.equals(dbIndex.getDescription(), comment)) {
                compareTableInfo.addIndexComment(indexName, comment, StringUtils.hasText(dbIndex.getDescription()));
            }

            // 索引定义比较：唯一性 + 列集合
            if (!isIndexSame(dbIndex, indexMetadata)) {
                compareTableInfo.addModifyIndex(indexMetadata);
            }
        }

        // 需要删除的索引
        Set<String> needDropIndexes = dbIndexMap.keySet();
        if (!needDropIndexes.isEmpty()) {
            PropertyConfig autoTableProperties = AutoTableGlobalConfig.instance().getAutoTableProperties();
            // 删除autotable创建的索引
            if (autoTableProperties.getAutoDropIndex()) {
                List<String> autoTableCreateIndexes = needDropIndexes.stream()
                        .filter(indexName -> indexName.startsWith(autoTableProperties.getIndexPrefix()))
                        .collect(Collectors.toList());
                compareTableInfo.getDropIndexList().addAll(autoTableCreateIndexes);
            }
            // 删除手动创建的索引
            if (autoTableProperties.getAutoDropCustomIndex()) {
                List<String> customCreateIndexes = needDropIndexes.stream()
                        .filter(indexName -> !indexName.startsWith(autoTableProperties.getIndexPrefix()))
                        .collect(Collectors.toList());
                compareTableInfo.getDropIndexList().addAll(customCreateIndexes);
            }
        }
    }

    /**
     * 比较数据库索引与实体索引定义是否一致（唯一性 + 列集合）。
     */
    private boolean isIndexSame(SqlServerDbIndex dbIndex, IndexMetadata indexMetadata) {
        // 唯一性
        boolean dbUnique = "YES".equals(dbIndex.getIsUnique());
        boolean entityUnique = IndexTypeEnum.UNIQUE == indexMetadata.getType();
        if (dbUnique != entityUnique) {
            return false;
        }
        // 列集合（仅比较列名集合，忽略排序差异以简化；如需严格排序比较可扩展）
        Set<String> dbColumns = StringUtils.hasText(dbIndex.getIndexColumns())
                ? new HashSet<>(Arrays.asList(dbIndex.getIndexColumns().split(",")))
                : new HashSet<>();
        Set<String> entityColumns = indexMetadata.getColumns().stream()
                .map(IndexMetadata.IndexColumnParam::getColumn)
                .collect(Collectors.toSet());
        return dbColumns.equals(entityColumns);
    }

    @Override
    public List<String> listAllTables(String schema) {
        return DataSourceManager.useConnection(connection -> {
            try {
                return Utils.getTables(connection, schema, new String[]{"TABLE"});
            } catch (SQLException e) {
                throw new RuntimeException("查询所有表出错", e);
            }
        });
    }
}
