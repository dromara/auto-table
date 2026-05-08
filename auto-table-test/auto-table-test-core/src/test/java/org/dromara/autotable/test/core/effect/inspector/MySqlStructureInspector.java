package org.dromara.autotable.test.core.effect.inspector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL 数据库结构查询实现
 * <p>
 * 通过查询 information_schema 获取真实数据库表结构信息。
 */
public class MySqlStructureInspector implements DbStructureInspector {

    private final Connection connection;
    private final String databaseName;

    public MySqlStructureInspector(Connection connection, String databaseName) {
        this.connection = connection;
        this.databaseName = databaseName;
    }

    @Override
    public TableSchema getTableSchema(String tableName) {
        String sql = "SELECT TABLE_NAME, TABLE_SCHEMA, TABLE_COMMENT, ENGINE, " +
                "TABLE_COLLATION FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, databaseName);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TableSchema schema = new TableSchema();
                    schema.setTableName(rs.getString("TABLE_NAME"));
                    schema.setSchema(rs.getString("TABLE_SCHEMA"));
                    schema.setComment(rs.getString("TABLE_COMMENT"));
                    schema.setEngine(rs.getString("ENGINE"));
                    String collation = rs.getString("TABLE_COLLATION");
                    if (collation != null) {
                        schema.setCollate(collation);
                        // charset 从 collation 中推断，如 utf8mb4_unicode_ci -> utf8mb4
                        int idx = collation.indexOf('_');
                        if (idx > 0) {
                            schema.setCharset(collation.substring(0, idx));
                        }
                    }
                    schema.setColumns(getColumns(tableName));
                    schema.setIndexes(getIndexes(tableName));
                    schema.setPrimaryKeys(getPrimaryKeys(tableName));
                    return schema;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询表结构失败: " + tableName, e);
        }
        return null;
    }

    @Override
    public List<ColumnSchema> getColumns(String tableName) {
        List<ColumnSchema> columns = new ArrayList<>();
        String sql = "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, " +
                "NUMERIC_PRECISION, NUMERIC_SCALE, COLUMN_COMMENT, IS_NULLABLE, " +
                "COLUMN_DEFAULT, EXTRA, CHARACTER_SET_NAME, COLLATION_NAME, ORDINAL_POSITION " +
                "FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, databaseName);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ColumnSchema column = new ColumnSchema();
                    column.setName(rs.getString("COLUMN_NAME"));
                    column.setType(rs.getString("DATA_TYPE"));

                    // 长度处理
                    Long charMaxLength = rs.getObject("CHARACTER_MAXIMUM_LENGTH", Long.class);
                    Integer numericPrecision = rs.getObject("NUMERIC_PRECISION", Integer.class);
                    if (charMaxLength != null) {
                        column.setLength(charMaxLength.intValue());
                    } else if (numericPrecision != null) {
                        column.setLength(numericPrecision);
                    }

                    // 小数位数
                    Integer numericScale = rs.getObject("NUMERIC_SCALE", Integer.class);
                    if (numericScale != null) {
                        column.setDecimalLength(numericScale);
                    }

                    column.setComment(rs.getString("COLUMN_COMMENT"));
                    column.setNullable("YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")));
                    column.setDefaultValue(rs.getString("COLUMN_DEFAULT"));
                    column.setAutoIncrement(rs.getString("EXTRA") != null &&
                            rs.getString("EXTRA").toLowerCase().contains("auto_increment"));
                    column.setCharset(rs.getString("CHARACTER_SET_NAME"));
                    column.setCollate(rs.getString("COLLATION_NAME"));
                    column.setOrdinalPosition(rs.getInt("ORDINAL_POSITION"));
                    columns.add(column);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询列信息失败: " + tableName, e);
        }
        return columns;
    }

    @Override
    public List<IndexSchema> getIndexes(String tableName) {
        List<IndexSchema> indexes = new ArrayList<>();
        // 查询非主键索引
        String sql = "SELECT INDEX_NAME, INDEX_TYPE, NON_UNIQUE, COLUMN_NAME, " +
                "SEQ_IN_INDEX, INDEX_COMMENT " +
                "FROM information_schema.STATISTICS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND INDEX_NAME != 'PRIMARY' " +
                "ORDER BY INDEX_NAME, SEQ_IN_INDEX";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, databaseName);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.Map<String, IndexSchema> indexMap = new java.util.LinkedHashMap<>();
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    IndexSchema index = indexMap.computeIfAbsent(indexName, k -> {
                        IndexSchema idx = new IndexSchema();
                        idx.setName(indexName);
                        return idx;
                    });
                    if (index.getColumns() == null) {
                        index.setColumns(new ArrayList<>());
                    }
                    index.getColumns().add(rs.getString("COLUMN_NAME"));
                    index.setType(rs.getString("INDEX_TYPE"));
                    index.setComment(rs.getString("INDEX_COMMENT"));
                }
                indexes.addAll(indexMap.values());
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询索引信息失败: " + tableName, e);
        }
        return indexes;
    }

    @Override
    public List<PrimaryKeySchema> getPrimaryKeys(String tableName) {
        List<PrimaryKeySchema> primaryKeys = new ArrayList<>();
        String sql = "SELECT INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX " +
                "FROM information_schema.STATISTICS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND INDEX_NAME = 'PRIMARY' " +
                "ORDER BY SEQ_IN_INDEX";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, databaseName);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                PrimaryKeySchema pk = new PrimaryKeySchema();
                pk.setName("PRIMARY");
                List<String> columns = new ArrayList<>();
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME"));
                }
                pk.setColumns(columns);
                if (!columns.isEmpty()) {
                    primaryKeys.add(pk);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询主键信息失败: " + tableName, e);
        }
        return primaryKeys;
    }

    @Override
    public boolean tableExists(String tableName) {
        String sql = "SELECT 1 FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, databaseName);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询表存在性失败: " + tableName, e);
        }
    }
}
