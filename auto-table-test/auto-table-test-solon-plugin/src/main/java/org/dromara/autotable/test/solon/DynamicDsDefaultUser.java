package org.dromara.autotable.test.solon;

import lombok.Data;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.ColumnComment;
import org.dromara.autotable.annotation.PrimaryKey;
import org.noear.solon.data.dynamicds.DynamicDs;

@Data
@DynamicDs
@AutoTable("sys_user")
public class DynamicDsDefaultUser {

    @PrimaryKey(autoIncrement = true)
    @ColumnComment("用户id")
    private Long id;

    @ColumnComment("年龄")
    private Integer age;

    @ColumnComment("性别")
    private String gender;

}
