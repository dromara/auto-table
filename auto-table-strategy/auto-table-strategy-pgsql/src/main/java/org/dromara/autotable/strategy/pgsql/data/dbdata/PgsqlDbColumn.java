package org.dromara.autotable.strategy.pgsql.data.dbdata;

import org.dromara.autotable.core.utils.StringUtils;
import lombok.Data;
import org.dromara.autotable.core.utils.DBHelper;

/**
 * pgsql数据库，字段信息
 */
@Data
public class PgsqlDbColumn {

    /**
     * 是否是主键
     */
    @DBHelper.ColumnName("primary")
    private boolean primary;
    /**
     * 列注释
     */
    @DBHelper.ColumnName("description")
    private String description;
    /**
     * 列所属的数据库名称。
     */
    @DBHelper.ColumnName("table_catalog")
    private String tableCatalog;
    /**
     * 列所属的模式（命名空间）名称。
     */
    @DBHelper.ColumnName("table_schema")
    private String tableSchema;
    /**
     * 列所属的表名称。
     */
    @DBHelper.ColumnName("table_name")
    private String tableName;
    /**
     * 列名
     */
    @DBHelper.ColumnName("column_name")
    private String columnName;
    /**
     * 列顺序
     */
    @DBHelper.ColumnName("ordinal_position")
    private String ordinalPosition;
    /**
     * 列默认值
     */
    @DBHelper.ColumnName("column_default")
    private String columnDefault;
    /**
     * 是否允许为null
     */
    @DBHelper.ColumnName("is_nullable")
    private String isNullable;
    /**
     * 数据类型
     */
    @DBHelper.ColumnName("data_type")
    private String dataType;
    /**
     * 如果数据类型是字符类型，表示字符的最大长度
     */
    @DBHelper.ColumnName("character_maximum_length")
    private String characterMaximumLength;
    /**
     * 如果数据类型是字符类型，表示以字节为单位的最大长度。
     */
    @DBHelper.ColumnName("character_octet_length")
    private String characterOctetLength;
    /**
     * 如果数据类型是数值类型，表示数值的总位数。
     */
    @DBHelper.ColumnName("numeric_precision")
    private String numericPrecision;
    /**
     * 如果数据类型是数值类型，表示数值的基数（通常为 2 或 10）
     */
    @DBHelper.ColumnName("numeric_precision_radix")
    private String numericPrecisionRadix;
    /**
     * 如果数据类型是数值类型，表示小数部分的位数
     */
    @DBHelper.ColumnName("numeric_scale")
    private String numericScale;
    /**
     * 如果数据类型是日期/时间类型，表示日期或时间的小数部分的位数
     */
    @DBHelper.ColumnName("datetime_precision")
    private String datetimePrecision;
    @DBHelper.ColumnName("interval_type")
    private String intervalType;
    @DBHelper.ColumnName("interval_precision")
    private String intervalPrecision;
    @DBHelper.ColumnName("character_set_catalog")
    private String characterSetCatalog;
    @DBHelper.ColumnName("character_set_schema")
    private String characterSetSchema;
    @DBHelper.ColumnName("character_set_name")
    private String characterSetName;
    @DBHelper.ColumnName("collation_catalog")
    private String collationCatalog;
    @DBHelper.ColumnName("collation_schema")
    private String collationSchema;
    @DBHelper.ColumnName("collation_name")
    private String collationName;
    @DBHelper.ColumnName("domain_catalog")
    private String domainCatalog;
    @DBHelper.ColumnName("domain_schema")
    private String domainSchema;
    @DBHelper.ColumnName("domain_name")
    private String domainName;
    @DBHelper.ColumnName("udt_catalog")
    private String udtCatalog;
    @DBHelper.ColumnName("udt_schema")
    private String udtSchema;
    @DBHelper.ColumnName("udt_name")
    private String udtName;
    @DBHelper.ColumnName("scope_catalog")
    private String scopeCatalog;
    @DBHelper.ColumnName("scope_schema")
    private String scopeSchema;
    @DBHelper.ColumnName("scope_name")
    private String scopeName;
    @DBHelper.ColumnName("maximum_cardinality")
    private String maximumCardinality;
    @DBHelper.ColumnName("dtd_identifier")
    private String dtdIdentifier;
    @DBHelper.ColumnName("is_self_referencing")
    private String isSelfReferencing;
    @DBHelper.ColumnName("is_identity")
    private String isIdentity;
    @DBHelper.ColumnName("identity_generation")
    private String identityGeneration;
    @DBHelper.ColumnName("identity_start")
    private String identityStart;
    @DBHelper.ColumnName("identity_increment")
    private String identityIncrement;
    @DBHelper.ColumnName("identity_maximum")
    private String identityMaximum;
    @DBHelper.ColumnName("identity_minimum")
    private String identityMinimum;
    @DBHelper.ColumnName("identity_cycle")
    private String identityCycle;
    @DBHelper.ColumnName("is_generated")
    private String isGenerated;
    @DBHelper.ColumnName("generation_expression")
    private String generationExpression;
    @DBHelper.ColumnName("is_updatable")
    private String isUpdatable;

    public String getDataTypeFormat() {
        switch (this.udtName) {
            // 数字
            case "int2":
            case "int4":
            case "int8":
                return this.udtName + "(" + this.numericPrecision + ")";
            case "numeric":
                return this.udtName + "(" + this.numericPrecision + "," + this.numericScale + ")";
            // 字符串
            case "varchar":
                if(StringUtils.hasText(this.characterMaximumLength)) {
                    return this.udtName + "(" + this.characterMaximumLength + ")";
                }
                return this.udtName;
            case "bpchar":
                return "char(" + this.characterMaximumLength + ")";
            // 其他的没有长度
            default:
                return this.udtName;
        }
    }
}
