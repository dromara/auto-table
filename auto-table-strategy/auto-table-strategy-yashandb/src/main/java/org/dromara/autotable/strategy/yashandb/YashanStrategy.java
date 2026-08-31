package org.dromara.autotable.strategy.yashandb;

import lombok.NonNull;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.config.PropertyConfig;
import org.dromara.autotable.core.constants.DatabaseDialect;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.DefaultTableMetadata;
import org.dromara.autotable.core.strategy.IStrategy;
import org.dromara.autotable.core.strategy.IndexMetadata;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.strategy.yashandb.builder.YashanCreateTableSqlBuilder;
import org.dromara.autotable.strategy.yashandb.builder.YashanModifyTableSqlBuilder;
import org.dromara.autotable.strategy.yashandb.builder.YashanTableMetadataBuilder;
import org.dromara.autotable.strategy.yashandb.data.YashanCompareTableInfo;
import org.dromara.autotable.strategy.yashandb.data.YashanDefaultTypeEnum;
import org.dromara.autotable.strategy.yashandb.data.YashanTypeHelper;
import org.dromara.autotable.strategy.yashandb.data.dbdata.YashanDbColumn;
import org.dromara.autotable.strategy.yashandb.data.dbdata.YashanDbIndex;
import org.dromara.autotable.strategy.yashandb.data.dbdata.YashanDbPrimary;
import org.dromara.autotable.strategy.yashandb.mapper.YashanTablesMapper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * AutoTable strategy for YashanDB in MySQL-compatible mode.
 *
 * <p>The strategy reads YashanDB metadata through its native catalog views,
 * while normalizing MySQL-originated types and identifiers for migrated OA
 * schemas. Other database strategies are not changed by this module.</p>
 */
public class YashanStrategy implements IStrategy<DefaultTableMetadata, YashanCompareTableInfo> {
    private static final Map<Class<?>, DefaultTypeEnumInterface> TYPE_MAPPING = createTypeMapping();
    private final YashanTablesMapper mapper = new YashanTablesMapper();

    /**
     * Returns the JDBC product dialect key used by AutoTable's strategy loader.
     */
    @Override
    public String databaseDialect() {
        return DatabaseDialect.YashanDB;
    }

    /**
     * YashanDB folds unquoted identifiers and resolves them case-insensitively. Returning an empty
     * identifier preserves that behavior for lower-case entity and column names.
     */
    @Override
    public String identifier() {
        return "";
    }

