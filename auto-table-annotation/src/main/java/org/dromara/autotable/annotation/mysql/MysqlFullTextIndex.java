package org.dromara.autotable.annotation.mysql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>指定MySQL字段的全文索引（FullText Index）。
 * <p>全文索引不是 {@link org.dromara.autotable.annotation.Index} 的补充，而是替代方案。
 * 字段上标注 {@code @MysqlFullTextIndex} 时，不需要同时写 {@code @Index}。
 * <p>生成的SQL示例：
 * <pre>{@code
 *   FULLTEXT INDEX `idx_content`(`content`) COMMENT '全文索引'
 *   FULLTEXT INDEX `idx_content`(`content`) COMMENT '全文索引' WITH PARSER ngram
 * }</pre>
 *
 * @author don
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MysqlFullTextIndex {

    /**
     * <p>生成规则优化：
     * <p>1. 优先使用 auto_idx_`[表名]`_`[字段名1]`_`[字段名2]`
     * <p>2. 若超长(63字符)了，使用 auto_idx_`[表名]`_`[所有字段名链接后的hash值]`
     * <p>   注：长度定义63是兼容了pgsql的63字符，与mysql的64字符考虑的，Oracle本就不打算兼容，所以不考虑它的30字符长度
     * <p>3. 若仍超长了，使用 auto_idx_`[表名+所有字段名链接后的hash值]`
     * @return 索引的名字，不设置默认为{auto_idx_[表名]_[字段名]}
     */
    String name() default "";

    /**
     * @return 索引注释
     */
    String comment() default "";

    /**
     * <p>全文索引的分词器。
     * <p>例如，中文可以使用 ngram 分词器：
     * <pre>{@code
     *   FULLTEXT INDEX `idx_content`(`content`) WITH PARSER ngram
     * }</pre>
     *
     * @return 分词器名称，默认为空字符串（不指定）
     */
    String parser() default "";

}
