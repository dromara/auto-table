package org.dromara.autotable.strategy.h2.data.dbdata;

import lombok.Data;
import org.dromara.autotable.core.utils.DBHelper;

/**
 * 数据库表查询的列信息
 *
 * @author don
 */
@Data
public class InformationSchemaColumns {
    /**
     *
     */
    @DBHelper.ColumnName("TABLE_CATALOG")
    private String tableCatalog;
    /**
     *
     */
    @DBHelper.ColumnName("TABLE_SCHEMA")
    private String tableSchema;
    /**
     *
     */
    @DBHelper.ColumnName("TABLE_NAME")
    private String tableName;
    /**
     * 列名，纯大写
     */
    @DBHelper.ColumnName("COLUMN_NAME")
    private String columnName;
    /**
     *
     */
    @DBHelper.ColumnName("ORDINAL_POSITION")
    private Integer ordinalPosition;
    /**
     * 列的默认值。如果没有默认值，则为 NULL。
     */
    @DBHelper.ColumnName("COLUMN_DEFAULT")
    private String columnDefault;
    /**
     * 列是否允许 NULL 值。YES/NO
     */
    @DBHelper.ColumnName("IS_NULLABLE")
    private String isNullable;
    /**
     * 列的数据类型（例如 INTEGER、CHARACTER VARYING 等）。
     */
    @DBHelper.ColumnName("DATA_TYPE")
    private String dataType;
    /**
     * 字符型列的最大长度（仅适用于字符类型，如 VARCHAR）。如果不适用，则为 NULL。
     */
    @DBHelper.ColumnName("CHARACTER_MAXIMUM_LENGTH")
    private Long characterMaximumLength;
    /**
     * 字符型列的最大字节长度（即实际占用的存储空间）。如果不适用，则为 NULL。
     */
    @DBHelper.ColumnName("CHARACTER_OCTET_LENGTH")
    private Long characterOctetLength;
    /**
     * 数值型列的精度，即数值的总位数（仅适用于数值类型，如 DECIMAL）。如果不适用，则为 NULL。
     */
    @DBHelper.ColumnName("NUMERIC_PRECISION")
    private Integer numericPrecision;
    /**
     * 数值型列的基数（通常为 10）。如果不适用，则为 NULL。
     */
    @DBHelper.ColumnName("NUMERIC_PRECISION_RADIX")
    private Integer numericPrecisionRadix;
    /**
     * 数值型列的小数点后的位数。适用于 DECIMAL 类型。如果不适用，则为 NULL。
     */
    @DBHelper.ColumnName("NUMERIC_SCALE")
    private Integer numericScale;
    /**
     * 日期/时间型列的精度，表示日期/时间的精确度。如果不适用，则为 NULL。
     */
    @DBHelper.ColumnName("DATETIME_PRECISION")
    private Integer datetimePrecision;
    /**
     * 列使用的字符集名称（如 UTF-8）。仅适用于字符型列。如果不适用，则为 NULL。
     */
    @DBHelper.ColumnName("INTERVAL_TYPE")
    private String intervalType;
    /**
     * 列使用的排序规则名称（如 UTF8_GENERAL_CI）。仅适用于字符型列。如果不适用，则为 NULL。
     */
    @DBHelper.ColumnName("INTERVAL_PRECISION")
    private String intervalPrecision;
    /**
     *
     */
    @DBHelper.ColumnName("CHARACTER_SET_CATALOG")
    private String characterSetCatalog;
    /**
     *
     */
    @DBHelper.ColumnName("CHARACTER_SET_SCHEMA")
    private String characterSetSchema;
    /**
     *
     */
    @DBHelper.ColumnName("CHARACTER_SET_NAME")
    private String characterSetName;
    /**
     *
     */
    @DBHelper.ColumnName("COLLATION_CATALOG")
    private String collationCatalog;
    /**
     *
     */
    @DBHelper.ColumnName("COLLATION_SCHEMA")
    private String collationSchema;
    /**
     *
     */
    @DBHelper.ColumnName("COLLATION_NAME")
    private String collationName;
    /**
     *
     */
    @DBHelper.ColumnName("DOMAIN_CATALOG")
    private String domainCatalog;
    /**
     *
     */
    @DBHelper.ColumnName("DOMAIN_SCHEMA")
    private String domainSchema;
    /**
     *
     */
    @DBHelper.ColumnName("DOMAIN_NAME")
    private String domainName;
    /**
     *
     */
    @DBHelper.ColumnName("MAXIMUM_CARDINALITY")
    private String maximumCardinality;
    /**
     *
     */
    @DBHelper.ColumnName("DTD_IDENTIFIER")
    private String dtdIdentifier;
    /**
     * 是否主键
     */
    @DBHelper.ColumnName("IS_IDENTITY")
    private String isIdentity;
    /**
     * 是否自增：BY DEFAULT
     */
    @DBHelper.ColumnName("IDENTITY_GENERATION")
    private String identityGeneration;
    /**
     *
     */
    @DBHelper.ColumnName("IDENTITY_START")
    private String identityStart;
    /**
     *
     */
    @DBHelper.ColumnName("IDENTITY_INCREMENT")
    private String identityIncrement;
    /**
     *
     */
    @DBHelper.ColumnName("IDENTITY_MAXIMUM")
    private String identityMaximum;
    /**
     *
     */
    @DBHelper.ColumnName("IDENTITY_MINIMUM")
    private String identityMinimum;
    /**
     *
     */
    @DBHelper.ColumnName("IDENTITY_CYCLE")
    private String identityCycle;
    /**
     * 指示列是否是自动生成的（如自增列）。可能的值为：
     * •	NEVER: 列不是自动生成的
     * •	ALWAYS: 列总是自动生成的
     */
    @DBHelper.ColumnName("IS_GENERATED")
    private String isGenerated;
    /**
     *
     */
    @DBHelper.ColumnName("GENERATION_EXPRESSION")
    private String generationExpression;
    /**
     *
     */
    @DBHelper.ColumnName("DECLARED_DATA_TYPE")
    private String declaredDataType;
    /**
     *
     */
    @DBHelper.ColumnName("DECLARED_NUMERIC_PRECISION")
    private String declaredNumericPrecision;
    /**
     *
     */
    @DBHelper.ColumnName("DECLARED_NUMERIC_SCALE")
    private String declaredNumericScale;
    /**
     *
     */
    @DBHelper.ColumnName("GEOMETRY_TYPE")
    private String geometryType;
    /**
     *
     */
    @DBHelper.ColumnName("GEOMETRY_SRID")
    private String geometrySrid;
    /**
     *
     */
    @DBHelper.ColumnName("IDENTITY_BASE")
    private String identityBase;
    /**
     *
     */
    @DBHelper.ColumnName("IDENTITY_CACHE")
    private String identityCache;
    /**
     *
     */
    @DBHelper.ColumnName("COLUMN_ON_UPDATE")
    private String columnOnUpdate;
    /**
     *
     */
    @DBHelper.ColumnName("IS_VISIBLE")
    private String isVisible;
    /**
     *
     */
    @DBHelper.ColumnName("DEFAULT_ON_NULL")
    private String defaultOnNull;
    /**
     *
     */
    @DBHelper.ColumnName("SELECTIVITY")
    private String selectivity;
    /**
     * 字段注释
     */
    @DBHelper.ColumnName("REMARKS")
    private String remarks;

    /**
     * 是否自增
     */
    public boolean autoIncrement() {
        return "BY DEFAULT".equalsIgnoreCase(identityGeneration);
    }

    /**
     * 是否是主键
     */
    public boolean primaryKey() {
        return "YES".equalsIgnoreCase(isIdentity);
    }
}
