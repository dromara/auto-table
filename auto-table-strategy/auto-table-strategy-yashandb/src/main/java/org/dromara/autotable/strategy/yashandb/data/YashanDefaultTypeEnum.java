package org.dromara.autotable.strategy.yashandb.data;

import lombok.Getter;
import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;

/**
 * YashanDB DDL types used when converting MySQL-compatible model types.
 */
@Getter
public enum YashanDefaultTypeEnum implements DefaultTypeEnumInterface {
    VARCHAR("VARCHAR", 255, null),
    CHAR("CHAR", 1, null),
    TINYINT("TINYINT", null, null),
    SMALLINT("SMALLINT", null, null),
    INTEGER("INTEGER", null, null),
    BIGINT("BIGINT", null, null),
    NUMBER("NUMBER", 19, 4),
    DECIMAL("DECIMAL", 19, 4),
    FLOAT("FLOAT", null, null),
    DOUBLE("DOUBLE", null, null),
    DATE("DATE", null, null),
    TIME("TIME", null, null),
    TIMESTAMP("TIMESTAMP", null, null),
    BLOB("BLOB", null, null),
    CLOB("CLOB", null, null);

    private final String typeName;
    private final Integer defaultLength;
    private final Integer defaultDecimalLength;

    YashanDefaultTypeEnum(String typeName, Integer defaultLength, Integer defaultDecimalLength) {
        this.typeName = typeName;
        this.defaultLength = defaultLength;
        this.defaultDecimalLength = defaultDecimalLength;
    }
}
