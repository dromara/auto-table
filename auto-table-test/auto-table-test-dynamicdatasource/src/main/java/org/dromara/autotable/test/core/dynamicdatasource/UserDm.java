package org.dromara.autotable.test.core.dynamicdatasource;

import lombok.Data;
import org.dromara.autotable.annotation.*;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;

@Data
@Ds("dm")
@AutoTable("sys_user")
public class UserDm {

    @PrimaryKey(autoIncrement = true)
    @ColumnComment("用户id")
    private Long id;

    @ColumnComment("姓名")
    @ColumnType(value = "text", length = 100)
    private String name;

    @ColumnComment("备注")
    @ColumnType("text")
    @ColumnDefault(type = DefaultValueEnum.NULL)
    private String mark;
}
