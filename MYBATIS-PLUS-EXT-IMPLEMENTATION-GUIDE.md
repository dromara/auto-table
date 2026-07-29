# mybatis-plus-ext 重构实现指南

> 本文档是 mybatis-plus-ext 项目的重构实现指南。AutoTable 的 adapter 模块已完成 MP 原生注解的兼容，mybatis-plus-ext 需在此基础上实现自定义注解的双向识别。

---

## 一、背景与架构定位

### 1.1 当前状态

| 项目 | 状态 | 职责 |
|------|:----:|------|
| **AutoTable adapter** | 已完成 | 单向兼容 MP 原生注解（`@TableName`/`@TableField`/`@TableId`/`@EnumValue`），零侵入 |
| **mybatis-plus-ext** | 待重构 | 定义一套自有注解（`@Table`/`@Column`/`@ColumnId`），同时让 AutoTable 和 MP 都能识别 |

### 1.2 分层扩展模式

所有涉及双向兼容的功能，统一采用 **adapter 基础兼容 + ext 扩展兼容** 的分层模式：

| 功能 | adapter 层（已完成） | ext 层（待实现） |
|------|----------------------|-------------------|
| **元数据适配** | `MybatisPlusMetadataAdapter` 读取 MP 原生注解 | 继承 Adapter，增加 `@Table`/`@Column`/`@ColumnId` 读取 |
| **类扫描** | `MybatisPlusAutoTableClassScanner` 扫描 `@TableName` | 继承 Scanner，增加 `@Table` 扫描 |
| **动态数据源** | `MybatisPlusDynamicDataSourceHandler` 读取 `@DS` | 继承 Handler，增加 `@Table.dsName()` 支持 |

### 1.3 ext 核心定位

mybatis-plus-ext 定义一套自有注解，通过 `@AliasFor` 桥接 MP 和 AutoTable 原生注解，实现：
- **AutoTable 侧**：继承 `MybatisPlusMetadataAdapter`，使用 `AnnotatedElementUtilsPlus` 读取自定义注解
- **MP 侧**：实现 `AnnotationHandler` 接口，让 MP 的 `TableInfoHelper` 能发现自定义注解

---

## 二、Maven 依赖

ext 的 autotable 模块需依赖 adapter 基础层：

```xml
<dependencies>
    <!-- AutoTable adapter 基础层（继承基础类） -->
    <dependency>
        <groupId>org.dromara.autotable</groupId>
        <artifactId>auto-table-adapter-mybatis-plus</artifactId>
        <version>${auto-table.version}</version>
    </dependency>

    <!-- AutoTable Spring Boot Starter（InitializeBeans 接口） -->
    <dependency>
        <groupId>org.dromara.autotable</groupId>
        <artifactId>auto-table-spring-boot-starter</artifactId>
        <version>${auto-table.version}</version>
    </dependency>

    <!-- MP 核心（AnnotationHandler 接口、GlobalConfig） -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-core</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>

    <!-- MP 注解（@TableName/@TableField/@TableId） -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-annotation</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>

    <!-- Spring（@AliasFor、AnnotatedElementUtils） -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-core</artifactId>
    </dependency>

    <!-- 动态数据源（@DS，optional） -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>dynamic-datasource-spring</artifactId>
        <version>${dynamic-datasource.version}</version>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## 三、待实现清单与代码参考

### 3.1 自定义注解定义

包路径：`org.dromara.mpe.autotable.annotation`（ext 自己的包）

通过 `@AliasFor` 桥接 MP 和 AutoTable 原生注解。以 `@Table` 为例：

```java
package org.dromara.mpe.autotable.annotation;

import com.baomidou.mybatisplus.annotation.TableName;
import org.dromara.autotable.annotation.AutoTable;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@TableName          // 桥接 MP
@AutoTable          // 桥接 AutoTable
public @interface Table {

