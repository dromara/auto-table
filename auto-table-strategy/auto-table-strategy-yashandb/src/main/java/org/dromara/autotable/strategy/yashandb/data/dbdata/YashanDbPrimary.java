package org.dromara.autotable.strategy.yashandb.data.dbdata;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Primary-key metadata read from YashanDB catalog views.
 */
@Data
public class YashanDbPrimary {
    private String name;
    private List<String> columns = new ArrayList<>();
}
