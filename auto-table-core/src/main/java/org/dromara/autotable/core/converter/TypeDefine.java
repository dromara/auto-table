package org.dromara.autotable.core.converter;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TypeDefine implements DefaultTypeEnumInterface {
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

    public static TypeDefine of(String typeName, Integer length, Integer decimalLength) {
        return new TypeDefine(typeName, length, decimalLength);
    }

    public static TypeDefine of(String typeName) {
        return TypeDefine.of(typeName, null, null);
    }

    public static TypeDefine of(String typeName, int length) {
        return TypeDefine.of(typeName, length, null);
    }

    public static TypeDefine ofNumber(int length, int decimalLength) {
        return TypeDefine.of("number", length, decimalLength);
    }
}