    @AliasFor(annotation = TableName.class, attribute = "value")
    @AliasFor(annotation = AutoTable.class, attribute = "value")
    String value() default "";

    @AliasFor(annotation = AutoTable.class, attribute = "comment")
    String comment() default "";

    @AliasFor(annotation = TableName.class, attribute = "schema")
    String schema() default "";

    @AliasFor(annotation = TableName.class, attribute = "keepGlobalPrefix")
    boolean keepGlobalPrefix() default false;

    /** 动态数据源名称（ext 扩展属性） */
    String dsName() default "";

    /** 排除的属性名（桥接 @TableName.excludeProperty） */
    @AliasFor(annotation = TableName.class, attribute = "excludeProperty")
    String[] excludeProperty() default {};
}
```

`@Column` 桥接 `@TableField` + AutoTable 属性：

```java
package org.dromara.mpe.autotable.annotation;

import com.baomidou.mybatisplus.annotation.TableField;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@TableField
public @interface Column {

    @AliasFor(annotation = TableField.class, attribute = "value")
    String value() default "";

    @AliasFor(annotation = TableField.class, attribute = "exist")
    boolean exist() default true;

    /** 默认值 */
    String defaultValue() default "";

    /** 默认值类型 */
    org.dromara.autotable.annotation.enums.DefaultValueEnum defaultValueType()
            default org.dromara.autotable.annotation.enums.DefaultValueEnum.UNDEFINED;
}
```

`@ColumnId` 桥接 `@TableId`：

```java
package org.dromara.mpe.autotable.annotation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@TableId
public @interface ColumnId {

    @AliasFor(annotation = TableId.class, attribute = "value")
    String value() default "";

    @AliasFor(annotation = TableId.class, attribute = "type")
    IdType mode() default IdType.NONE;
}
```

`@UniqueIndex` 定义唯一索引：

```java
package org.dromara.mpe.autotable.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface UniqueIndex {
    String name() default "";
}
```

---

### 3.2 AnnotationHandler 实现（让 MP 识别自定义注解）

MP 3.5.16 提供了 `com.baomidou.mybatisplus.core.handlers.AnnotationHandler` SPI，通过自定义实现让 MP 的 `TableInfoHelper` 发现 `@AliasFor` 桥接的注解。

```java
package org.dromara.mpe.autotable.handler;

import com.baomidou.mybatisplus.core.handlers.AnnotationHandler;
import org.dromara.mpe.autotable.util.AnnotatedElementUtilsPlus;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * MP 注解处理器，使用 Spring AnnotatedElementUtils 支持 @AliasFor 深度合并。
 * 让 MP 扫描 @TableField 时，能发现 @Column 通过 @AliasFor 桥接过来的值。
 */
public class ExtAnnotationHandler implements AnnotationHandler {

    @Override
    public <T extends Annotation> T getAnnotation(Class<?> beanClass, Class<T> annotationClass) {
        return AnnotatedElementUtils.findMergedAnnotation(beanClass, annotationClass);
    }

    @Override
    public <T extends Annotation> T getAnnotation(AnnotatedElement annotatedElement, Class<T> annotationClass) {
        return AnnotatedElementUtils.findMergedAnnotation(annotatedElement, annotationClass);
    }

    @Override
    public boolean isAnnotationPresent(Class<?> beanClass, Class<? extends Annotation> annotationClass) {
        return AnnotatedElementUtils.hasMetaAnnotation(beanClass, annotationClass);
    }

    @Override
    public boolean isAnnotationPresent(AnnotatedElement annotatedElement, Class<? extends Annotation> annotationClass) {
        return AnnotatedElementUtils.hasMetaAnnotation(annotatedElement, annotationClass);
    }
}
```

注册到 MP 的 `GlobalConfig`：

```java
@Bean
public GlobalConfigCustomizer extGlobalConfigCustomizer() {
    return globalConfig -> globalConfig.setAnnotationHandler(new ExtAnnotationHandler());
}
```

---

### 3.3 MetadataAdapter 扩展（让 AutoTable 识别自定义注解）

继承 adapter 的 `MybatisPlusMetadataAdapter`，使用 `AnnotatedElementUtilsPlus` 读取自定义注解：

```java
package org.dromara.mpe.autotable;

