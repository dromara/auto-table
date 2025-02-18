package org.dromara.autotable.annotation;

import org.dromara.autotable.annotation.enums.IndexSortTypeEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 索引字段的详细描述
 * @author don
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE})
public @interface IndexField {

    /**
     * @return 字段名
     */
    String field();

    /**
     * @return 字段排序方式
     */
    IndexSortTypeEnum sort();
}
