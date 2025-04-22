package org.dromara.autotable.core.strategy.h2.mapper;

import org.dromara.autotable.core.strategy.h2.data.dbdata.InformationSchemaColumns;
import org.dromara.autotable.core.strategy.h2.data.dbdata.InformationSchemaIndexes;
import org.dromara.autotable.core.strategy.h2.data.dbdata.InformationSchemaTables;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;


/**
 * 创建更新表结构的Mapper
 *
 * @author don
 */
public interface H2TablesMapper {

    /**
     * 根据表名查询表在库中是否存在
     *
     * @param tableSchema schema名
     * @param tableName   表名
     * @return InformationSchemaTables
     */
    @Results({
            @Result(column = "TABLE_CATALOG", property = "tableCatalog"),
            @Result(column = "TABLE_SCHEMA", property = "tableSchema"),
            @Result(column = "TABLE_NAME", property = "tableName"),
            @Result(column = "TABLE_TYPE", property = "tableType"),
            @Result(column = "IS_INSERTABLE_INTO", property = "isInsertableInto"),
            @Result(column = "COMMIT_ACTION", property = "commitAction"),
            @Result(column = "STORAGE_TYPE", property = "storageType"),
            @Result(column = "REMARKS", property = "remarks"),
            @Result(column = "LAST_MODIFICATION", property = "lastModification"),
            @Result(column = "TABLE_CLASS", property = "tableClass"),
            @Result(column = "ROW_COUNT_ESTIMATE", property = "rowCountEstimate"),
    })
    @Select("SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = UPPER(#{tableSchema}) and TABLE_NAME = UPPER(#{tableName});")
    InformationSchemaTables findTableInformation(String tableSchema, String tableName);

    /**
     * 根据表名查询库中该表的字段结构等信息
     *
     * @param tableSchema schema名
     * @param tableName   表名
     * @return 表的字段结构等信息
     */
    @Results({
            @Result(column = "TABLE_CATALOG", property = "tableCatalog"),
            @Result(column = "TABLE_SCHEMA", property = "tableSchema"),
            @Result(column = "TABLE_NAME", property = "tableName"),
            @Result(column = "COLUMN_NAME", property = "columnName"),
            @Result(column = "ORDINAL_POSITION", property = "ordinalPosition"),
            @Result(column = "COLUMN_DEFAULT", property = "columnDefault"),
            @Result(column = "IS_NULLABLE", property = "isNullable"),
            @Result(column = "DATA_TYPE", property = "dataType"),
            @Result(column = "CHARACTER_MAXIMUM_LENGTH", property = "characterMaximumLength"),
            @Result(column = "CHARACTER_OCTET_LENGTH", property = "characterOctetLength"),
            @Result(column = "NUMERIC_PRECISION", property = "numericPrecision"),
            @Result(column = "NUMERIC_PRECISION_RADIX", property = "numericPrecisionRadix"),
            @Result(column = "NUMERIC_SCALE", property = "numericScale"),
            @Result(column = "DATETIME_PRECISION", property = "datetimePrecision"),
            @Result(column = "INTERVAL_TYPE", property = "intervalType"),
            @Result(column = "INTERVAL_PRECISION", property = "intervalPrecision"),
            @Result(column = "CHARACTER_SET_CATALOG", property = "characterSetCatalog"),
            @Result(column = "CHARACTER_SET_SCHEMA", property = "characterSetSchema"),
            @Result(column = "CHARACTER_SET_NAME", property = "characterSetName"),
            @Result(column = "COLLATION_CATALOG", property = "collationCatalog"),
            @Result(column = "COLLATION_SCHEMA", property = "collationSchema"),
            @Result(column = "COLLATION_NAME", property = "collationName"),
            @Result(column = "DOMAIN_CATALOG", property = "domainCatalog"),
            @Result(column = "DOMAIN_SCHEMA", property = "domainSchema"),
            @Result(column = "DOMAIN_NAME", property = "domainName"),
            @Result(column = "MAXIMUM_CARDINALITY", property = "maximumCardinality"),
            @Result(column = "DTD_IDENTIFIER", property = "dtdIdentifier"),
            @Result(column = "IS_IDENTITY", property = "isIdentity"),
            @Result(column = "IDENTITY_GENERATION", property = "identityGeneration"),
            @Result(column = "IDENTITY_START", property = "identityStart"),
            @Result(column = "IDENTITY_INCREMENT", property = "identityIncrement"),
            @Result(column = "IDENTITY_MAXIMUM", property = "identityMaximum"),
            @Result(column = "IDENTITY_MINIMUM", property = "identityMinimum"),
            @Result(column = "IDENTITY_CYCLE", property = "identityCycle"),
            @Result(column = "IS_GENERATED", property = "isGenerated"),
            @Result(column = "GENERATION_EXPRESSION", property = "generationExpression"),
            @Result(column = "DECLARED_DATA_TYPE", property = "declaredDataType"),
            @Result(column = "DECLARED_NUMERIC_PRECISION", property = "declaredNumericPrecision"),
            @Result(column = "DECLARED_NUMERIC_SCALE", property = "declaredNumericScale"),
            @Result(column = "GEOMETRY_TYPE", property = "geometryType"),
            @Result(column = "GEOMETRY_SRID", property = "geometrySrid"),
            @Result(column = "IDENTITY_BASE", property = "identityBase"),
            @Result(column = "IDENTITY_CACHE", property = "identityCache"),
            @Result(column = "COLUMN_ON_UPDATE", property = "columnOnUpdate"),
            @Result(column = "IS_VISIBLE", property = "isVisible"),
            @Result(column = "DEFAULT_ON_NULL", property = "defaultOnNull"),
            @Result(column = "SELECTIVITY", property = "selectivity"),
            @Result(column = "REMARKS", property = "remarks"),
    })
    @Select("SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = UPPER(#{tableSchema}) and TABLE_NAME = UPPER(#{tableName});")
    List<InformationSchemaColumns> findColumnInformation(String tableSchema, String tableName);