import org.dromara.autotable.adapter.mybatisplus.MybatisPlusMetadataAdapter;
import org.dromara.autotable.adapter.mybatisplus.MybatisPlusAdapterConfig;
import org.dromara.autotable.annotation.ColumnDefault;
import org.dromara.autotable.springboot.InitializeBeans;
import org.dromara.mpe.autotable.annotation.Column;
import org.dromara.mpe.autotable.annotation.ColumnId;
import org.dromara.mpe.autotable.annotation.Table;
import org.dromara.mpe.autotable.util.AnnotatedElementUtilsPlus;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;

/**
 * 扩展 adapter 的 MetadataAdapter，支持读取自定义注解。
 * 使用 AnnotatedElementUtilsPlus 处理 @AliasFor 合并逻辑。
 * 实现 InitializeBeans 确保在 AutoTableAutoConfig 处理 ObjectProvider 前被创建。
 */
public class ExtMetadataAdapter extends MybatisPlusMetadataAdapter implements InitializeBeans {

    public ExtMetadataAdapter(MybatisPlusAdapterConfig config) {
        super(config);
    }

    @Override
    public String getTableName(Class<?> clazz) {
        Table tableAnno = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(clazz, Table.class);
        if (tableAnno != null && StringUtils.hasText(tableAnno.value())) {
            String finalTableName = filterSpecialChar(tableAnno.value());
            String tablePrefix = getConfig().getTablePrefix();
            boolean addTablePrefix = StringUtils.hasText(tablePrefix) && !tableAnno.keepGlobalPrefix();
            if (addTablePrefix) {
                finalTableName = tablePrefix + finalTableName;
            }
            return finalTableName;
        }
        return super.getTableName(clazz);
    }

    @Override
    public String getColumnName(Class<?> clazz, Field field) {
        Column columnAnno = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(field, Column.class);
        if (columnAnno != null && StringUtils.hasText(columnAnno.value())) {
            return filterSpecialChar(columnAnno.value());
        }
        ColumnId columnIdAnno = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(field, ColumnId.class);
        if (columnIdAnno != null && StringUtils.hasText(columnIdAnno.value())) {
            return filterSpecialChar(columnIdAnno.value());
        }
        return super.getColumnName(clazz, field);
    }

    @Override
    public Boolean isPrimary(Field field, Class<?> clazz) {
        ColumnId columnIdAnno = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(field, ColumnId.class);
        if (columnIdAnno != null) {
            return true;
        }
        return super.isPrimary(field, clazz);
    }

    @Override
    public Boolean isAutoIncrement(Field field, Class<?> clazz) {
        if (!isPrimary(field, clazz)) {
            return false;
        }
        ColumnId columnIdAnno = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(field, ColumnId.class);
        if (columnIdAnno != null) {
            return columnIdAnno.mode() == com.baomidou.mybatisplus.annotation.IdType.AUTO;
        }
        return super.isAutoIncrement(field, clazz);
    }

    @Override
    public Boolean isIgnoreField(Field field, Class<?> clazz) {
        // 通过 @AliasFor 合并后，@Column 带有 @TableField 的 exist 属性
        com.baomidou.mybatisplus.annotation.TableField tableFieldAnno =
                AnnotatedElementUtilsPlus.findDeepMergedAnnotation(field,
                        com.baomidou.mybatisplus.annotation.TableField.class);
        if (tableFieldAnno != null && !tableFieldAnno.exist()) {
            return true;
        }
        Table tableAnno = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(clazz, Table.class);
        if (tableAnno != null) {
            for (String property : tableAnno.excludeProperty()) {
                if (property.equals(field.getName())) {
                    return true;
                }
            }
        }
        return super.isIgnoreField(field, clazz);
    }

