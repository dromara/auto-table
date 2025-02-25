//package org.dromara.autotable.test.core.dynamicdatasource;
//
//import org.dromara.autotable.annotation.AutoTable;
//import org.dromara.autotable.annotation.ColumnComment;
//import org.dromara.autotable.annotation.ColumnDefault;
//import org.dromara.autotable.annotation.ColumnType;
//import org.dromara.autotable.annotation.Index;
//import org.dromara.autotable.annotation.PrimaryKey;
//import org.dromara.autotable.annotation.enums.DefaultValueEnum;
//import org.dromara.autotable.annotation.pgsql.PgsqlTypeConstant;
//import lombok.Data;
//
/// **
// * @author don
// */
//@Data
//@Ds("pgsql")
//@AutoTable(value = "sys_user", comment = "用户表")
//public class UserPgsql {
//
//    @PrimaryKey(autoIncrement = true)
//    @ColumnComment("用户id")
//    private Long id;
//
//    @ColumnComment("姓名")
//    @Index
//    private String name;
//
//    @ColumnComment("备注")
//    @ColumnType(PgsqlTypeConstant.VARCHAR)
//    @ColumnDefault(type = DefaultValueEnum.NULL)
//    private String mark;
//}
