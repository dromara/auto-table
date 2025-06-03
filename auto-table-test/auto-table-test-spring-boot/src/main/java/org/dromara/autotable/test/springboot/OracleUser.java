package org.dromara.autotable.test.springboot;

import lombok.Data;
import org.dromara.autotable.annotation.*;
import org.dromara.autotable.annotation.enums.IndexSortTypeEnum;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;

import java.util.Date;

@Data
@AutoTable(value = "oracle_user", comment = "修改注释")
@TableIndex(type = IndexTypeEnum.UNIQUE, indexFields = {
        @IndexField(field = "age", sort = IndexSortTypeEnum.DESC)
        , @IndexField(field = "phone", sort = IndexSortTypeEnum.DESC)
})
public class OracleUser {
    @ColumnComment("用户id")
    private Long id;

    // @PrimaryKey(autoIncrement = true)
    @ColumnComment("用户id2")
    private Long id2;

    @AutoColumn(value = "PHONe", notNull = true)
    @ColumnComment("电话")
    @ColumnDefault("17")
    @ColumnType(value = "varchar2", length = 1999)
    private String phone;

    @ColumnComment("年龄2")
    @ColumnDefault("11")
    private Integer age;

    /*@ColumnComment("备注2")
    @ColumnType("CLOB")
    private String mark;*/


    @AutoColumn(comment = "创建时间", defaultValue = "SYSDATE")
    @Index(type = IndexTypeEnum.NORMAL, method = "SYSDATE")
    private Date time;

}
