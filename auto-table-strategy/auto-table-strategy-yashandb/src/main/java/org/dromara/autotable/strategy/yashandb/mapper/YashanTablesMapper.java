package org.dromara.autotable.strategy.yashandb.mapper;

import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.utils.StringUtils;
import org.dromara.autotable.strategy.yashandb.data.dbdata.YashanDbColumn;
import org.dromara.autotable.strategy.yashandb.data.dbdata.YashanDbIndex;
import org.dromara.autotable.strategy.yashandb.data.dbdata.YashanDbPrimary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads table metadata from YashanDB catalog views for the MySQL-compatible
 * AutoTable strategy.
 */
public class YashanTablesMapper {
    /**
     * Checks whether a table exists in the requested schema, ignoring identifier case.
     */
    public boolean tableExists(String schema, String tableName) {
        return DataSourceManager.useConnection(connection -> {
            String owner = resolveSchema(connection, schema);
            // UPPER comparisons preserve matches for quoted and unquoted model names.
            String sql = "SELECT COUNT(*) FROM ALL_TABLES WHERE UPPER(OWNER) = UPPER(?) "
                    + "AND UPPER(TABLE_NAME) = UPPER(?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, owner);
                statement.setString(2, tableName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() && resultSet.getInt(1) > 0;
                }
            } catch (SQLException exception) {
                throw metadataFailure(owner, tableName, exception);
            }
        });
    }

    /**
     * Lists user-visible tables in the requested schema in catalog order.
     */
    public List<String> selectTables(String schema) {
        return DataSourceManager.useConnection(connection -> {
            String owner = resolveSchema(connection, schema);
            String sql = "SELECT TABLE_NAME FROM ALL_TABLES WHERE UPPER(OWNER) = UPPER(?) ORDER BY TABLE_NAME";
            List<String> tables = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, owner);
                try (ResultSet resultSet = statement.executeQuery()) {
                    // Preserve the catalog order used by AutoTable's table selector.
                    while (resultSet.next()) {
                        tables.add(resultSet.getString(1));
                    }
                }
                return tables;
            } catch (SQLException exception) {
                throw metadataFailure(owner, null, exception);
            }
        });
    }

    /**
     * Reads a table comment from YashanDB's table-comments catalog view.
     */
    public String selectTableComment(String schema, String tableName) {
        return DataSourceManager.useConnection(connection -> {
            String owner = resolveSchema(connection, schema);
            // Comments are stored separately from ALL_TABLES in YashanDB.
            String sql = "SELECT COMMENTS FROM ALL_TAB_COMMENTS WHERE UPPER(OWNER) = UPPER(?) "
                    + "AND UPPER(TABLE_NAME) = UPPER(?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, owner);
                statement.setString(2, tableName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? resultSet.getString(1) : null;
                }
            } catch (SQLException exception) {
                throw metadataFailure(owner, tableName, exception);
            }
        });
    }

    /**
     * Reads columns, types, nullability, defaults, identity flags, and comments.
     */
    public List<YashanDbColumn> selectColumns(String schema, String tableName) {
        return DataSourceManager.useConnection(connection -> {
            String owner = resolveSchema(connection, schema);
            // Join column comments and retain identity/precision fields needed for comparison.
            String sql = "SELECT c.COLUMN_NAME, c.DATA_TYPE, c.CHAR_LENGTH, c.DATA_PRECISION, "
                    + "c.DATA_SCALE, c.NULLABLE, c.DATA_DEFAULT, c.IDENTITY_COLUMN, comments.COMMENTS "
                    + "FROM ALL_TAB_COLUMNS c LEFT JOIN ALL_COL_COMMENTS comments "
                    + "ON comments.OWNER = c.OWNER AND comments.TABLE_NAME = c.TABLE_NAME "
                    + "AND comments.COLUMN_NAME = c.COLUMN_NAME "
                    + "WHERE UPPER(c.OWNER) = UPPER(?) AND UPPER(c.TABLE_NAME) = UPPER(?) "
                    + "ORDER BY c.COLUMN_ID";
            List<YashanDbColumn> columns = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, owner);
                statement.setString(2, tableName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    // Convert nullable catalog values to the metadata model used by the strategy.
                    while (resultSet.next()) {
                        YashanDbColumn column = new YashanDbColumn();
                        column.setName(resultSet.getString("COLUMN_NAME"));
                        column.setType(resultSet.getString("DATA_TYPE"));
                        column.setCharLength(integer(resultSet, "CHAR_LENGTH"));
                        column.setPrecision(integer(resultSet, "DATA_PRECISION"));
                        column.setScale(integer(resultSet, "DATA_SCALE"));
                        column.setNullable("Y".equalsIgnoreCase(resultSet.getString("NULLABLE")));
                        column.setDefaultValue(resultSet.getString("DATA_DEFAULT"));
                        column.setAutoIncrement("Y".equalsIgnoreCase(resultSet.getString("IDENTITY_COLUMN")));
                        column.setComment(resultSet.getString("COMMENTS"));
                        columns.add(column);
                    }
                }
                return columns;
            } catch (SQLException exception) {
                throw metadataFailure(owner, tableName, exception);
            }
        });
    }

    /**
     * Reads the primary-key constraint and preserves its column order.
     */
    public YashanDbPrimary selectPrimaryKey(String schema, String tableName) {
        return DataSourceManager.useConnection(connection -> {
            String owner = resolveSchema(connection, schema);
            // Constraint rows are ordered by POSITION so composite keys remain deterministic.
            String sql = "SELECT constraints.CONSTRAINT_NAME, columns.COLUMN_NAME "
                    + "FROM ALL_CONSTRAINTS constraints JOIN ALL_CONS_COLUMNS columns "
                    + "ON columns.OWNER = constraints.OWNER AND columns.TABLE_NAME = constraints.TABLE_NAME "
                    + "AND columns.CONSTRAINT_NAME = constraints.CONSTRAINT_NAME "
                    + "WHERE UPPER(constraints.OWNER) = UPPER(?) AND UPPER(constraints.TABLE_NAME) = UPPER(?) "
                    + "AND constraints.CONSTRAINT_TYPE = 'P' ORDER BY columns.POSITION";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, owner);
                statement.setString(2, tableName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    YashanDbPrimary primary = null;
                    // One row is returned per primary-key column; aggregate them into one object.
                    while (resultSet.next()) {
                        if (primary == null) {
                            primary = new YashanDbPrimary();
                            primary.setName(resultSet.getString("CONSTRAINT_NAME"));
                        }
                        primary.getColumns().add(resultSet.getString("COLUMN_NAME"));
                    }
                    return primary;
                }
            } catch (SQLException exception) {
                throw metadataFailure(owner, tableName, exception);
            }
        });
    }

    /**
     * Reads non-primary indexes, including uniqueness, columns, and sort direction.
     */
    public List<YashanDbIndex> selectIndexes(String schema, String tableName) {
        return DataSourceManager.useConnection(connection -> {
            String owner = resolveSchema(connection, schema);
            // Exclude LOB/system indexes and primary-key backing indexes from AutoTable indexes.
            String sql = "SELECT DISTINCT indexes.INDEX_NAME, indexes.UNIQUENESS, columns.COLUMN_NAME, "
                    + "columns.COLUMN_POSITION, columns.DESCEND "
                    + "FROM ALL_INDEXES indexes JOIN ALL_IND_COLUMNS columns "
                    + "ON columns.INDEX_OWNER = indexes.OWNER AND columns.TABLE_OWNER = indexes.TABLE_OWNER "
                    + "AND columns.TABLE_NAME = indexes.TABLE_NAME "
                    + "AND columns.INDEX_NAME = indexes.INDEX_NAME "
                    + "WHERE UPPER(indexes.TABLE_OWNER) = UPPER(?) AND UPPER(indexes.TABLE_NAME) = UPPER(?) "
                    + "AND indexes.INDEX_TYPE <> 'LOB' AND indexes.INDEX_NAME NOT LIKE 'SYS_IL%' "
                    + "AND NOT EXISTS (SELECT 1 FROM ALL_CONSTRAINTS pk "
                    + "WHERE pk.OWNER = indexes.OWNER AND pk.TABLE_NAME = indexes.TABLE_NAME "
                    + "AND pk.INDEX_NAME = indexes.INDEX_NAME AND pk.CONSTRAINT_TYPE = 'P') "
                    + "ORDER BY indexes.INDEX_NAME, columns.COLUMN_POSITION";
            Map<String, YashanDbIndex> indexes = new LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, owner);
                statement.setString(2, tableName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    // Catalog rows are one-per-index-column, so group them by index name.
                    while (resultSet.next()) {
                        String name = resultSet.getString("INDEX_NAME");
                        YashanDbIndex index = indexes.get(name);
                        if (index == null) {
                            index = new YashanDbIndex();
                            index.setName(name);
                            String uniqueness = resultSet.getString("UNIQUENESS");
                            index.setUnique("UNIQUE".equalsIgnoreCase(uniqueness) || "Y".equalsIgnoreCase(uniqueness));
                            indexes.put(name, index);
                        }
                        index.getColumns().add(resultSet.getString("COLUMN_NAME"));
                        index.getSorts().add(resultSet.getString("DESCEND"));
                    }
                }
                return new ArrayList<>(indexes.values());
            } catch (SQLException exception) {
                throw metadataFailure(owner, tableName, exception);
            }
        });
    }

    /**
     * Resolves an explicit schema or the current JDBC session schema.
     */
    private static String resolveSchema(Connection connection, String schema) {
        if (StringUtils.hasText(schema)) {
            return unquote(schema);
        }
        // Resolve the session schema only when the entity did not specify one.
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT SYS_CONTEXT('USERENV','CURRENT_SCHEMA') FROM DUAL");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
            return connection.getSchema();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to resolve the current YashanDB schema", exception);
        }
    }

    /**
     * Removes SQL identifier quotes supplied by AutoTable configuration.
     */
    private static String unquote(String value) {
        if (value != null && value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * Reads a nullable numeric catalog value without treating SQL NULL as zero.
     */
    private static Integer integer(ResultSet resultSet, String column) throws SQLException {
        Number value = (Number) resultSet.getObject(column);
        return value == null ? null : value.intValue();
    }

    /**
     * Creates a consistent metadata error containing the affected schema and table.
     */
    private static IllegalStateException metadataFailure(String schema, String table, SQLException exception) {
        String object = table == null ? schema : schema + "." + table;
        return new IllegalStateException("Failed to read YashanDB metadata for " + object, exception);
    }
}
