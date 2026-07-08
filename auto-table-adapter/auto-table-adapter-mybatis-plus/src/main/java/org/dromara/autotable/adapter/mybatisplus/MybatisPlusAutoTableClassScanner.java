package org.dromara.autotable.adapter.mybatisplus;

import com.baomidou.mybatisplus.annotation.TableName;
import org.dromara.autotable.core.AutoTableClassScanner;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * MyBatis-Plus 类扫描器（零 Spring 依赖）。
 * <p>
 * 扫描 MP 原生 @TableName 注解标注的实体类。
 * 自定义注解（@Table 等）的扫描由 starter 的扩展类 {@code MybatisPlusExtendedClassScanner} 补充。
 *
 * @author auto-table
 */
public class MybatisPlusAutoTableClassScanner extends AutoTableClassScanner {

    @Override
    protected Set<Class<? extends Annotation>> getIncludeAnnotations() {
        Set<Class<? extends Annotation>> includeAnnotations = super.getIncludeAnnotations();
        includeAnnotations.add(TableName.class);
        return includeAnnotations;
    }
}