    @Override
    public ColumnDefault getColumnDefaultValue(Field field, Class<?> clazz) {
        Column columnAnno = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(field, Column.class);
        if (columnAnno != null && StringUtils.hasText(columnAnno.defaultValue())) {
            final String value = columnAnno.defaultValue();
            final var type = columnAnno.defaultValueType();
            return new ColumnDefault() {
                @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return null; }
                @Override public org.dromara.autotable.annotation.enums.DefaultValueEnum type() { return type; }
                @Override public String value() { return value; }
            };
        }
        return super.getColumnDefaultValue(field, clazz);
    }
}
```

**关键点**：
- 继承 adapter 的 `MybatisPlusMetadataAdapter`，覆盖所有方法
- 自定义注解不存在或值为空时，回退到 `super.xxx()` 调用 adapter 基础逻辑
- 实现 `InitializeBeans` 确保在 `AutoTableAutoConfig` 处理 `ObjectProvider` 之前被创建

---

### 3.4 ClassScanner 扩展

继承 adapter 的 `MybatisPlusAutoTableClassScanner`，额外扫描 `@Table` 注解：

```java
package org.dromara.mpe.autotable;

import org.dromara.autotable.adapter.mybatisplus.MybatisPlusAutoTableClassScanner;
import org.dromara.mpe.autotable.annotation.Table;

import java.lang.annotation.Annotation;
import java.util.Set;

public class ExtClassScanner extends MybatisPlusAutoTableClassScanner {
    @Override
    protected Set<Class<? extends Annotation>> getIncludeAnnotations() {
        Set<Class<? extends Annotation>> annos = super.getIncludeAnnotations();
        annos.add(Table.class);
        return annos;
    }
}
```

---

### 3.5 DynamicDataSourceHandler 扩展

继承 adapter 的 `MybatisPlusDynamicDataSourceHandler`，增加 `@Table.dsName()` 支持：

```java
package org.dromara.mpe.autotable;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.dromara.autotable.adapter.mybatisplus.spring.MybatisPlusDynamicDataSourceHandler;
import org.dromara.autotable.core.dynamicds.IDataSourceHandler;
import org.dromara.mpe.autotable.annotation.Table;
import org.dromara.mpe.autotable.util.AnnotatedElementUtilsPlus;
import org.springframework.util.StringUtils;

/**
 * 扩展 adapter 的 DynamicDataSourceHandler。
 * 优先级：MP 原生 @DS > 自定义 @Table.dsName > primary 数据源。
 */
public class ExtDynamicDataSourceHandler extends MybatisPlusDynamicDataSourceHandler {

    public ExtDynamicDataSourceHandler(String primaryDataSourceName) {
        super(primaryDataSourceName);
    }

    @Override
    public String getDataSourceName(Class clazz) {
        // 1. 优先读 MP 原生 @DS
        DS ds = (DS) AnnotatedElementUtilsPlus.findDeepMergedAnnotation(clazz, DS.class);
        if (ds != null && StringUtils.hasText(ds.value())) {
            return ds.value();
        }
        // 2. 读自定义 @Table.dsName()
        Table tableAnno = AnnotatedElementUtilsPlus.findDeepMergedAnnotation(clazz, Table.class);
        if (tableAnno != null && StringUtils.hasText(tableAnno.dsName())) {
            return tableAnno.dsName();
        }
        // 3. 回退到 adapter 基础逻辑（@DS + primary）
        return super.getDataSourceName(clazz);
    }
}
```

---

### 3.6 工具类

`AnnotatedElementUtilsPlus` 和 `AnnotationDefaultValueHelper` 从原 mybatis-plus-ext 原样保留，不做修改。

- `AnnotatedElementUtilsPlus`：封装 Spring `AnnotatedElementUtils`，支持 `@AliasFor` 深度合并
- `AnnotationDefaultValueHelper`：处理注解默认值

---

## 四、Spring Boot 自动配置

ext 的 AutoConfiguration 需用 `@ConditionalOnMissingBean` 覆盖 adapter 的基础 Bean：

```java
package org.dromara.mpe.autotable.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import org.dromara.autotable.adapter.mybatisplus.MybatisPlusAdapterConfig;
import org.dromara.autotable.core.AutoTableClassScanner;
import org.dromara.autotable.core.AutoTableMetadataAdapter;
import org.dromara.mpe.autotable.ExtClassScanner;
import org.dromara.mpe.autotable.ExtMetadataAdapter;
import org.dromara.mpe.autotable.handler.ExtAnnotationHandler;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "com.baomidou.mybatisplus.annotation.TableName")
@AutoConfigureBefore(name = "org.dromara.autotable.springboot.AutoTableAutoConfig")
public class ExtAutoConfiguration {