    /**
     * 查询指定表的所有主键和索引信息
     *
     * @param tableSchema schema名
     * @param tableName   表名
     * @return 所有主键和索引信息
     */
    @Results({
            @Result(column = "INDEX_CATALOG", property = "indexCatalog"),
            @Result(column = "INDEX_SCHEMA", property = "indexSchema"),
            @Result(column = "INDEX_NAME", property = "indexName"),
            @Result(column = "TABLE_CATALOG", property = "tableCatalog"),
            @Result(column = "TABLE_SCHEMA", property = "tableSchema"),
            @Result(column = "TABLE_NAME", property = "tableName"),
            @Result(column = "COLUMN_NAME", property = "columnName"),
            @Result(column = "ORDINAL_POSITION", property = "ordinalPosition"),
            @Result(column = "ORDERING_SPECIFICATION", property = "orderingSpecification"),
            @Result(column = "NULL_ORDERING", property = "nullOrdering"),
            @Result(column = "IS_UNIQUE", property = "isUnique"),
            @Result(column = "REMARKS", property = "remarks"),
    })
    @Select("SELECT IC.*, I.REMARKS FROM INFORMATION_SCHEMA.INDEX_COLUMNS IC " +
            "LEFT JOIN INFORMATION_SCHEMA.INDEXES I ON I.INDEX_NAME = IC.INDEX_NAME AND IC.TABLE_SCHEMA = I.TABLE_SCHEMA AND IC.TABLE_NAME = I.TABLE_NAME " +
            "WHERE IC.TABLE_SCHEMA = UPPER(#{tableSchema}) AND IC.TABLE_NAME = UPPER(#{tableName}) AND I.INDEX_TYPE_NAME != 'PRIMARY KEY';")
    List<InformationSchemaIndexes> findIndexInformation(String tableSchema, String tableName);
}
