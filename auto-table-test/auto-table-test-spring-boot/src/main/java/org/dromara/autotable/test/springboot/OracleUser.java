package org.dromara.autotable.test.springboot;

import lombok.Data;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.ColumnComment;
import org.dromara.autotable.annotation.ColumnDefault;
import org.dromara.autotable.annotation.ColumnType;
import org.dromara.autotable.annotation.Index;
import org.dromara.autotable.annotation.PrimaryKey;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;

import java.util.Date;

@Data
// @AutoTable(value = "oracle_user", comment = "修改注释")
// @TableIndex(type = IndexTypeEnum.UNIQUE, indexFields = {
//         @IndexField(field = "phone", sort = IndexSortTypeEnum.DESC)
//         , @IndexField(field = "age", sort = IndexSortTypeEnum.DESC)
//
// })
public class OracleUser {

    @PrimaryKey(autoIncrement = true)
    @ColumnComment("用户id")
    private Long id;

    @ColumnComment("用户id2")
    private Long id2;

    @AutoColumn(value = "PHONe", notNull = true)
    @ColumnComment("电话")
    @ColumnDefault("50")
    @ColumnType(value = "varchar2", length = 300)
    private String phone;

    @ColumnComment("年龄2")
    @ColumnDefault("11")
    private Integer age;

    @ColumnComment("备注2")
    @ColumnType("CLOB")
    private String mark;


    @AutoColumn(comment = "创建时间", defaultValue = "SYSDATE")
    @Index(type = IndexTypeEnum.NORMAL, method = "SYSDATE")
    private Date time;

}
