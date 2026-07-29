package org.dromara.autotable.adapter.mybatisflex;

import com.mybatisflex.annotation.Table;
import org.dromara.autotable.core.AutoTableClassScanner;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * MyBatis-Flex 类扫描器（零 Spring 依赖）。
 * <p>
 * 扫描 MyBatis-Flex 原生 @Table 注解标注的实体类。
 *
 * @author auto-table
 */
public class MybatisFlexAutoTableClassScanner extends AutoTableClassScanner {

    @Override
    protected Set<Class<? extends Annotation>> getIncludeAnnotations() {
        Set<Class<? extends Annotation>> includeAnnotations = super.getIncludeAnnotations();
        includeAnnotations.add(Table.class);
        return includeAnnotations;
    }
}
