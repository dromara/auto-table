package org.dromara.autotable.test.core.dynamicdatasource;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.*;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.annotation.h2.H2TypeConstant;

import java.time.LocalDateTime;

@Data
@FieldNameConstants
@Ds("h2")
@AutoTable(value = "sys_user", comment = "用户表")
@TableIndex(fields = {UserH2.Fields.phone, UserH2.Fields.name})
public class UserH2 {

    @PrimaryKey(autoIncrement = true)
    @ColumnComment("用户id")
    private Long id;

    @ColumnComment("年龄")
    @ColumnDefault("25")
    private Integer age;

    @Index(type = IndexTypeEnum.UNIQUE, comment = "name的索引")
    @ColumnComment("姓名")
    @ColumnType(value = H2TypeConstant.CHARACTER_VARYING, length = 120)
    private String name;

    @Index(type = IndexTypeEnum.UNIQUE)
    @ColumnComment("电话")
    @ColumnDefault("18888888888")
    @ColumnType(length = 11)
    private String phone;

    @AutoColumn(defaultValue = "123这是备注", comment = "备注")
    private String mark;

    @ColumnComment("创建时间")
    private LocalDateTime createTime;
}
