package org.dromara.autotable.test.core.entity.pgsql;

import lombok.Data;
import org.dromara.autotable.annotation.AutoTable;

/**
 * @author don
 */
@Data
@AutoTable
public class TestNoColumnComment {

    private String name;
}
