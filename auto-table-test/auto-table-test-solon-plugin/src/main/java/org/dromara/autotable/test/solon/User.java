package org.dromara.autotable.test.solon;

import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.ColumnComment;
import org.dromara.autotable.annotation.ColumnType;
import org.dromara.autotable.annotation.PrimaryKey;
import org.dromara.autotable.annotation.mysql.MysqlTypeConstant;
import lombok.Data;

@Data
@AutoTable("sys_user")
public class User {

    @PrimaryKey(autoIncrement = true)
    @ColumnComment("用户id")
    private Long id;

    @ColumnComment("电话")
    private String phone;

    @ColumnComment("年龄")
    private Integer age;
//
//    @ColumnComment("性别")
//    private String gender;

    @ColumnComment("备注")
    @ColumnType(MysqlTypeConstant.TEXT)
    private String mark;
}
