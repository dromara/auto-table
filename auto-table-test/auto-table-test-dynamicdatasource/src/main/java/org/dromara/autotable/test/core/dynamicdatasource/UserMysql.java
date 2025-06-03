package org.dromara.autotable.test.core.dynamicdatasource;

import lombok.Data;
import org.dromara.autotable.annotation.*;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.annotation.mysql.MysqlColumnCharset;
import org.dromara.autotable.annotation.mysql.MysqlTypeConstant;

/**
 * @author don
 */
@Data
@AutoTable(value = "sys_user", comment = "用户表")
public class UserMysql {

    @PrimaryKey(autoIncrement = true)
    @ColumnComment("用户id")
    private Long id;

    @ColumnComment("姓名")
    private String name;

    @ColumnComment("备注")
    @ColumnType(MysqlTypeConstant.TEXT)
    @ColumnDefault(type = DefaultValueEnum.NULL)
    @MysqlColumnCharset(value = "utf8mb4", collate = "utf8mb4_0900_ai_ci")
    private String mark;
}
