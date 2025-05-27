package org.dromara.autotable.annotation.doris.emuns;

/**
 * 时间单位
 *
 * @author lizhian
 */
public enum DorisTimeUnit {

    none(""),
    year("year"),
    month("month"),
    week("week"),
    day("day"),
    hour("hour"),
    ;

    private final String value;

    DorisTimeUnit(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
