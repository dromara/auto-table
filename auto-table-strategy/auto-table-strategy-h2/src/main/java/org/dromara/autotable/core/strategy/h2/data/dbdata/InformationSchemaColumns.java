package org.dromara.autotable.core.strategy.h2.data.dbdata;

import lombok.Data;

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
    private String tableCatalog;
    /**
     *
     */
    private String tableSchema;
    /**
     *
     */
    private String tableName;
    /**
     * 列名，纯大写
     */
    private String columnName;
    /**
     *
     */
    private Integer ordinalPosition;
    /**
     * 列的默认值。如果没有默认值，则为 NULL。
     */
    private String columnDefault;
    /**
     * 列是否允许 NULL 值。YES/NO
     */
    private String isNullable;
    /**
     * 列的数据类型（例如 INTEGER、CHARACTER VARYING 等）。
     */
    private String dataType;
    /**
     * 字符型列的最大长度（仅适用于字符类型，如 VARCHAR）。如果不适用，则为 NULL。
     */
    private Long characterMaximumLength;
    /**
     * 字符型列的最大字节长度（即实际占用的存储空间）。如果不适用，则为 NULL。
     */
    private Long characterOctetLength;
    /**
     * 数值型列的精度，即数值的总位数（仅适用于数值类型，如 DECIMAL）。如果不适用，则为 NULL。
     */
    private Integer numericPrecision;
    /**
     * 数值型列的基数（通常为 10）。如果不适用，则为 NULL。
     */
    private Integer numericPrecisionRadix;
    /**
     * 数值型列的小数点后的位数。适用于 DECIMAL 类型。如果不适用，则为 NULL。
     */
    private Integer numericScale;
    /**
     * 日期/时间型列的精度，表示日期/时间的精确度。如果不适用，则为 NULL。
     */
    private Integer datetimePrecision;
    /**
     * 列使用的字符集名称（如 UTF-8）。仅适用于字符型列。如果不适用，则为 NULL。
     */
    private String intervalType;
    /**
     * 列使用的排序规则名称（如 UTF8_GENERAL_CI）。仅适用于字符型列。如果不适用，则为 NULL。
     */
    private String intervalPrecision;
    /**
     *
     */
    private String characterSetCatalog;
    /**
     *
     */
    private String characterSetSchema;
    /**
     *
     */
    private String characterSetName;
    /**
     *
     */
    private String collationCatalog;
    /**
     *
     */
    private String collationSchema;
    /**
     *
     */
    private String collationName;
    /**
     *
     */
    private String domainCatalog;
    /**
     *
     */
    private String domainSchema;
    /**
     *
     */
    private String domainName;
    /**
     *
     */
    private String maximumCardinality;
    /**
     *
     */
    private String dtdIdentifier;
    /**
     * 是否主键
     */
    private String isIdentity;
    /**
     * 是否自增：BY DEFAULT
     */
    private String identityGeneration;
    /**
     *
     */
    private String identityStart;
    /**
     *
     */
    private String identityIncrement;
    /**
     *
     */
    private String identityMaximum;
    /**
     *
     */
    private String identityMinimum;
    /**
     *
     */
    private String identityCycle;
    /**
     * 指示列是否是自动生成的（如自增列）。可能的值为：
     * •	NEVER: 列不是自动生成的
     * •	ALWAYS: 列总是自动生成的
     */
    private String isGenerated;
    /**
     *
     */
    private String generationExpression;
    /**
     *
     */
    private String declaredDataType;
    /**
     *
     */
    private String declaredNumericPrecision;
    /**
     *
     */
    private String declaredNumericScale;
    /**
     *
     */
    private String geometryType;
    /**
     *
     */
    private String geometrySrid;
    /**
     *
     */
    private String identityBase;
    /**
     *
     */
    private String identityCache;
    /**
     *
     */
    private String columnOnUpdate;
    /**
     *
     */
    private String isVisible;
    /**
     *
     */
    private String defaultOnNull;
    /**
     *
     */
    private String selectivity;
    /**
     * 字段注释
     */
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
