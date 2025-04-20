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
public class BaseEntity<ID_TYPE extends Serializable, TIME_TYPE> {

    @AutoColumn(comment = "创建人", sort = -4)
    protected ID_TYPE createBy;
    @AutoColumn(comment = "最后更新人", sort = -3)
    protected ID_TYPE updateBy;
    @AutoColumn(comment = "创建时间", sort = -2)
    protected TIME_TYPE createTime;
    @AutoColumn(comment = "最后更新时间", sort = -1)
    protected TIME_TYPE updateTime;
}
