package org.dromara.autotable.test.solon;

import lombok.Data;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.ColumnComment;
import org.dromara.autotable.annotation.ColumnType;
import org.dromara.autotable.annotation.PrimaryKey;
import org.dromara.autotable.annotation.mysql.MysqlTypeConstant;
import org.noear.solon.data.dynamicds.DynamicDs;

@Data
@DynamicDs("db_user_w")
@AutoTable("sys_user")
public class DynamicDsUser {

    @PrimaryKey(autoIncrement = true)
    @ColumnComment("用户id")
    private Long id;

    @ColumnComment("电话")
    private String phone;


    @ColumnComment("备注")
    @ColumnType(MysqlTypeConstant.TEXT)
    private String mark;
}
