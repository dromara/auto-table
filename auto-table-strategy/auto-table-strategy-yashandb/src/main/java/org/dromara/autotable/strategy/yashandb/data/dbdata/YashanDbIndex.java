package org.dromara.autotable.strategy.yashandb.data.dbdata;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Index metadata read from YashanDB catalog views.
 */
@Data
public class YashanDbIndex {
    private String name;
    private boolean unique;
    private List<String> columns = new ArrayList<>();
    private List<String> sorts = new ArrayList<>();
}