    /**
     * 覆盖 adapter 的 MetadataAdapter，使用 ext 扩展版本。
     * @ConditionalOnMissingBean 确保用户可自定义覆盖。
     */
    @Bean
    @ConditionalOnMissingBean(AutoTableMetadataAdapter.class)
    public ExtMetadataAdapter extMetadataAdapter(MybatisPlusAdapterConfig config) {
        return new ExtMetadataAdapter(config);
    }

    /**
     * 覆盖 adapter 的 ClassScanner，使用 ext 扩展版本。
     */
    @Bean
    @ConditionalOnMissingBean(AutoTableClassScanner.class)
    public ExtClassScanner extClassScanner() {
        return new ExtClassScanner();
    }

    /**
     * 注册 AnnotationHandler，让 MP 识别自定义注解。
     */
    @Bean
    public GlobalConfigCustomizer extGlobalConfigCustomizer() {
        return globalConfig -> globalConfig.setAnnotationHandler(new ExtAnnotationHandler());
    }
}
```

**InitializeBeans 机制**：

`AutoTableAutoConfig` 在构造器中通过 `ObjectProvider<InitializeBeans>` 强制触发 Bean 实例化。ext 的 `ExtMetadataAdapter` 必须实现 `InitializeBeans` 接口，确保在 `AutoTableAutoConfig` 处理 `ObjectProvider` 之前被创建，否则 adapter 的基础 `MybatisPlusMetadataAdapter` 会先被注册，ext 的扩展版本不生效。

---

## 五、验证清单

### 功能验证
- [ ] 自定义 `@Table` 注解实体 -> 建表 SQL 正确（`@AliasFor` 合并生效）
- [ ] `@Table.keepGlobalPrefix()` 控制全局前缀行为正确
- [ ] `@Column` 注解字段 -> 列名、默认值正确
- [ ] `@Column(exist=false)` -> 字段不建列
- [ ] `@ColumnId` 注解字段 -> 主键策略正确
- [ ] `@UniqueIndex` -> 唯一索引创建
- [ ] MP 查询能识别 `@Table`/`@Column`/`@ColumnId`（AnnotationHandler 生效）
- [ ] 逻辑删除字段默认值正确
- [ ] 枚举字段（`@EnumValue` + `IEnum`）类型正确
- [ ] 动态数据源：`@DS` > `@Table.dsName` > primary 优先级正确

### 兼容验证
- [ ] 只引入 adapter（不引入 ext），MP 原生注解实体建表正常
- [ ] 同时引入 adapter + ext，自定义注解实体建表正常
- [ ] 同时引入 adapter + ext，MP 原生注解实体建表仍正常（向下兼容）

### Bean 加载顺序验证
- [ ] `ExtMetadataAdapter` 实现 `InitializeBeans`，在 `AutoTableAutoConfig` 前被创建
- [ ] `@AutoConfigureBefore` 生效，ext 配置先于 AutoTable 主配置加载
