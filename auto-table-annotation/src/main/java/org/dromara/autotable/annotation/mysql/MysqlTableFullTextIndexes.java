package org.dromara.autotable.annotation.mysql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 设置多个 MySQL 表级别全文索引
 *
 * @author don
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MysqlTableFullTextIndexes {

    /**
     * @return 索引集合
     */
    MysqlTableFullTextIndex[] value();
}
