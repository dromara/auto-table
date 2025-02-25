//package org.dromara.autotable.test.core.dynamicdatasource;
//
//import org.dromara.autotable.annotation.AutoTable;
//import org.dromara.autotable.annotation.ColumnComment;
//import org.dromara.autotable.annotation.ColumnDefault;
//import org.dromara.autotable.annotation.ColumnType;
//import org.dromara.autotable.annotation.PrimaryKey;
//import org.dromara.autotable.annotation.enums.DefaultValueEnum;
//import lombok.Data;
//
//@Data
//@Ds("sqlite")
//@AutoTable("sys_user")
//public class UserSqlite {
//
//    @PrimaryKey(autoIncrement = true)
//    @ColumnComment("用户id")
//    private Long id;
//
//    @ColumnComment("姓名")
//    @ColumnType(value = "text", length = 100)
//    private String name;
//
//    @ColumnComment("备注")
//    @ColumnType("text")
//    @ColumnDefault(type = DefaultValueEnum.NULL)
//    private String mark;
//}
