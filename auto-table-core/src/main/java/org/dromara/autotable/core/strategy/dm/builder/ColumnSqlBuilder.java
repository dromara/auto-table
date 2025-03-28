package org.dromara.autotable.core.strategy.dm.builder;

import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.core.converter.DatabaseTypeAndLength;
import org.dromara.autotable.core.strategy.ColumnMetadata;
import org.dromara.autotable.core.strategy.dm.data.DmDefaultTypeEnum;
import org.dromara.autotable.core.utils.StringConnectHelper;
import org.dromara.autotable.core.utils.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 达梦列SQL构建器
 */
public class ColumnSqlBuilder {
    // 达梦保留字集合（部分示例）
    public static final Set<String> RESERVED_WORDS = new HashSet<>(Arrays.asList(
            "ACCESS", "ELSE", "MODIFY", "START", "ADD", "EXCLUSIVE", "NOAUDIT", "SELECT",
            "ALL", "EXISTS", "NOCOMPRESS", "SESSION", "ALTER", "FILE", "NOT", "SET", "AND", "FLOAT",
            "NOTFOUND", "SHARE", "ANY", "FOR", "NOWAIT", "SIZE", "ARRAYLEN", "FROM", "NULL", "SMALLINT",
            "AS", "GRANT", "NUMBER", "SQLBUF", "ASC", "GROUP", "OF", "SUCCESSFUL", "AUDIT", "HAVING",
            "OFFLINE", "SYNONYM", "BETWEEN", "IDENTIFIED", "ON", "SYSDATE", "BY", "IMMEDIATE", "ONLINE",
            "TABLE", "CHAR", "IN", "OPTION", "THEN", "CHECK", "INCREMENT", "OR", "TO", "CLUSTER", "INDEX",
            "ORDER", "TRIGGER", "COLUMN", "INITIAL", "PCTFREE", "UID", "COMMENT", "INSERT", "PRIOR",
            "UNION", "COMPRESS", "INTEGER", "PRIVILEGES", "UNIQUE", "CONNECT", "INTERSECT", "PUBLIC",
            "UPDATE", "CREATE", "INTO", "RAW", "USER", "CURRENT", "IS", "RENAME", "VALIDATE", "DATE", "LEVEL",
            "RESOURCE", "VALUES", "DECIMAL", "LIKE", "REVOKE", "VARCHAR", "DEFAULT", "LOCK", "ROW", "VARCHAR2",
            "DELETE", "LONG", "ROWID", "VIEW", "DESC", "MAXEXTENTS", "ROWLABEL", "WHENEVER", "DISTINCT", "MINUS",
            "ROWNUM", "WHERE", "DROP", "MODE", "ROWS", "WITH", "ADMIN", "CURSOR", "FOUND", "MOUNT", "AFTER", "CYCLE",
            "FUNCTION", "NEXT", "ALLOCATE", "DATABASE", "GO", "NEW", "ANALYZE", "DATAFILE", "GOTO", "NOARCHIVELOG",
            "ARCHIVE", "DBA", "GROUPS", "NOCACHE", "ARCHIVELOG", "DEC", "INCLUDING", "NOCYCLE", "AUTHORIZATION",
            "DECLARE", "INDICATOR", "NOMAXVALUE", "AVG", "DISABLE", "INITRANS", "NOMINVALUE", "BACKUP", "DISMOUNT",
            "INSTANCE", "NONE", "BEGIN", "DOUBLE", "INT", "NOORDER", "BECOME", "DUMP", "KEY", "NORESETLOGS", "BEFORE",
            "EACH", "LANGUAGE", "NORMAL", "BLOCK", "ENABLE", "LAYER", "NOSORT", "BODY", "END", "LINK", "NUMERIC", "CACHE",
            "ESCAPE", "LISTS", "OFF", "CANCEL", "EVENTS", "LOGFILE", "OLD", "CASCADE", "EXCEPT", "MANAGE", "ONLY", "CHANGE",
            "EXCEPTIONS", "MANUAL", "OPEN", "CHARACTER", "EXEC", "MAX", "OPTIMAL", "CHECKPOINT", "EXPLAIN", "MAXDATAFILES",
            "OWN", "CLOSE", "EXECUTE", "MAXINSTANCES", "PACKAGE", "COBOL", "EXTENT", "MAXLOGFILES", "PARALLEL", "COMMIT",
            "EXTERNALLY", "MAXLOGHISTORY", "PCTINCREASE", "COMPILE", "FETCH", "MAXLOGMEMBERS", "PCTUSED", "CONSTRAINT",
            "FLUSH", "MAXTRANS", "PLAN", "CONSTRAINTS", "FREELIST", "MAXVALUE", "PLI", "CONTENTS", "FREELISTS", "MIN",
            "PRECISION", "CONTINUE", "FORCE", "MINEXTENTS", "PRIMARY", "CONTROLFILE", "FOREIGN", "MINVALUE", "PRIVATE",
            "COUNT", "FORTRAN", "MODULE", "PROCEDURE", "PROFILE", "SAVEPOINT", "SQLSTATE", "TRACING", "QUOTA", "SCHEMA",
            "STATEMENT_ID", "TRANSACTION", "READ", "SCN", "STATISTICS", "TRIGGERS", "REAL", "SECTION", "STOP", "TRUNCATE",
            "RECOVER", "SEGMENT", "STORAGE", "UNDER", "REFERENCES", "SEQUENCE", "SUM", "UNLIMITED", "REFERENCING", "SHARED",
            "SWITCH", "UNTIL", "RESETLOGS", "SNAPSHOT", "SYSTEM", "USE", "RESTRICTED", "SOME", "TABLES", "USING", "REUSE",
            "SORT", "TABLESPACE", "WHEN", "ROLE", "SQL", "TEMPORARY", "WRITE", "ROLES", "SQLCODE", "THREAD", "WORK", "ROLLBACK",
            "SQLERROR", "TIME", "ABORT", "BETWEEN", "CRASH", "DIGITS", "ACCEPT", "BINARY_INTEGER", "CREATE", "DISPOSE", "ACCESS",
            "BODY", "CURRENT", "DISTINCT", "ADD", "BOOLEAN", "CURRVAL", "DO", "ALL", "BY", "CURSOR", "DROP", "ALTER", "CASE", "DATABASE",
            "ELSE", "AND", "CHAR", "DATA_BASE", "ELSIF", "ANY", "CHAR_BASE", "DATE", "END", "ARRAY", "CHECK", "DBA", "ENTRY", "ARRAYLEN",
            "CLOSE", "DEBUGOFF", "EXCEPTION", "AS", "CLUSTER", "DEBUGON", "EXCEPTION_INIT", "ASC", "CLUSTERS", "DECLARE", "EXISTS",
            "ASSERT", "COLAUTH", "DECIMAL", "EXIT", "ASSIGN", "COLUMNS", "DEFAULT", "FALSE", "AT", "COMMIT", "DEFINITION", "FETCH",
            "AUTHORIZATION", "COMPRESS", "DELAY", "FLOAT", "AVG", "CONNECT", "DELETE", "FOR", "BASE_TABLE", "CONSTANT", "DELTA", "FORM",
            "BEGIN", "COUNT", "DESC", "FROM", "FUNCTION", "NEW", "RELEASE", "SUM", "GENERIC", "NEXTVAL", "REMR", "TABAUTH",
            "GOTO", "NOCOMPRESS", "RENAME", "TABLE", "GRANT", "NOT", "RESOURCE", "TABLES", "GROUP", "NULL", "RETURN", "TASK", "HAVING",
            "NUMBER", "REVERSE", "TERMINATE", "IDENTIFIED", "NUMBER_BASE", "REVOKE", "THEN", "IF", "OF", "ROLLBACK", "TO", "IN", "ON",
            "ROWID", "TRUE", "INDEX", "OPEN", "ROWLABEL", "TYPE", "INDEXES", "OPTION", "ROWNUM", "UNION", "INDICATOR", "OR", "ROWTYPE",
            "UNIQUE", "INSERT", "ORDER", "RUN", "UPDATE", "INTEGER", "OTHERS", "SAVEPOINT", "USE", "INTERSECT", "OUT", "SCHEMA", "VALUES",
            "INTO", "PACKAGE", "SELECT", "VARCHAR", "IS", "PARTITION", "SEPARATE", "VARCHAR2", "LEVEL", "PCTFREE", "SET", "VARIANCE",
            "LIKE", "POSITIVE", "SIZE", "VIEW", "LIMITED", "PRAGMA", "SMALLINT", "VIEWS", "LOOP", "PRIOR", "SPACE", "WHEN", "MAX", "PRIVATE",
            "SQL", "WHERE", "MIN", "PROCEDURE", "SQLCODE", "WHILE", "MINUS", "PUBLIC", "SQLERRM", "WITH", "MLSLABEL", "RAISE", "START",
            "WORK", "MOD", "RANGE", "STATEMENT", "XOR", "MODE", "REAL", "STDDEV", "NATURAL", "RECORD", "SUBTYPE", "GEN", "KP", "L",
            "NA", "NC", "ND", "NL", "NM", "NR", "NS", "NT", "NZ", "TTC", "UPI", "O", "S", "XA"
    ));
    /**
     * 生成达梦字段定义SQL
     */
    public static String buildSql(ColumnMetadata columnMetadata) {
        StringConnectHelper sql = StringConnectHelper.newInstance("{columnName} {type} {null} {default} " +
                        "{autoIncrement}")
                .replace("{columnName}", wrapColumnName(columnMetadata.getName()))
                .replace("{type}", buildTypeDefinition(columnMetadata.getType()))
                .replace("{null}", columnMetadata.isNotNull() ? "NOT NULL" : "")
                .replace("{default}", buildDefaultValue(columnMetadata))
                .replace("{autoIncrement}", buildAutoIncrement(columnMetadata));

        return sql.toString().replaceAll("\\s+", " ").trim();
    }

