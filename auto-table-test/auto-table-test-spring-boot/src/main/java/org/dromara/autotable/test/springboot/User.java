package org.dromara.autotable.test.springboot;

import lombok.Data;
import org.dromara.autotable.annotation.*;
import org.dromara.autotable.annotation.mysql.MysqlTypeConstant;
import org.dromara.autotable.annotation.oracle.OracleTypeConstant;
import org.dromara.autotable.core.constants.DatabaseDialect;

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

    @ColumnComment("备注")
    @AutoColumns({
            @AutoColumn(dialect = DatabaseDialect.MySQL, type = MysqlTypeConstant.TEXT)
            , @AutoColumn(dialect = DatabaseDialect.Oracle, type = OracleTypeConstant.VARCHAR2, length = 4000)
    })
    private String mark;
}
