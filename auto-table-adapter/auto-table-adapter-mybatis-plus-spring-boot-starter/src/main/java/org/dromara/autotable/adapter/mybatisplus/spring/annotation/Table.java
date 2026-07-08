package org.dromara.autotable.adapter.mybatisplus.spring.annotation;

import com.baomidou.mybatisplus.annotation.TableName;
import org.dromara.autotable.annotation.AutoTable;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 创建表时的表名（组合注解）。
 * <p>
 * 组合了 MP 的 {@link TableName} 和 AutoTable 的 {@link AutoTable}，
 * 通过 {@link AliasFor} 实现属性别名，一个注解同时配置表名、schema 和注释。
 *
 * @author don
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TableName
@AutoTable
public @interface Table {

    /**
     * 表名
     */
    @AliasFor(annotation = TableName.class, attribute = "value")
    String value() default "";

    /**
     * 表 schema
     */
    @AliasFor(annotation = TableName.class, attribute = "schema")
    String schema() default "";

    /**
     * 表注释
     */
    @AliasFor(annotation = AutoTable.class, attribute = "comment")
    String comment() default "";

    /**
     * 数据源名称
     */
    String dsName() default "";

    /**
     * 需要排除的属性名，字段不作为数据库维护的列，同时数据操作时也会忽略。
     * 具备 {@link TableName#excludeProperty()} 的作用。
     */
    @AliasFor(annotation = TableName.class, attribute = "excludeProperty")
    String[] excludeProperty() default {"serialVersionUID"};

    /**
     * 是否保持全局表名前缀。
     * <p>
     * 默认 false：当配置了全局表前缀时，{@link #value()} 指定的表名不添加前缀。
     * 设为 true：即使 {@link #value()} 指定了表名，也会添加全局前缀。
     * 具备 {@link TableName#keepGlobalPrefix()} 的作用。
     */
    @AliasFor(annotation = TableName.class, attribute = "keepGlobalPrefix")
    boolean keepGlobalPrefix() default false;
}
