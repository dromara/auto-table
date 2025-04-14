package org.dromara.autotable.test.core;

import lombok.Getter;
import lombok.Setter;
import org.dromara.autotable.annotation.AutoColumn;

import java.io.Serializable;

/**
 * @author don
 */
@Getter
@Setter
public class BaseIDEntity<ID_TYPE extends Serializable, TIME_TYPE> extends BaseEntity<ID_TYPE, TIME_TYPE> {

    @AutoColumn(comment = "主键", sort = 1)
    protected ID_TYPE id;
}
