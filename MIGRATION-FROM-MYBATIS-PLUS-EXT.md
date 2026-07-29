# MyBatis-Plus 适配器迁移文档

> 本文档记录从 `mybatis-plus-ext/mybatis-plus-ext-autotable` 迁移到 `auto-table-adapter-mybatis-plus` 的架构决策与当前状态。

---

## 一、架构定位

### 1.1 模块边界

| 模块 | 职责 | Spring 依赖 |
|------|------|:---:|
| `auto-table-adapter-mybatis-plus` | 读取 MP 原生注解（`@TableName`/`@TableField`/`@TableId`/`@EnumValue`），实现 AutoTable SPI | 无 |
| `auto-table-adapter-mybatis-plus-spring-boot-starter` | Spring 装配 + 配置桥接 + 动态数据源支持 | 依赖 |

### 1.2 核心定位

adapter 的目标是**零侵入兼容 MP 原生注解**：用户已有的 MP 实体类无需添加任何 AutoTable 注解，引入 adapter 依赖即可获得 DDL 自动管理能力。

### 1.3 adapter 铁律

**adapter 模块内不得出现**：
- 任何 `org.springframework.*` import
- `@AliasFor`、`@Configuration`、`@Bean`、`@ConditionalOnXxx`、`@Autowired`、`@Value`、`@Component`、`@Import`
- `spring.factories`、`AutoConfiguration.imports`
- `AnnotatedElementUtils`、`StringUtils`（Spring 的）

**adapter 只允许依赖**：
- `auto-table-core`、`auto-table-annotation`
- `mybatis-plus-annotation`、`mybatis-plus-core`（MP 原生）
- `slf4j-api`

### 1.4 拆分模式

| 职责 | 归属 |
|------|------|
| 实现 AutoTable SPI 接口的主体逻辑 | adapter |
| 读 MP 原生注解（`@TableName`/`@TableField`/`@TableId`/`@EnumValue`） | adapter（Java 原生反射） |
| 数据源切换、拦截器忽略等 MP 原生 API 调用 | adapter |
| config 值承载（POJO，无 Spring） | adapter |
| `@Configuration`/`@Bean`/`@ConditionalOnXxx` | starter |
| 从 Spring Properties 提取值注入 adapter | starter（桥接） |
| 动态数据源处理（`@DS`、`DynamicRoutingDataSource`） | starter（因依赖 Spring） |

---

## 二、adapter 模块代码结构

包路径：`org.dromara.autotable.adapter.mybatisplus`

| 类 | 职责 |
|----|------|
| `MybatisPlusAdapterConfig` | 配置 POJO（tablePrefix、mapUnderscoreToCamelCase、capitalMode、logicDeleteField、logicNotDeleteValue） |
| `MybatisPlusMetadataAdapter` | 核心：读取 MP 原生注解，转换为 AutoTable 元数据 |
| `MybatisPlusJavaTypeToDatabaseTypeConverter` | 枚举类型（`@EnumValue`/`IEnum`）和自定义 TypeHandler 的字段类型转换 |
| `MybatisPlusAutoTableClassScanner` | 扫描 `@TableName` 注解标注的实体类 |
| `MybatisPlusRunBeforeCallback` | 建表前：设置 MP 拦截器忽略策略 |
| `MybatisPlusRunAfterCallback` | 建表后：清理拦截器忽略策略 |

### 2.1 关键实现细节

**`@Ignore` 与 `@TableField(exist=false)` 互通**：

`isIgnoreField()` 方法同时检查两者，任一存在即视为忽略字段：
```java
@Override
public Boolean isIgnoreField(Field field, Class<?> clazz) {
    // 1. @Ignore（auto-table 注解）
    Ignore ignore = field.getAnnotation(Ignore.class);
    if (ignore != null) {
        return true;
    }
    // 2. @TableField.exist() = false
    TableField tableField = field.getAnnotation(TableField.class);
    if (tableField != null && !tableField.exist()) {
        return true;
    }
    // 3. @TableName.excludeProperty()
    // ...
}
```

**枚举值使用 `Enum::name()`**：

无 `@EnumValue` 时，使用 `Enum::name()` 而非 `Object::toString()`，与 MP 默认行为一致。

---

## 三、starter 模块代码结构

包路径：`org.dromara.autotable.adapter.mybatisplus.spring`

| 类 | 职责 |
|----|------|
| `MybatisPlusAutoConfiguration` | Spring 自动配置入口：注册 adapter Bean、桥接 MP 配置 |
| `MybatisPlusDynamicDataSourceHandler` | 动态数据源处理器：读取 MP `@DS` 注解 |
| `MybatisPlusDataSourceInfoExtractor` | 动态数据源场景下的 JDBC 信息提取 |

### 3.1 自动配置注册的 Bean

