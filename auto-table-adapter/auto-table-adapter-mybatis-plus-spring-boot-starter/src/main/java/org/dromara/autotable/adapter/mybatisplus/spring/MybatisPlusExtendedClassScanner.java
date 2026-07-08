package org.dromara.autotable.adapter.mybatisplus.spring;

import org.dromara.autotable.adapter.mybatisplus.MybatisPlusAutoTableClassScanner;
import org.dromara.autotable.adapter.mybatisplus.spring.annotation.Table;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * 扩展 adapter 的类扫描器，额外扫描自定义 {@link Table} 注解。
 * <p>
 * adapter 的 {@link MybatisPlusAutoTableClassScanner} 只扫描 MP 原生 {@code @TableName}，
 * 本扩展类补充扫描自定义 {@code @Table}（带 {@code @AliasFor}）标注的实体。
 *
 * @author auto-table
 */
public class MybatisPlusExtendedClassScanner extends MybatisPlusAutoTableClassScanner {

    @Override
    protected Set<Class<? extends Annotation>> getIncludeAnnotations() {
        Set<Class<? extends Annotation>> annos = super.getIncludeAnnotations();
        annos.add(Table.class);
        return annos;
    }
}
