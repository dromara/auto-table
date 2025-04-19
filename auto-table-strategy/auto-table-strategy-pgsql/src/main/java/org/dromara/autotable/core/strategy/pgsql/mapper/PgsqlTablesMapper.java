package org.dromara.autotable.core.strategy.pgsql.mapper;

import org.dromara.autotable.core.strategy.pgsql.data.dbdata.PgsqlDbColumn;
import org.dromara.autotable.core.strategy.pgsql.data.dbdata.PgsqlDbIndex;
import org.dromara.autotable.core.strategy.pgsql.data.dbdata.PgsqlDbPrimary;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;


/**
 * 创建更新表结构的Mapper
 *
 * @author don
 */
public interface PgsqlTablesMapper {

    /**
     * 查询表名注释
     *
     * @param schema    schema
     * @param tableName 表名
     * @return 表注释
     */
    @Select("SELECT des.description FROM pg_catalog.pg_description des " +
            "LEFT JOIN pg_catalog.pg_class clas ON des.objoid = clas.oid " +
            "LEFT JOIN pg_catalog.pg_namespace nams ON clas.relnamespace = nams.oid " +
            "WHERE nams.nspname = #{schema} AND clas.relname = #{tableName} AND des.objsubid = 0;")
    String selectTableDescription(String schema, String tableName);

    /**
     * 查询所有字段信息
     *
     * @param tableName 表名
     * @return 字段信息
     */
    @Results({
            @Result(column = "primary", property = "primary"),
            @Result(column = "description", property = "description"),
            @Result(column = "table_catalog", property = "tableCatalog"),
            @Result(column = "table_schema", property = "tableSchema"),
            @Result(column = "table_name", property = "tableName"),
            @Result(column = "column_name", property = "columnName"),
            @Result(column = "ordinal_position", property = "ordinalPosition"),
            @Result(column = "column_default", property = "columnDefault"),
            @Result(column = "is_nullable", property = "isNullable"),
            @Result(column = "data_type", property = "dataType"),
            @Result(column = "character_maximum_length", property = "characterMaximumLength"),
            @Result(column = "character_octet_length", property = "characterOctetLength"),
            @Result(column = "numeric_precision", property = "numericPrecision"),
            @Result(column = "numeric_precision_radix", property = "numericPrecisionRadix"),
            @Result(column = "numeric_scale", property = "numericScale"),
            @Result(column = "datetime_precision", property = "datetimePrecision"),
            @Result(column = "interval_type", property = "intervalType"),
            @Result(column = "interval_precision", property = "intervalPrecision"),
            @Result(column = "character_set_catalog", property = "characterSetCatalog"),
            @Result(column = "character_set_schema", property = "characterSetSchema"),
            @Result(column = "character_set_name", property = "characterSetName"),
            @Result(column = "collation_catalog", property = "collationCatalog"),
            @Result(column = "collation_schema", property = "collationSchema"),
            @Result(column = "collation_name", property = "collationName"),
            @Result(column = "domain_catalog", property = "domainCatalog"),
            @Result(column = "domain_schema", property = "domainSchema"),
            @Result(column = "domain_name", property = "domainName"),
            @Result(column = "udt_catalog", property = "udtCatalog"),
            @Result(column = "udt_schema", property = "udtSchema"),
            @Result(column = "udt_name", property = "udtName"),
            @Result(column = "scope_catalog", property = "scopeCatalog"),
            @Result(column = "scope_schema", property = "scopeSchema"),
            @Result(column = "scope_name", property = "scopeName"),
            @Result(column = "maximum_cardinality", property = "maximumCardinality"),
            @Result(column = "dtd_identifier", property = "dtdIdentifier"),
            @Result(column = "is_self_referencing", property = "isSelfReferencing"),
            @Result(column = "is_identity", property = "isIdentity"),
            @Result(column = "identity_generation", property = "identityGeneration"),
            @Result(column = "identity_start", property = "identityStart"),
            @Result(column = "identity_increment", property = "identityIncrement"),
            @Result(column = "identity_maximum", property = "identityMaximum"),
            @Result(column = "identity_minimum", property = "identityMinimum"),
            @Result(column = "identity_cycle", property = "identityCycle"),
            @Result(column = "is_generated", property = "isGenerated"),
            @Result(column = "generation_expression", property = "generationExpression"),
            @Result(column = "is_updatable", property = "isUpdatable"),
    })
    @Select("SELECT DISTINCT key_col.column_name IS NOT NULL AS primary, des.description, cols.* " +
            "FROM information_schema.columns cols " +
            "LEFT JOIN information_schema.key_column_usage key_col ON key_col.column_name = cols.column_name " +
            "LEFT JOIN pg_catalog.pg_class clas ON clas.relname = cols.table_name AND clas.relnamespace = ( SELECT oid FROM pg_namespace WHERE nspname = cols.table_schema ) " +
            "LEFT JOIN pg_catalog.pg_description des ON des.objoid = clas.oid AND cols.ordinal_position = des.objsubid " +
            "WHERE cols.table_schema = #{schema} AND cols.table_name = #{tableName};")
    List<PgsqlDbColumn> selectTableFieldDetail(String schema, String tableName);

    /**
     * <p>查询所有索引信息
     * <p>关于pg_constraint表的contype值有以下几种：
     * <p>主键约束（PRIMARY KEY）：contype 字段的值为 'p'
     * <p>唯一约束（UNIQUE）：contype 字段的值为 'u'
     * <p>检查约束（CHECK）：contype 字段的值为 'c'
     * <p>外键约束（FOREIGN KEY）：contype 字段的值为 'f'
     * <p>排他约束（EXCLUDE）：contype 字段的值为 'x'
     *
     * @param tableName 表名
     * @return 索引信息
     */
    @Results({
            @Result(column = "description", property = "description"),
            @Result(column = "schemaname", property = "schemaName"),
            @Result(column = "tablename", property = "tableName"),
            @Result(column = "indexname", property = "indexName"),
            @Result(column = "tablespace", property = "tablespace"),
            @Result(column = "indexdef", property = "indexdef"),
    })
    @Select("SELECT DISTINCT des.description, idxs.* " +
            "FROM pg_catalog.pg_indexes idxs " +
            "LEFT JOIN pg_catalog.pg_class clas ON idxs.indexname = clas.relname " +
            "LEFT JOIN pg_catalog.pg_description des ON clas.oid = des.objoid " +
            "LEFT JOIN pg_catalog.pg_constraint cst ON idxs.indexname = cst.conname " +
            "WHERE idxs.schemaname = #{schema} AND idxs.tablename = #{tableName} AND cst.contype is null;")
    List<PgsqlDbIndex> selectTableIndexesDetail(String schema, String tableName);

    /**
     * 查询表下的主键信息
     *
     * @param tableName 表明
     * @return 主键名
     */
    @Results({
            @Result(column = "primary_name", property = "primaryName"),
            @Result(column = "columns", property = "columns"),
    })
    @Select("SELECT key_col.constraint_name as primary_name, string_agg(key_col.column_name, ',' ORDER BY key_col.ordinal_position ASC) as columns " +
            "FROM information_schema.key_column_usage key_col " +
            "WHERE key_col.table_schema = #{schema} AND key_col.table_name = #{tableName} " +
            "GROUP BY key_col.constraint_name;")
    PgsqlDbPrimary selectPrimaryKeyName(String schema, String tableName);
}