```java
@Configuration
@ConditionalOnClass(name = "com.baomidou.mybatisplus.annotation.TableName")
@AutoConfigureBefore(name = "org.dromara.autotable.springboot.AutoTableAutoConfig")
public class MybatisPlusAutoConfiguration {

    // 1. 桥接：MP Properties -> adapter config POJO
    @Bean
    public MybatisPlusAdapterConfig mybatisPlusAdapterConfig(MybatisPlusProperties mpProperties) { ... }

    // 2. 元数据适配器
    @Bean
    @ConditionalOnMissingBean(AutoTableMetadataAdapter.class)
    public MybatisPlusMetadataAdapter mybatisPlusMetadataAdapter(MybatisPlusAdapterConfig config) { ... }

    // 3. 类扫描器
    @Bean
    @ConditionalOnMissingBean(AutoTableClassScanner.class)
    public MybatisPlusAutoTableClassScanner mybatisPlusAutoTableClassScanner() { ... }

    // 4. 类型转换器
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusJavaTypeToDatabaseTypeConverter mybatisPlusJavaTypeToDatabaseTypeConverter() { ... }

    // 5. 生命周期回调
    @Bean public MybatisPlusRunBeforeCallback mybatisPlusRunBeforeCallback() { ... }
    @Bean public MybatisPlusRunAfterCallback mybatisPlusRunAfterCallback() { ... }

    // 6. 动态数据源（独立内部类，类级 @ConditionalOnClass 保护）
    @Configuration
    @ConditionalOnClass(name = "...DynamicDataSourceProperties")
    static class DynamicDataSourceConfiguration { ... }
}
```

### 3.2 MP 原生配置 API 映射（MP 3.5.16 实测）

| adapter config 字段 | MP 原生读取路径 | 备注 |
|---------------------|----------------|------|
| `tablePrefix` | `mpProperties.getGlobalConfig().getDbConfig().getTablePrefix()` | |
| `mapUnderscoreToCamelCase` | `mpProperties.getConfiguration().getMapUnderscoreToCamelCase()` | 返回 `Boolean`，需空值保护 |
| `capitalMode` | `mpProperties.getGlobalConfig().getDbConfig().isCapitalMode()` | |
| `logicDeleteField` | `mpProperties.getGlobalConfig().getDbConfig().getLogicDeleteField()` | |
| `logicNotDeleteValue` | `mpProperties.getGlobalConfig().getDbConfig().getLogicNotDeleteValue()` | |

---

## 四、与 mybatis-plus-ext 的职责划分

| 项目 | 职责 | 机制 |
|------|------|------|
| **AutoTable adapter** | 单向兼容 MP 原生注解，让 AutoTable 能正确处理 MP 标注的实体 | 实现 `AutoTableMetadataAdapter`，读取 MP 注解并转换为 AutoTable 元数据 |
| **mybatis-plus-ext** | 定义一套自有注解（`@Table`/`@Column`/`@ColumnId` 等），同时让 AutoTable 和 MP 都能识别 | 双向适配：实现 `AutoTableMetadataAdapter` 让 AutoTable 识别自定义注解；实现 MP 的 `AnnotationHandler` 让 MP 识别自定义注解 |

### 4.1 分层扩展模式

所有涉及双向兼容的功能，统一采用 **adapter 基础兼容 + ext 扩展兼容** 的分层模式：

| 功能 | adapter 层（基础兼容） | ext 层（扩展兼容） |
|------|----------------------|-------------------|
| **元数据适配** | `MybatisPlusMetadataAdapter` 读取 MP 原生注解 | 继承 Adapter，增加 `@Table`/`@Column`/`@ColumnId` 读取 |
| **类扫描** | `MybatisPlusAutoTableClassScanner` 扫描 `@TableName` | 继承 Scanner，增加 `@Table` 扫描 |
| **动态数据源** | `MybatisPlusDynamicDataSourceHandler` 读取 MP `@DS` | 继承 Handler，增加 `@Table.dsName()` 支持 |

### 4.2 mybatis-plus-ext 待实现清单

以下功能在 mybatis-plus-ext 项目中实现（不在 AutoTable 仓库内）：

1. **自定义注解定义**：`@Table`、`@Column`、`@ColumnId`、`@UniqueIndex`，通过 `@AliasFor` 桥接 MP 和 AutoTable 原生注解
2. **AnnotationHandler 实现**：实现 MP 的 `com.baomidou.mybatisplus.core.handlers.AnnotationHandler` 接口，让 MP 识别自定义注解
3. **AutoTableMetadataAdapter 扩展**：继承 `MybatisPlusMetadataAdapter`，重写方法以读取自定义注解
4. **AutoTableClassScanner 扩展**：继承 `MybatisPlusAutoTableClassScanner`，额外扫描 `@Table` 注解
5. **工具类**：`AnnotatedElementUtilsPlus`（Spring `@AliasFor` 深度合并）、`AnnotationDefaultValueHelper`

> 详细的实现指南见 [MYBATIS-PLUS-EXT-IMPLEMENTATION-GUIDE.md](MYBATIS-PLUS-EXT-IMPLEMENTATION-GUIDE.md)

---

## 五、自动配置注册文件

`auto-table-adapter-mybatis-plus-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：

```
org.dromara.autotable.adapter.mybatisplus.spring.MybatisPlusAutoConfiguration
```

---

## 六、使用方式

### 方式一：使用 MP 原生注解（推荐）

直接引入 adapter starter，已有的 MP 实体无需任何修改：

```xml
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-adapter-mybatis-plus-spring-boot-starter</artifactId>
    <version>最新版本</version>
</dependency>
```

```java
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_name")
    private String username;
}
```

### 方式二：使用 mybatis-plus-ext 自定义注解

额外引入 mybatis-plus-ext，使用 `@Table`/`@Column`/`@ColumnId` 等注解，同时被 AutoTable 和 MP 识别：

```xml
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-adapter-mybatis-plus-spring-boot-starter</artifactId>
    <version>最新版本</version>
</dependency>
<dependency>
    <groupId>com.tangzc</groupId>
    <artifactId>mybatis-plus-ext-autotable</artifactId>
    <version>最新版本</version>
</dependency>
```
