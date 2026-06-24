package org.dromara.autotable.strategy.sqlserver.mapper;

import org.dromara.autotable.core.dynamicds.DataSourceManager;
import org.dromara.autotable.core.utils.DBHelper;
import org.dromara.autotable.strategy.sqlserver.data.dbdata.SqlServerDbColumn;
import org.dromara.autotable.strategy.sqlserver.data.dbdata.SqlServerDbIndex;
import org.dromara.autotable.strategy.sqlserver.data.dbdata.SqlServerDbPrimary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询/维护 SQLServer 表结构的 Mapper。
 *
 * <p>使用 {@code DBHelper} 的 {@code :name} 命名占位符（本质为字符串替换，参考 PG mapper）。
 * 系统目录查询基于 {@code sys.tables} / {@code sys.columns} / {@code sys.indexes} /
 * {@code sys.index_columns} / {@code sys.default_constraints} / {@code sys.extended_properties}。</p>
 *
 * @author don
 */
public class SqlServerTablesMapper {

    private static Map<String, Object> params(String schema, String tableName) {
        Map<String, Object> map = new HashMap<>(4);
        map.put("tableName", tableName);
        map.put("schema", schema);
        return map;
    }

    /**
     * 查询表注释（来自 sys.extended_properties，name='MS_Description'，minor_id=0）。
     *
     * @param schema    schema
     * @param tableName 表名
     * @return 表注释，无则 null
     */
    public String selectTableDescription(String schema, String tableName) {

        String sql = "SELECT CAST(ep.value AS NVARCHAR(MAX)) AS description " +
                "FROM sys.extended_properties ep " +
                "JOIN sys.tables t ON ep.major_id = t.object_id " +
                "JOIN sys.schemas s ON t.schema_id = s.schema_id " +
                "WHERE s.name = ':schema' AND t.name = ':tableName' " +
                "AND ep.name = 'MS_Description' AND ep.minor_id = 0;";

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryValue(connection, sql, params(schema, tableName));
        });
    }

    /**
     * 查询表的全部字段信息。
     *
     * @param schema    schema
     * @param tableName 表名
     * @return 字段信息列表
     */
    public List<SqlServerDbColumn> selectTableFieldDetail(String schema, String tableName) {

        String sql = "SELECT " +
                "CAST(CASE WHEN EXISTS( " +
                "  SELECT 1 FROM sys.indexes i " +
                "  JOIN sys.index_columns ic ON i.object_id = ic.object_id AND i.index_id = ic.index_id " +
                "  WHERE i.object_id = c.object_id AND i.is_primary_key = 1 AND ic.column_id = c.column_id " +
                ") THEN 1 ELSE 0 END AS BIT) AS [primary], " +
                "CAST(ep.value AS NVARCHAR(MAX)) AS description, " +
                "c.name AS columnName, " +
                "t.name AS dataType, " +
                "c.max_length AS characterMaximumLength, " +
                "c.precision AS numericPrecision, " +
                "c.scale AS numericScale, " +
                "CASE WHEN c.is_nullable = 1 THEN 'YES' ELSE 'NO' END AS isNullable, " +
                "CASE WHEN c.is_identity = 1 THEN 'YES' ELSE 'NO' END AS isIdentity, " +
                "OBJECT_DEFINITION(c.default_object_id) AS columnDefault, " +
                "dc.name AS defaultConstraintName " +
                "FROM sys.columns c " +
                "JOIN sys.tables tb ON c.object_id = tb.object_id " +
                "JOIN sys.schemas s ON tb.schema_id = s.schema_id " +
                "JOIN sys.types t ON c.user_type_id = t.user_type_id " +
                "LEFT JOIN sys.default_constraints dc ON dc.object_id = c.default_object_id " +
                "LEFT JOIN sys.extended_properties ep " +
                "  ON ep.major_id = tb.object_id AND ep.minor_id = c.column_id AND ep.name = 'MS_Description' " +
                "WHERE s.name = ':schema' AND tb.name = ':tableName' " +
                "ORDER BY c.column_id;";

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObjectList(connection, sql, params(schema, tableName), SqlServerDbColumn.class);
        });
    }

    /**
     * 查询表的全部索引信息（排除主键索引和唯一约束索引）。
     *
     * @param schema    schema
     * @param tableName 表名
     * @return 索引信息列表
     */
    public List<SqlServerDbIndex> selectTableIndexesDetail(String schema, String tableName) {

        String sql = "SELECT " +
                "CAST(ep.value AS NVARCHAR(MAX)) AS description, " +
                "i.name AS indexName, " +
                "CASE WHEN i.is_unique = 1 THEN 'YES' ELSE 'NO' END AS isUnique, " +
                "STUFF((SELECT ',' + col.name " +
                "       FROM sys.index_columns ic " +
                "       JOIN sys.columns col ON ic.object_id = col.object_id AND ic.column_id = col.column_id " +
                "       WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id AND ic.is_included_column = 0 " +
                "       ORDER BY ic.key_ordinal " +
                "       FOR XML PATH('')), 1, 1, '') AS indexColumns " +
                "FROM sys.indexes i " +
                "JOIN sys.tables tb ON i.object_id = tb.object_id " +
                "JOIN sys.schemas s ON tb.schema_id = s.schema_id " +
                "LEFT JOIN sys.extended_properties ep " +
                "  ON ep.class = 7 AND ep.major_id = tb.object_id AND ep.minor_id = i.index_id AND ep.name = 'MS_Description' " +
                "WHERE s.name = ':schema' AND tb.name = ':tableName' " +
                "AND i.is_primary_key = 0 AND i.is_unique_constraint = 0 AND i.index_id > 0;";

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObjectList(connection, sql, params(schema, tableName), SqlServerDbIndex.class);
        });
    }

    /**
     * 查询表的主键信息。
     *
     * @param schema    schema
     * @param tableName 表名
     * @return 主键信息，无主键返回 null
     */
    public SqlServerDbPrimary selectPrimaryKeyName(String schema, String tableName) {

        String sql = "SELECT i.name AS primaryName, " +
                "STUFF((SELECT ',' + col.name " +
                "       FROM sys.index_columns ic " +
                "       JOIN sys.columns col ON ic.object_id = col.object_id AND ic.column_id = col.column_id " +
                "       WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id " +
                "       ORDER BY ic.key_ordinal " +
                "       FOR XML PATH('')), 1, 1, '') AS columns " +
                "FROM sys.indexes i " +
                "JOIN sys.tables tb ON i.object_id = tb.object_id " +
                "JOIN sys.schemas s ON tb.schema_id = s.schema_id " +
                "WHERE s.name = ':schema' AND tb.name = ':tableName' AND i.is_primary_key = 1;";

        return DataSourceManager.useConnection(connection -> {
            return DBHelper.queryObject(connection, sql, params(schema, tableName), SqlServerDbPrimary.class);
        });
    }
}
