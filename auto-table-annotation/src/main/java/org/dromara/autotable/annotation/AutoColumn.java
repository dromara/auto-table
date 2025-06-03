package org.dromara.autotable.annotation;

import org.dromara.autotable.annotation.enums.DefaultValueEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoColumn {

    /**
     * 列名
     * {@link ColumnName#value()}
     *
     * @return 列名
     */
    String value() default "";

    /**
     * 字段类型：不填默认使用属性的数据类型进行转换，转换失败的字段不会添加
     * {@link ColumnType#value()}
     *
     * @return 字段类型
     */
    String type() default "";

    /**
     * 字段长度,默认是-1，小于0相当于null
     * {@link ColumnType#length()}
     *
     * @return 默认字段长度
     */
    int length() default -1;

    /**
     * 小数点长度，默认是-1，小于0相当于null
     * {@link ColumnType#decimalLength()}
     *
     * @return 小数点长度
     */
    int decimalLength() default -1;

    /**
     * 是否为可以为null，true是可以，false是不可以，默认为true
     * {@link ColumnNotNull#value()}
     *
     * @return 是否为可以为null，true是不可以，false是可以，默认为false
     */
    boolean notNull() default false;

    /**
     * 默认值，默认为null
     * {@link ColumnDefault#value()}
     *
     * @return 默认值
     */
    String defaultValue() default "";

    /**
     * 默认值，默认为null
     * {@link ColumnDefault#type()}
     *
     * @return 默认值
     */
    DefaultValueEnum defaultValueType() default DefaultValueEnum.UNDEFINED;

    /**
     * 数据表字段备注
     * {@link ColumnComment#value()}
     *
     * @return 默认值，默认为空
     */
    String comment() default "";

    /**
     * 字段排序，注意并非所有数据库都适用
     * @return 序号，默认为0，1代表第一个，2代表第二个，以此类推，-1代表最后一个，-2代表倒数第二个，以此类推
     */
    int sort() default 0;

    /**
     * @return 自定义数据库方言，参考 {@link org.dromara.autotable.core.constants.DatabaseDialect} 常量
     */
    String dialect() default "";
}
