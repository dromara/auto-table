package org.dromara.autotable.annotation.mysql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 设置 MySQL 表级别全文索引，用于组合多个字段的全文索引
 *
 * @author don
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(MysqlTableFullTextIndexes.class)
public @interface MysqlTableFullTextIndex {

    /**
     * <p>索引的名字，设置了名字例如ft_name，系统会默认在名字前加auto_idx_前缀，也就是auto_idx_ft_name
     * <p>如果为空，则采用如下规则：
     * <p>1. 优先使用 auto_idx_`[表名]`_`[字段名1]`_`[字段名2]`
     * <p>2. 若超长(63字符)了，使用 auto_idx_`[表名]`_`[所有字段名链接后的hash值]`
     * @return 索引名
     */
    String name() default "";

    /**
     * @return 索引注释: 默认空字符串
     */
    String comment() default "";

    /**
     * @return 分词器，例如 ngram
     */
    String parser() default "";

    /**
     * <p>字段名：支持多字段
     * <p>注意，多字段的情况下，字段顺序即构建索引时候的顺序
     * @return 索引字段配置
     */
    String[] fields() default {};
}
