package org.dromara.autotable.annotation;

import org.dromara.autotable.annotation.enums.IndexTypeEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 设置表索引，fields与indexFields必须配置一个，不然不生效
 *
 * @author tangzc
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(TableIndexes.class)
public @interface TableIndex {

    /**
     * <p>索引的名字，设置了名字例如union_name,系统会默认在名字前加auto_idx_前缀，也就是auto_idx_union_name
     * <p>如果为空，则采用如下规则：
     * <p>1. 优先使用 auto_idx_`[表名]`_`[字段名1]`_`[字段名2]`
     * <p>2. 若超长(63字符)了，使用 auto_idx_`[表名]`_`[所有字段名链接后的hash值]`
     * <p>   注：长度定义63是兼容了pgsql的63字符以及mysql的64字符考虑的，Oracle本就不打算兼容，所以不考虑它的30字符长度
     * <p>3. 若仍超长了，使用 auto_idx_`[表名+所有字段名链接后的hash值]`
     * @return 索引名
     */
    String name() default "";

    /**
     * @return 索引类型标识
     */
    IndexTypeEnum type() default IndexTypeEnum.NORMAL;

    /**
     * @return 索引方法
     * Mysql中，默认为BTREE，可选的：BTREE、HASH
     * Pgsql中，默认为BTREE，可选的：BTREE、GiST、GIN
     * H2中，默认为BTREE，可选的：BTREE、HASH
     */
    String method() default "";

    /**
     * <p>字段名：支持多字段
     * <p>注意，多字段的情况下，字段书序即构建索引时候的顺序，牵扯索引左匹配问题
     * <p>该配置优先级低于{@link #indexFields()}，具体可参考{@link #indexFields()}的说明
     * @return 索引字段配置
     */
    String[] fields() default {};

    /**
     * <p>字段名：兼容需要指定字段排序方式的模式
     * <p>注意，多字段的情况下，字段书序即构建索引时候的顺序，牵扯索引左匹配问题
     * <p>该配置优先级高于{@link #fields()}，也就是说，生成索引字段的顺序，该配置中的列会排在{@link #fields()}之前，同时，如果该配置与{@link #fields()}之间存在重名的情况，以该配置为主
     * @return 索引字段配置
     */
    IndexField[] indexFields() default {};

    /**
     * @return 索引注释: 默认空字符串
     */
    String comment() default "";

}

