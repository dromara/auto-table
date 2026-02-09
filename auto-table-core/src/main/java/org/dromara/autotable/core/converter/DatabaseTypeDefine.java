package org.dromara.autotable.core.converter;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * 数据库类型定义
 */
@Data
@RequiredArgsConstructor
public class DatabaseTypeDefine implements DefaultTypeEnumInterface {

    private final String typeName;
    private final Integer length;
    private final Integer decimalLength;

    @Override
    public Integer getDefaultLength() {
        return length;
    }

    @Override
    public Integer getDefaultDecimalLength() {
        return decimalLength;
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    public static DatabaseTypeDefine of(String typeName, Integer length, Integer decimalLength) {
        return new DatabaseTypeDefine(typeName, length, decimalLength);
    }

    public static DatabaseTypeDefine of(String typeName) {
        return DatabaseTypeDefine.of(typeName, null, null);
    }

    public static DatabaseTypeDefine of(String typeName, int length) {
        return DatabaseTypeDefine.of(typeName, length, null);
    }

}
