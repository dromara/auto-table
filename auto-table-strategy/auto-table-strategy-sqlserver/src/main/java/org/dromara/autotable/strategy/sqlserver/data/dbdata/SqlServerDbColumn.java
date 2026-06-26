package org.dromara.autotable.strategy.sqlserver.data.dbdata;

import lombok.Data;
import org.dromara.autotable.core.utils.DBHelper;
import org.dromara.autotable.core.utils.StringUtils;

/**
 * SQLServer 数据库，字段信息。
 *
 * <p>SQLServer 的 {@code sys.columns} 中：</p>
 * <ul>
 *   <li>{@code is_nullable} / {@code is_identity} 为 bit 类型，SQL 中已 CASE 成 'YES'/'NO' 字符串</li>
 *   <li>{@code max_length} 对 NVARCHAR/NCHAR 是字节数，字符数 = max_length / 2；对 VARCHAR/CHAR/BINARY 等是字节数=字符数；MAX 为 -1</li>
 *   <li>默认值表达式需通过 {@code OBJECT_DEFINITION(default_object_id)} 取，且外层带括号，如 {@code (0)}、{@code ('abc')}</li>
 * </ul>
 *
 * @author don
 */
@Data
public class SqlServerDbColumn {

    /**
     * 是否是主键（0/1，SQL 中 CAST 成 BIT）
     */
    @DBHelper.ColumnName("primary")
    private boolean primary;
    /**
     * 列注释
     */
    @DBHelper.ColumnName("description")
    private String description;
    /**
     * 列名
     */
    @DBHelper.ColumnName("columnName")
    private String columnName;
    /**
     * 数据类型名（来自 sys.types.name）
     */
    @DBHelper.ColumnName("dataType")
    private String dataType;
    /**
     * 最大长度（字节）。NVARCHAR/NCHAR 需除以 2 得字符数；-1 表示 MAX
     */
    @DBHelper.ColumnName("characterMaximumLength")
    private Integer characterMaximumLength;
    /**
     * 数值总位数
     */
    @DBHelper.ColumnName("numericPrecision")
    private Integer numericPrecision;
    /**
     * 数值小数位数
     */
    @DBHelper.ColumnName("numericScale")
    private Integer numericScale;
    /**
     * 是否允许为 null（"YES"/"NO"，对齐 core 比较逻辑）
     */
    @DBHelper.ColumnName("isNullable")
    private String isNullable;
    /**
     * 是否自增列（"YES"/"NO"）
     */
    @DBHelper.ColumnName("isIdentity")
    private String isIdentity;
    /**
     * 列默认值表达式（来自 OBJECT_DEFINITION(default_object_id)，外层带括号）
     */
    @DBHelper.ColumnName("columnDefault")
    private String columnDefault;
    /**
     * 默认值约束名（来自 sys.default_constraints.name，改默认值时需 drop 该约束）
     */
    @DBHelper.ColumnName("defaultConstraintName")
    private String defaultConstraintName;

    /**
     * 还原成可比较的类型字符串，与 {@code ColumnMetadata.getType().getDefaultFullType()} 对齐。
     * <p>类型名统一小写，例如：bigint、int、nvarchar(255)、decimal(19,4)、datetime2、bit。</p>
     * <p>注意：decimal/numeric 即使 scale=0 也输出 {@code (precision,0)}，以对齐 core
     * {@code getDefaultFullType()} 在 decimalLength=0 时仍输出 {@code ,0} 的行为。</p>
     *
     * @return 类型字符串
     */
    public String getDataTypeFormat() {
        if (this.dataType == null) {
            return null;
        }
        String type = this.dataType.toLowerCase();
        switch (type) {
            // 整数：无长度
            case "bigint":
            case "int":
            case "smallint":
            case "tinyint":
            case "bit":
                return type;
            // 精确小数：始终输出 (precision,scale) 以对齐 getDefaultFullType
            case "decimal":
            case "numeric":
                if (this.numericPrecision != null) {
                    int scale = this.numericScale == null ? 0 : this.numericScale;
                    return type + "(" + this.numericPrecision + "," + scale + ")";
                }
                return type;
            // 浮点：real 无精度参数；float 的精度存于 sys.columns.precision（1-53），输出以对齐实体 @ColumnType(length=n)
            case "real":
                return type;
            case "float":
                if (this.numericPrecision != null) {
                    return type + "(" + this.numericPrecision + ")";
                }
                return type;
            // Unicode 字符串：max_length 是字节数，字符数 = max_length / 2
            case "nvarchar":
            case "nchar":
                if (this.characterMaximumLength != null) {
                    if (this.characterMaximumLength == -1) {
                        return type;
                    }
                    return type + "(" + (this.characterMaximumLength / 2) + ")";
                }
                return type;
            // 非 Unicode 字符串/二进制：max_length 即字符数/字节数
            case "varchar":
            case "char":
            case "varbinary":
            case "binary":
                if (this.characterMaximumLength != null) {
                    if (this.characterMaximumLength == -1) {
                        return type;
                    }
                    return type + "(" + this.characterMaximumLength + ")";
                }
                return type;
            // 带可选小数秒精度的类型：精度存于 sys.columns.scale（0-7），输出以与实体 @ColumnType(length=n) 对齐
            // 例：datetime2(7)、time(0)、datetimeoffset(6)
            case "time":
            case "datetime2":
            case "datetimeoffset":
                if (this.numericScale != null) {
                    return type + "(" + this.numericScale + ")";
                }
                return type;
            // 日期时间：固定精度类型（date / 旧式 datetime / smalldatetime），实体不映射，保持裸类型
            case "date":
            case "datetime":
            case "smalldatetime":
                return type;
            // 大文本/大二进制
            case "text":
            case "ntext":
            case "image":
                return type;
            default:
                return type;
        }
    }

    /**
     * 去除默认值表达式外层的括号，便于与实体定义的默认值比较。
     * <p>SQLServer 的默认值表达式形如 {@code (0)}、{@code ('abc')}、{@code (getdate())}，
     * 去除外层括号后得到 {@code 0}、{@code 'abc'}、{@code getdate()}。</p>
     *
     * @return 去括号后的默认值，无默认值返回 null
     */
    public String getColumnDefaultWithoutParen() {
        if (!StringUtils.hasText(this.columnDefault)) {
            return null;
        }
        String value = this.columnDefault.trim();
        while (value.startsWith("(") && value.endsWith(")")) {
            value = value.substring(1, value.length() - 1).trim();
        }
        return value;
    }
}
