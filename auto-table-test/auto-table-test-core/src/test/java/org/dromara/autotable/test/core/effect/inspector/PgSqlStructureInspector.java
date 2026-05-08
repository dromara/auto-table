package org.dromara.autotable.test.core.effect.inspector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL 数据库结构查询实现
 * <p>
 * 通过查询 information_schema 和 pg_catalog 获取真实数据库表结构信息。
 */
public class PgSqlStructureInspector implements DbStructureInspector {

    private final Connection connection;
    private final String schema;

    public PgSqlStructureInspector(Connection connection, String schema) {
        this.connection = connection;
        this.schema = schema;
    }

    @Override
    public TableSchema getTableSchema(String tableName) {
        String sql = "SELECT c.relname AS table_name, n.nspname AS table_schema, " +
                "obj_description(c.oid, 'pg_class') AS table_comment " +
                "FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid " +
                "WHERE n.nspname = ? AND c.relname = ? AND c.relkind = 'r'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TableSchema tableSchema = new TableSchema();
                    tableSchema.setTableName(rs.getString("table_name"));
                    tableSchema.setSchema(rs.getString("table_schema"));
                    tableSchema.setComment(rs.getString("table_comment"));
                    tableSchema.setColumns(getColumns(tableName));
                    tableSchema.setIndexes(getIndexes(tableName));
                    tableSchema.setPrimaryKeys(getPrimaryKeys(tableName));
                    return tableSchema;
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
        String sql = "SELECT column_name, data_type, character_maximum_length, " +
                "numeric_precision, numeric_scale, collation_name, ordinal_position, " +
                "is_nullable, column_default " +
                "FROM information_schema.columns " +
                "WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ColumnSchema column = new ColumnSchema();
                    column.setName(rs.getString("column_name"));
                    column.setType(rs.getString("data_type"));

                    Integer charMaxLength = rs.getObject("character_maximum_length", Integer.class);
                    Integer numericPrecision = rs.getObject("numeric_precision", Integer.class);
                    if (charMaxLength != null) {
                        column.setLength(charMaxLength.intValue());
                    } else if (numericPrecision != null) {
                        column.setLength(numericPrecision);
                    }

                    Integer numericScale = rs.getObject("numeric_scale", Integer.class);
                    if (numericScale != null) {
                        column.setDecimalLength(numericScale);
                    }

                    column.setCollate(rs.getString("collation_name"));
                    column.setOrdinalPosition(rs.getInt("ordinal_position"));
                    column.setNullable("YES".equalsIgnoreCase(rs.getString("is_nullable")));
                    column.setDefaultValue(rs.getString("column_default"));

                    // 检查是否是自增（serial 或生成列）
                    String defaultValue = column.getDefaultValue();
                    if (defaultValue != null && (defaultValue.contains("nextval(") ||
                            defaultValue.contains("generated always"))) {
                        column.setAutoIncrement(true);
                    }

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
        String sql = "SELECT i.relname AS index_name, am.amname AS index_type, " +
                "a.attname AS column_name, ix.indisunique AS is_unique " +
                "FROM pg_index ix " +
                "JOIN pg_class t ON t.oid = ix.indrelid " +
                "JOIN pg_class i ON i.oid = ix.indexrelid " +
                "JOIN pg_am am ON i.relam = am.oid " +
                "JOIN pg_namespace n ON n.oid = t.relnamespace " +
                "JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(ix.indkey) " +
                "WHERE n.nspname = ? AND t.relname = ? AND NOT ix.indisprimary " +
                "ORDER BY i.relname, a.attnum";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.Map<String, IndexSchema> indexMap = new java.util.LinkedHashMap<>();
                while (rs.next()) {
                    String indexName = rs.getString("index_name");
                    String indexType = rs.getString("index_type");
                    IndexSchema index = indexMap.computeIfAbsent(indexName, k -> {
                        IndexSchema idx = new IndexSchema();
                        idx.setName(indexName);
                        idx.setType(indexType);
                        return idx;
                    });
                    if (index.getColumns() == null) {
                        index.setColumns(new ArrayList<>());
                    }
                    index.getColumns().add(rs.getString("column_name"));
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
        String sql = "SELECT tc.constraint_name, kcu.column_name, kcu.ordinal_position " +
                "FROM information_schema.table_constraints tc " +
                "JOIN information_schema.key_column_usage kcu " +
                "ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema " +
                "WHERE tc.table_schema = ? AND tc.table_name = ? AND tc.constraint_type = 'PRIMARY KEY' " +
                "ORDER BY kcu.ordinal_position";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                PrimaryKeySchema pk = new PrimaryKeySchema();
                List<String> columns = new ArrayList<>();
                while (rs.next()) {
                    if (pk.getName() == null) {
                        pk.setName(rs.getString("constraint_name"));
                    }
                    columns.add(rs.getString("column_name"));
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
        String sql = "SELECT 1 FROM information_schema.tables " +
                "WHERE table_schema = ? AND table_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询表存在性失败: " + tableName, e);
        }
    }
}
