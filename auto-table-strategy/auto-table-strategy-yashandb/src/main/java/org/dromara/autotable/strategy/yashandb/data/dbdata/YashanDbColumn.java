package org.dromara.autotable.strategy.yashandb.data.dbdata;

import lombok.Data;

/**
 * Column metadata read from YashanDB catalog views.
 */
@Data
public class YashanDbColumn {
    private String name;
    private String type;
    private Integer charLength;
    private Integer precision;
    private Integer scale;
    private boolean nullable;
    private String defaultValue;
    private String comment;
    private boolean autoIncrement;
}
