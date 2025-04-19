package org.dromara.autotable.core.strategy.sqlite.data;

import org.dromara.autotable.core.converter.DefaultTypeEnumInterface;
import lombok.Getter;

/**
 * @author don
 */
@Getter
public enum SqliteDefaultTypeEnum implements DefaultTypeEnumInterface {

    INTEGER("integer", 16, null),
    REAL("real", 10, 2),
    TEXT("text", null, null),
    BLOB("blob", null, null);

    /**
     * 类型名称
     */
    private final String typeName;
    /**
     * 默认类型长度
     */
    private final Integer defaultLength;
    /**
     * 默认小数点后长度
     */
    private final Integer defaultDecimalLength;

    SqliteDefaultTypeEnum(String typeName, Integer defaultLength, Integer defaultDecimalLength) {
        this.typeName = typeName;
        this.defaultLength = defaultLength;
        this.defaultDecimalLength = defaultDecimalLength;
    }
}