    /**
     * Removes trailing semicolons before AutoTable executes generated SQL.
     */
    @Override
    public String wrapSql(String rawSql) {
        String sql = rawSql.trim();
        // AutoTable may pass statements with one or more terminators from migration scripts.
        while (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        return sql;
    }

    /**
     * Returns Java-to-YashanDB type mappings, including MySQL-compatible aliases.
     */
    @Override
    public Map<Class<?>, DefaultTypeEnumInterface> typeMapping() {
        return TYPE_MAPPING;
    }

    /**
     * Builds a YashanDB drop-table statement and removes dependent constraints.
     */
    @Override
    public String dropTable(String schema, String tableName) {
        return "DROP TABLE IF EXISTS " + concatWrapName(schema, tableName) + " CASCADE CONSTRAINTS";
    }

    /**
     * Converts an annotated Java model into AutoTable metadata using YashanDB rules.
     */
    @Override
    public @NonNull DefaultTableMetadata analyseClass(Class<?> beanClass) {
        return new YashanTableMetadataBuilder().build(beanClass);
    }

    /**
     * Generates YashanDB CREATE TABLE, index, and comment statements.
     */
    @Override
    public List<String> createTable(DefaultTableMetadata tableMetadata) {
        return YashanCreateTableSqlBuilder.buildSql(tableMetadata);
    }

    /**
     * Checks table existence through YashanDB catalog views using case-insensitive names.
     */
    @Override
    public boolean checkTableNotExist(String schema, String tableName) {
        return !mapper.tableExists(schema, tableName);
    }

    /**
     * Lists tables visible in the requested YashanDB schema.
     */
    @Override
    public List<String> listAllTables(String schema) {
        return mapper.selectTables(schema);
    }

    /**
     * Compares model metadata with live YashanDB metadata and records safe changes.
     */
    @Override
    public @NonNull YashanCompareTableInfo compareTable(DefaultTableMetadata table) {
        YashanCompareTableInfo changes = new YashanCompareTableInfo(table.getTableName(), table.getSchema());
        // Compare each metadata category independently so generated ALTER statements stay ordered.
        compareTableComment(table, changes);
        compareColumns(table, changes);
        comparePrimaryKey(table, changes);
        compareIndexes(table, changes);
        return changes;
    }

    /**
     * Converts the recorded differences into executable YashanDB ALTER statements.
     */
    @Override
    public List<String> modifyTable(YashanCompareTableInfo compareTableInfo) {
        return YashanModifyTableSqlBuilder.buildSql(compareTableInfo);
    }

    /**
     * Records a table-comment change without issuing SQL during comparison.
     */
    private void compareTableComment(DefaultTableMetadata table, YashanCompareTableInfo changes) {
        String expected = emptyToNull(table.getComment());
        String actual = emptyToNull(mapper.selectTableComment(table.getSchema(), table.getTableName()));
        if (!Objects.equals(expected, actual)) {
            changes.setComment(table.getComment());
            changes.setTableCommentChanged(true);
        }
    }

    /**
     * Compares columns case-insensitively and honors AutoTable's drop/rename settings.
     */
    private void compareColumns(DefaultTableMetadata table, YashanCompareTableInfo changes) {
        Map<String, YashanDbColumn> existing = caseInsensitiveMap();
        // YashanDB folds unquoted names; a case-insensitive map avoids false additions/deletions.
        mapper.selectColumns(table.getSchema(), table.getTableName())
                .forEach(column -> existing.put(column.getName(), column));

        for (ColumnMetadata expected : table.getColumnMetadataList()) {
            YashanDbColumn actual = existing.remove(expected.getName());
            if (actual == null) {
                // A model column missing in the database is created and its comment is applied later.
                changes.getNewColumns().add(expected);
                if (StringUtils.hasText(expected.getComment())) {
                    changes.getColumnComments().put(expected.getName(), expected.getComment());
                }
                continue;
            }
            if (columnDefinitionChanged(expected, actual)) {
                changes.getModifyColumns().add(expected);
            }
            if (!Objects.equals(emptyToNull(expected.getComment()), emptyToNull(actual.getComment()))) {
                changes.getColumnComments().put(expected.getName(), expected.getComment());
            }
        }

        PropertyConfig properties = AutoTableGlobalConfig.instance().getAutoTableProperties();
        String logicDropPrefix = properties.getLogicDropColumnPrefix();
        // Remaining columns exist only in the database; drop or logically rename them per configuration.
        Set<String> remaining = existing.keySet().stream()
                .filter(name -> !startsWithIgnoreCase(name, logicDropPrefix))
                .collect(Collectors.toCollection(HashSet::new));
        if (properties.getAutoDropColumn()) {
            changes.addDropColumns(remaining);
        } else if (StringUtils.hasText(logicDropPrefix)) {
            changes.addRenameColumns(remaining, logicDropPrefix);
        }
    }

    /**
     * Compares primary-key order because YashanDB treats it as part of the definition.
     */
    private void comparePrimaryKey(DefaultTableMetadata table, YashanCompareTableInfo changes) {
        List<ColumnMetadata> expected = table.getColumnMetadataList().stream()
                .filter(ColumnMetadata::isPrimary)
                .collect(Collectors.toList());
        YashanDbPrimary actual = mapper.selectPrimaryKey(table.getSchema(), table.getTableName());
        List<String> actualColumns = actual == null ? Collections.emptyList() : actual.getColumns();
        boolean same = expected.size() == actualColumns.size();
        if (same) {
            // Primary-key column order is significant for composite keys.
            for (int index = 0; index < expected.size(); index++) {
                if (!expected.get(index).getName().equalsIgnoreCase(actualColumns.get(index))) {
                    same = false;
                    break;
                }
            }
        }
        if (!same) {
            if (actual != null) {
                changes.setDropPrimaryKeyName(actual.getName());
            }
            changes.addNewPrimary(expected);
        }
    }

    /**
     * Compares index uniqueness, column order, sort direction, and configured cleanup rules.
     */
    private void compareIndexes(DefaultTableMetadata table, YashanCompareTableInfo changes) {
        Map<String, YashanDbIndex> existing = caseInsensitiveMap();
        mapper.selectIndexes(table.getSchema(), table.getTableName())
                .forEach(index -> existing.put(index.getName(), index));
        List<IndexMetadata> expectedIndexes = table.getIndexMetadataList() == null
                ? Collections.emptyList() : table.getIndexMetadataList();
        // Match generated names first, then fall back to the original name for existing deployments.
        for (IndexMetadata expected : expectedIndexes) {
            String expectedName = YashanIdentifierUtils.normalizeIndexName(expected.getName());
            YashanDbIndex actual = existing.remove(expectedName);
            if (actual == null) {
                actual = existing.remove(expected.getName());
            }
            if (actual == null) {
                changes.getNewIndexes().add(expected);
            } else if (!sameIndex(expected, actual)) {
                changes.addModifyIndex(expected);
            }
        }

        PropertyConfig properties = AutoTableGlobalConfig.instance().getAutoTableProperties();
        // Remove only indexes permitted by the configured AutoTable cleanup policy.
        for (String indexName : existing.keySet()) {
            boolean autoTableIndex = startsWithIgnoreCase(indexName, properties.getIndexPrefix());
            if ((autoTableIndex && properties.getAutoDropIndex())
                    || (!autoTableIndex && properties.getAutoDropCustomIndex())) {
                changes.getDropIndexes().add(indexName);
            }
        }
    }

    /**
     * Detects type, nullability, identity, and default-value differences for one column.
     */
    private static boolean columnDefinitionChanged(ColumnMetadata expected, YashanDbColumn actual) {
        // Compare each part separately because catalog types and defaults use different formats.
        if (!sameType(expected.getType(), actual)) {
            return true;
        }
        boolean expectedNotNull = expected.isNotNull() || expected.isPrimary() || expected.isAutoIncrement();
        if (expectedNotNull == actual.isNullable()) {
            return true;
        }
        if (expected.isAutoIncrement() != actual.isAutoIncrement()) {
            return true;
        }
        if (!expected.isAutoIncrement()
                && !Objects.equals(normalizeDefault(expectedDefault(expected)), normalizeDefault(actual.getDefaultValue()))) {
            return true;
        }
        return false;
    }

    /**
     * Compares canonical YashanDB types and their length/precision metadata.
     */
    private static boolean sameType(DatabaseTypeAndLength expected, YashanDbColumn actual) {
        String expectedType = canonicalType(expected.getType());
        String actualType = canonicalType(actual.getType());
        if (!expectedType.equals(actualType)) {
            return false;
        }
        if ("CHAR".equals(expectedType) || "VARCHAR".equals(expectedType) || "VARCHAR2".equals(expectedType)) {
            // A null model length means that the database's existing length is accepted.
            return expected.getLength() == null || Objects.equals(expected.getLength(), actual.getCharLength());
        }
        if ("NUMBER".equals(expectedType)) {
            // YashanDB reports precision and scale separately for numeric columns.
            Integer expectedScale = expected.getDecimalLength() == null ? 0 : expected.getDecimalLength();
            Integer actualScale = actual.getScale() == null ? 0 : actual.getScale();
            return (expected.getLength() == null || Objects.equals(expected.getLength(), actual.getPrecision()))
                    && Objects.equals(expectedScale, actualScale);
        }
        return true;
    }

    /**
     * Compares an expected AutoTable index with its catalog representation.
     */
    private static boolean sameIndex(IndexMetadata expected, YashanDbIndex actual) {
        boolean expectedUnique = expected.getType() == IndexTypeEnum.UNIQUE;
        if (expectedUnique != actual.isUnique() || expected.getColumns().size() != actual.getColumns().size()) {
            return false;
        }
        for (int index = 0; index < expected.getColumns().size(); index++) {
            IndexMetadata.IndexColumnParam column = expected.getColumns().get(index);
            if (!column.getColumn().equalsIgnoreCase(actual.getColumns().get(index))) {
                return false;
            }
            if (column.getSort() != null
                    && !column.getSort().name().equalsIgnoreCase(actual.getSorts().get(index))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Resolves the model's default-value annotation to its SQL value.
     */
    private static String expectedDefault(ColumnMetadata column) {
        if (column.getDefaultValueType() == org.dromara.autotable.annotation.enums.DefaultValueEnum.NULL) {
            return null;
        }
        if (column.getDefaultValueType() == org.dromara.autotable.annotation.enums.DefaultValueEnum.EMPTY_STRING) {
            return "";
        }
        return column.getDefaultValue();
    }

    /**
     * Normalizes catalog and model defaults before comparing them.
     */
    private static String normalizeDefault(String value) {
        value = emptyToNull(value);
        if (value == null || "NULL".equalsIgnoreCase(value)) {
            return null;
        }
        value = value.trim();
        // Catalog defaults may be quoted while model defaults are not.
        if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
            value = value.substring(1, value.length() - 1).replace("''", "'");
        }
        return value.toUpperCase(Locale.ROOT);
    }

    /**
     * Delegates type canonicalization to the YashanDB MySQL-compatibility mapper.
     */
    private static String canonicalType(String type) {
        return YashanTypeHelper.canonicalType(type);
    }

    /**
     * Tests an AutoTable configuration prefix without case sensitivity.
     */
    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return StringUtils.hasText(prefix) && value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Converts blank catalog values to null for stable comparisons.
     */
    private static String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    /**
     * Creates a map that treats YashanDB's folded identifier names case-insensitively.
     */
    private static <V> Map<String, V> caseInsensitiveMap() {
        return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * Creates the Java-to-YashanDB type mapping used by this strategy only.
     */
    private static Map<Class<?>, DefaultTypeEnumInterface> createTypeMapping() {
        Map<Class<?>, DefaultTypeEnumInterface> mapping = new HashMap<>();
        // Keep Java mappings local to YashanDB so existing database strategies retain their types.
        mapping.put(String.class, YashanDefaultTypeEnum.VARCHAR);
        mapping.put(Character.class, YashanDefaultTypeEnum.CHAR);
        mapping.put(char.class, YashanDefaultTypeEnum.CHAR);
        mapping.put(Byte.class, YashanDefaultTypeEnum.TINYINT);
        mapping.put(byte.class, YashanDefaultTypeEnum.TINYINT);
        mapping.put(Short.class, YashanDefaultTypeEnum.SMALLINT);
        mapping.put(short.class, YashanDefaultTypeEnum.SMALLINT);
        mapping.put(Integer.class, YashanDefaultTypeEnum.INTEGER);
        mapping.put(int.class, YashanDefaultTypeEnum.INTEGER);
        mapping.put(Long.class, YashanDefaultTypeEnum.BIGINT);
        mapping.put(long.class, YashanDefaultTypeEnum.BIGINT);
        mapping.put(BigInteger.class, YashanDefaultTypeEnum.BIGINT);
        mapping.put(Boolean.class, YashanDefaultTypeEnum.TINYINT);
        mapping.put(boolean.class, YashanDefaultTypeEnum.TINYINT);
        mapping.put(Float.class, YashanDefaultTypeEnum.FLOAT);
        mapping.put(float.class, YashanDefaultTypeEnum.FLOAT);
        mapping.put(Double.class, YashanDefaultTypeEnum.DOUBLE);
        mapping.put(double.class, YashanDefaultTypeEnum.DOUBLE);
        mapping.put(BigDecimal.class, YashanDefaultTypeEnum.DECIMAL);
        mapping.put(Date.class, YashanDefaultTypeEnum.TIMESTAMP);
        mapping.put(java.sql.Timestamp.class, YashanDefaultTypeEnum.TIMESTAMP);
        mapping.put(LocalDateTime.class, YashanDefaultTypeEnum.TIMESTAMP);
        mapping.put(java.sql.Date.class, YashanDefaultTypeEnum.DATE);
        mapping.put(LocalDate.class, YashanDefaultTypeEnum.DATE);
        mapping.put(java.sql.Time.class, YashanDefaultTypeEnum.TIME);
        mapping.put(LocalTime.class, YashanDefaultTypeEnum.TIME);
        mapping.put(byte[].class, YashanDefaultTypeEnum.BLOB);
        mapping.put(Blob.class, YashanDefaultTypeEnum.BLOB);
        mapping.put(Clob.class, YashanDefaultTypeEnum.CLOB);
        return Collections.unmodifiableMap(mapping);
    }
}
