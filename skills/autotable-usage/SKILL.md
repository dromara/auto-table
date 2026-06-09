---
name: autotable-usage
description: AutoTable 框架完整使用指南。基于 JDBC 的自动建表框架（非 MyBatis 依赖），支持 9 种数据库。覆盖注解配置、多数据库适配、生命周期钩子、SPI 扩展、SQL 审计、数据初始化、多数据源等。当用户询问 AutoTable 的用法、注解、配置、扩展、最佳实践时激活。
related_skills: []
---

# AutoTable 完整使用指南

AutoTable 是一个**基于 JDBC**（commons-dbutils）的自动维护数据库表结构的 Java 框架，不依赖任何 ORM 框架，可与 MyBatis/MyBatis-Plus/MyBatis-Flex/JPA 等共存。

## 快速定位

| 用户问题 | 参考文档 |
|---------|---------|
| 如何安装、启用、定义实体 | references/quick-start.md |
| 某个注解的完整属性、用法 | references/annotation-reference.md |
| Spring Boot / Solon 配置项 | references/configuration.md |
| 生命周期回调、拦截器 | references/lifecycle.md |
| MySQL / Doris / PostgreSQL 专属功能 | references/database-specific.md |
| 类型映射、自定义类型转换 | references/type-mapping.md |
| 多数据源配置 | references/multi-datasource.md |
| 生产部署、测试、最佳实践 | references/best-practices.md |
| 内部架构、SPI 扩展、自定义策略 | references/architecture.md |

## 核心概念速查

### 运行模式（RunMode）

| 模式 | 说明 | 适用场景 |
|------|------|---------|
| `update` | 增量更新表结构（默认） | 开发环境 |
| `validate` | 仅校验，不一致则报错 | 生产环境 |
| `create` | 删表重建（⚠️ 丢数据） | 测试环境 |
| `none` | 不做任何处理 | 禁用 |

### 支持数据库

MySQL、MariaDB、PostgreSQL、Oracle、SQLite、H2、Doris、达梦（DM）、人大金仓（KingBase）

### 注解层级

```
@AutoTable (类级) → 声明建表
├── @TableIndex (类级，组合索引，可重复)
├── @MysqlEngine / @MysqlCharset (MySQL 类级)
├── @MysqlTableFullTextIndex (MySQL 全文索引)
├── @DorisTable (Doris 类级)
│
@AutoColumn (字段级) → 聚合配置
├── @ColumnName / @ColumnType / @ColumnNotNull
├── @ColumnDefault / @ColumnComment
├── @PrimaryKey / @AutoIncrement
├── @Index (字段级索引)
├── @Ignore (忽略字段)
├── @MysqlFullTextIndex / @MysqlColumnCharset 等
├── @DorisColumn
└── @AutoColumns (多数据库适配容器)
```

### 最小依赖

```xml
<!-- Spring Boot（兼容 2.x 和 3.x），版本号请查询 Maven Central 获取 -->
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-spring-boot-starter</artifactId>
    <version>${最新版本}</version>
</dependency>

<!-- Solon -->
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-solon-plugin</artifactId>
    <version>${最新版本}</version>
</dependency>
```

### 启用方式

```java
// Spring Boot
@EnableAutoTable
@SpringBootApplication
public class Application { ... }

// Solon
@EnableAutoTable  // 标注在 @SolonMain 类上
public class Application { ... }
```

### 最小实体

```java
@Data
@AutoTable(comment = "用户表")
public class User {
    @PrimaryKey(autoIncrement = true)
    private Long id;

    @AutoColumn(comment = "用户名", notNull = true)
    private String username;
}
```

## 版本获取

**禁止写死版本号。** 每次生成依赖配置前，必须访问以下 URL 获取最新版本：

https://central.sonatype.com/artifact/org.dromara.autotable/auto-table-core/versions

在示例中使用 `${最新版本}` 占位符，并提示用户自行查询替换。

## 关键陷阱

1. **基于 JDBC，非 MyBatis**：pom.xml 旧版 description 写的"Mybatis下"是历史遗留，实际核心依赖 commons-dbutils
2. **`create` 模式会删表**：测试完务必切回 `update`
3. **`auto-drop-table/column`** 生产环境必须关闭，会造成数据丢失
4. **索引名前缀**：自动生成 `auto_idx_` 前缀，超长时 hash 处理
5. **父类字段排序**：`super-insert-position` 默认 `after`（父类字段在子类后面），不符合直觉时可改为 `before`
6. **MySQL 云数据库**：部分云厂商禁止 ALTER TABLE 中混用 DROP 和 ADD，需开启 `mysql.alter-table-separate-drop`
7. **Doris 大表更新**：默认超过 1GB 跳过更新（`doris.update-limit-table-data-length`），需按需调整