    /**
     * 构建类型定义
     */
    private static String buildTypeDefinition(DatabaseTypeAndLength type) {
        String typeName = type.getType().toUpperCase();
        Integer length = type.getLength();
        Integer decimal = type.getDecimalLength();

        // 优先使用枚举中定义的默认值
        DmDefaultTypeEnum typeEnum = Arrays.stream(DmDefaultTypeEnum.values())
                .filter(e -> e.getTypeName().equalsIgnoreCase(typeName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid type: " + typeName));

        if ("NUMBER".equals(typeName)) {
            return handleNumberType(typeEnum, length, decimal);
        } else if ("VARCHAR2".equals(typeName)) {
            int actualLength = (length != null) ? Math.min(length, 8188) : 50;
            return String.format("VARCHAR2(%d)", actualLength);
        } else if ("CHAR".equals(typeName)) {
            return (length != null && length > 1) ? "CHAR(" + length + ")" : "CHAR";
        } else {
            return buildDefaultType(typeEnum, length, decimal);
        }
    }

    private static String handleNumberType(DmDefaultTypeEnum typeEnum,
                                           Integer length, Integer decimal) {
        int precision = length != null ? length : typeEnum.getDefaultLength();
        int scale = decimal != null ? decimal : typeEnum.getDefaultDecimalLength();
        return DmDefaultTypeEnum.convertNumberType(precision, scale);
    }

    private static String buildDefaultType(DmDefaultTypeEnum typeEnum,
                                           Integer length, Integer decimal) {
        if (typeEnum == DmDefaultTypeEnum.FLOAT || typeEnum == DmDefaultTypeEnum.DOUBLE) {
            int actualLength = (length != null) ? length : typeEnum.getDefaultLength();
            return String.format("%s(%d)", typeEnum.getTypeName(), actualLength);
        } else if (typeEnum == DmDefaultTypeEnum.DECIMAL) {
            int actualLength = (length != null) ? length : typeEnum.getDefaultLength();
            int actualDecimal = (decimal != null) ? decimal : typeEnum.getDefaultDecimalLength();
            return String.format("%s(%d,%d)", typeEnum.getTypeName(), actualLength, actualDecimal);
        } else {
            return typeEnum.getTypeName();
        }
    }


    /**
     * 构建默认值子句
     */
    private static String buildDefaultValue(ColumnMetadata columnMetadata) {
        if (columnMetadata.isAutoIncrement()) {
            return "";
        }

        DefaultValueEnum defaultValueType = columnMetadata.getDefaultValueType();
        String defaultValue = columnMetadata.getDefaultValue();

        // 根据字段类型判断是否需要引号
        String typeName = columnMetadata.getType().getType().toUpperCase();
        boolean isNumberType = isNumberType(typeName);
        boolean isBooleanType = "TINYINT".equals(typeName);

        if (defaultValueType == DefaultValueEnum.NULL) {
            return "DEFAULT NULL";
        }

        if (defaultValueType == DefaultValueEnum.EMPTY_STRING) {
            return isNumberType ? "" : "DEFAULT ''";
        }

        if (DefaultValueEnum.isCustom(defaultValueType) && StringUtils.hasText(defaultValue)) {
            if (isFunctionDefault(defaultValue)) {
                return "DEFAULT " + defaultValue;
            }

            // 处理布尔类型
            if (isBooleanType) {
                return "DEFAULT " + convertBooleanValue(defaultValue);
            }

            // 处理数值类型
            if (isNumberType) {
                return "DEFAULT " + defaultValue;
            }

            // 字符串类型需要转义单引号
            return "DEFAULT '" + defaultValue.replace("'", "''") + "'";
        }

        return "";
    }

    // 判断是否为数值类型
    private static boolean isNumberType(String typeName) {
        return typeName.matches("BIGINT|INT|INTEGER|SMALLINT|TINYINT|NUMBER|DECIMAL|FLOAT|DOUBLE");
    }

    // 转换布尔值
    private static String convertBooleanValue(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return "1";
        }
        if ("false".equalsIgnoreCase(value)) {
            return "0";
        }
        // 非标准布尔值保持原样
        return value;
    }


    /**
     * 构建自增子句
     */
    private static String buildAutoIncrement(ColumnMetadata columnMetadata) {
        if (!columnMetadata.isAutoIncrement()) {
            return "";
        }

        // 达梦的SERIAL类型已包含自增特性
        return "SERIAL".equalsIgnoreCase(columnMetadata.getType().getType()) ?
                "" : "IDENTITY(1,1)";
    }


    /**
     * 处理保留字列名
     */
    private static String wrapColumnName(String columnName) {
        // 统一转为大写判断（达梦保留字不区分大小写）
        String upperName = columnName.toUpperCase();

        // 仅对保留字添加双引号
        if (RESERVED_WORDS.contains(upperName)) {
            return "\"" + columnName + "\"";
        }

        // 普通列名保持原样
        return columnName;
    }

    /**
     * 判断是否为函数型默认值
     */
    private static boolean isFunctionDefault(String value) {
        return value.toUpperCase().matches("^(SYSDATE|CURRENT_TIMESTAMP|NEXTVAL\\()");
    }
}
