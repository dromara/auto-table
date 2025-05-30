package org.dromara.autotable.test.core.entity.common_update;

import lombok.Data;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.AutoColumns;
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.ColumnComment;
import org.dromara.autotable.annotation.ColumnDefault;
import org.dromara.autotable.annotation.ColumnType;
import org.dromara.autotable.annotation.enums.DefaultValueEnum;
import org.dromara.autotable.annotation.mysql.MysqlTypeConstant;
import org.dromara.autotable.annotation.pgsql.PgsqlTypeConstant;
import org.dromara.autotable.core.constants.DatabaseDialect;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AutoTable
public class TestDefineColumn {

    @ColumnComment("id主键")
    protected String id;

    // @ColumnNotNull
    @ColumnDefault(type = DefaultValueEnum.EMPTY_STRING)
    // @ColumnType(length = 100)
    @ColumnComment("用户名")
    protected String username;

    @ColumnDefault("1")
    // @ColumnDefault("0")
    // @ColumnComment("年龄")
    protected Integer age;

    @ColumnType(length = 20)
    @AutoColumn(comment = "电话", defaultValue = "+00 00000000")
    // @AutoColumn(comment = "电话", defaultValue = "+00 00000000", notNull = true)
    protected String phone;

    @AutoColumn(comment = "资产", length = 14, decimalLength = 5)
    // @AutoColumn(comment = "资产", length = 12, decimalLength = 6)
    protected BigDecimal money;

    @ColumnDefault("true")
    @AutoColumn(comment = "激活状态(true/false)")
    // @AutoColumn(comment = "激活状态")
    protected Boolean active;

    @ColumnComment("这是个人简介")
    @ColumnDefault("这个人很懒～，没有什么可说的。123～abc~")
    @AutoColumns({
            @AutoColumn(type = MysqlTypeConstant.VARCHAR, length = 1000, dialect = DatabaseDialect.MySQL),
            @AutoColumn(type = PgsqlTypeConstant.TEXT, dialect = DatabaseDialect.PostgreSQL),
    })
    protected String description;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @AutoColumn(comment = "注册时间")
    protected LocalDateTime registerTime;

    // @Ignore
    protected String extra;
}
