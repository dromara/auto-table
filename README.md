<p align="center">
  <img src="https://autotable.tangzc.com/logo.png" alt="AutoTable" width="120px" />
</p>

<h1 align="center">AutoTable</h1>
<p align="center"><b>自动维护数据库表结构的 Java 框架</b></p>
<p align="center">你只负责维护实体，数据库的事交给我</p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/org.dromara.autotable/auto-table-core">
    <img src="https://img.shields.io/maven-central/v/org.dromara.autotable/auto-table-core?style=flat-square" alt="Maven Central" />
  </a>
  <a href="https://www.apache.org/licenses/LICENSE-2.0">
    <img src="https://img.shields.io/badge/license-Apache%202-blue?style=flat-square" alt="License" />
  </a>
  <a href="https://autotable.tangzc.com">
    <img src="https://img.shields.io/badge/文档-autotable.tangzc.com-green?style=flat-square" alt="Documentation" />
  </a>
  <a href="https://gitee.com/dromara/auto-table/stargazers">
    <img src="https://gitee.com/dromara/auto-table/badge/star.svg?theme=dark" alt="Gitee Stars" />
  </a>
</p>

---

## ✨ 特性

| | 特性 | 说明 |
|:---:|------|------|
| 🚀 | **开箱即用** | 一个 `@AutoTable` 注解激活，零配置启动 |
| 🔌 | **10 种数据库** | MySQL、PostgreSQL、Oracle、SQL Server、达梦、人大金仓、H2、SQLite、Doris、MariaDB |
| 🌐 | **多库适配** | 同一实体适配多种数据库，通过 `dialect` 属性自动切换字段配置 |
| 🏗️ | **自动建库** | 连数据库都帮你建好，真正的开箱即用 |
| 📦 | **数据初始化** | 建表后自动灌入初始数据，支持 SQL 文件和 Java 方法 |
| 🎯 | **ORM 兼容** | Mybatis-Plus、Mybatis-Flex、原生 Mybatis |
| 💾 | **多数据源** | 一套代码管理多个数据库，自动按数据源分组处理 |
| 🛡️ | **生产友好** | validate / update / create 多模式，SQL 变更可审计追溯 |
| 🔔 | **生命周期** | 10 种事件回调 + 4 种拦截器，完全掌控执行过程 |
| 🔧 | **高度扩展** | 自定义类型映射、支持扩展新数据库策略 |

## 🚀 快速体验

**定义实体：**

```java
@Data
@AutoTable(comment = "用户表")
public class User {
    
    @PrimaryKey(autoIncrement = true)
    private Long id;
    
    @AutoColumn(comment = "用户名", notNull = true)
    private String username;

    @AutoColumn(comment = "邮箱")
    @Index
    private String email;
    
    @ColumnComment("状态")
    @ColumnDefault("0")
    private Integer status;
}
```

**启动应用后，自动生成表结构：**

```sql
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL COMMENT '用户名',
  `email` varchar(255) DEFAULT NULL COMMENT '邮箱',
  `status` int DEFAULT '0',
  PRIMARY KEY (`id`),
  INDEX `auto_idx_user_email` (`email`)
) COMMENT='用户表';
```

**后续修改实体，表结构自动同步！** 新增字段、修改类型、添加索引，全部自动处理。

## 📦 快速开始

### 1. 添加依赖

```xml
<!-- Spring Boot 项目（兼容 Spring Boot 2.x 和 3.x） -->
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-spring-boot-starter</artifactId>
    <version>最新版本</version>
</dependency>
```

> 💡 最新版本请查看 [Maven Central](https://central.sonatype.com/artifact/org.dromara.autotable/auto-table-spring-boot-starter)

### 2. 启用 AutoTable

```java
@EnableAutoTable
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 定义实体

```java
@Data
@AutoTable
public class Article {
    @PrimaryKey(autoIncrement = true)
    private Long id;
    private String title;
    private String content;
}
```

启动项目，表自动创建完成！🎉

## 🌐 多数据库适配

同一个实体，轻松适配多种数据库！通过 `@AutoColumns` + `dialect` 属性，为不同数据库配置不同的字段类型：

```java
@AutoTable
public class Article {
    
    @PrimaryKey(autoIncrement = true)
    private Long id;
    
    // 不同数据库使用不同的大文本类型
    @AutoColumns({
        @AutoColumn(type = "longtext", dialect = "MySQL"),
        @AutoColumn(type = "text", dialect = "PostgreSQL"),
        @AutoColumn(type = "clob", dialect = "Oracle")
    })
    private String content;
    
    // 不同数据库使用不同的字段长度
    @AutoColumns({
        @AutoColumn(length = 100, dialect = "MySQL"),
        @AutoColumn(length = 200, dialect = "PostgreSQL")
    })
    private String summary;
}
```

> 💡 未指定 `dialect` 的配置作为默认值，框架会根据当前数据库自动选择匹配的配置！

## 🔥 高级特性

### 🏗️ 自动建库

开启后，连数据库都不用手动创建，真正的开箱即用：

```yaml
auto-table:
  auto-build-database: true  # 自动创建数据库
```

> 适合产品类项目、开源框架，启动即具备完整的库表结构。

### 📦 数据初始化

建表后自动初始化数据，支持三种方式：

```
src/main/resources/sql/
├── user.sql          # 方式1：自动匹配表名的 SQL 文件
├── _init_.sql        # 全局初始化脚本
└── ...
```

```java
@AutoTable
public class User {
    // 方式2：Java 方法返回初始数据
    @InitDataList
    public static List<User> initData() {
        return Arrays.asList(
            new User("admin", "管理员"),
            new User("guest", "访客")
        );
    }
}

// 方式3：指定 SQL 文件路径
@AutoTable(initSql = "classpath:sql/{dialect}/user.sql")
public class User { ... }
```

### 📝 SQL 审计

所有表结构变更可追溯，支持记录到数据库或文件：

```yaml
auto-table:
  record-sql:
    enable: true
    record-type: db      # db / file / custom
    version: 1.0.0       # 版本号，方便管理
```

> 生产环境可配合 Flyway 使用，自动生成迁移脚本。

### 💾 多数据源

一套实体代码，自动按数据源分组处理：

```java
@AutoTable
@DS("master")  // 主库
public class User { ... }

@AutoTable
@DS("slave")   // 从库
public class Order { ... }
```

### 🔔 生命周期钩子

10 种事件回调 + 4 种拦截器，完全掌控执行过程：

```java
@Component
public class MyCallback implements CreateTableFinishCallback {
    @Override
    public void afterCreateTable(String dialect, TableMetadata metadata) {
        log.info("表 {} 创建完成，开始初始化数据...", metadata.getTableName());
    }
}
```

> 详细文档：[事件回调](https://autotable.tangzc.com/高级功能/事件回调) | [拦截器](https://autotable.tangzc.com/高级功能/拦截器)

## ⚡ 与 JPA 对比

| 特性 | JPA | AutoTable |
|------|:---:|:---------:|
| 自动建表 | ✅ | ✅ |
| 自动建库 | ❌ | ✅ |
| 增量更新结构 | ⚠️ 有限 | ✅ 完整 |
| 索引管理 | ⚠️ 基础 | ✅ 完整 |
| 字段顺序保持 | ❌ | ✅ MySQL |
| Mybatis 生态 | ❌ | ✅ |
| 多数据库支持 | ✅ | ✅ 10种 |
| 多数据库适配 | ❌ | ✅ dialect |
| 数据初始化 | ❌ | ✅ 3种方式 |
| 生产模式（仅校验） | ❌ | ✅ |
| SQL 变更审计 | ❌ | ✅ |
| 生命周期钩子 | ⚠️ 有限 | ✅ 10+4 |

## 💾 支持的数据库

| 数据库 | 测试版本 | 状态 | 维护者 |
|--------|----------|:----:|--------|
| MySQL | 5.7+ | ✅ | |
| MariaDB | 对应 MySQL 版本 | ✅ | |
| PostgreSQL | 15.5 | ✅ | |
| SQLite | 3.35.5 | ✅ | |
| H2 | 2.2.220 | ✅ | |
| Oracle | 11g / 23ai | ✅ | [@lizhian](https://gitee.com/lizhian) |
| Doris | 2.0 | ✅ | [@lizhian](https://gitee.com/lizhian) |
| SQL Server | 2016+ | ✅ | |
| 达梦 | dm8 | ✅ | [@minfc](https://gitee.com/minfc) |
| 人大金仓 | V009R001C002B0014 | ✅ | [@minfc](https://gitee.com/minfc) |

> 🙌 其他数据库暂未支持，期待你的 PR！

## 🔗 生态

### ORM 框架适配

AutoTable 提供开箱即用的 ORM 框架适配模块，引入依赖即可让已有实体获得自动建表能力，**无需修改任何代码**：

| 框架 | 适配包 | 说明 |
|------|--------|------|
| Mybatis-Plus | `auto-table-adapter-mybatis-plus-spring-boot-starter` | 零侵入兼容 `@TableName`/`@TableField`/`@TableId` 等 MP 原生注解 |

```xml
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-adapter-mybatis-plus-spring-boot-starter</artifactId>
    <version>最新版本</version>
</dependency>
```

### ORM 框架扩展

| 框架 | 扩展包 | 说明 |
|------|--------|------|
| Mybatis-Plus | [mybatis-plus-ext](https://gitee.com/dromara/mybatis-plus-ext) | 自定义注解（`@Table`/`@Column`）、免手写 Mapper、数据填充、关联查询等 |
| Mybatis-Flex | [mybatis-flex-ext](https://gitee.com/tangzc/mybatis-flex-ext) | 数据填充（类似 JPA 审计） |

> mybatis-plus-ext 在 adapter 基础上扩展，定义一套自有注解同时被 AutoTable 和 MP 识别，详见 [迁移文档](MIGRATION-FROM-MYBATIS-PLUS-EXT.md)

## 📖 文档

完整文档请访问：**[https://autotable.tangzc.com](https://autotable.tangzc.com)**

- [快速开始](https://autotable.tangzc.com/快速开始/5分钟上手)
- [配置参考](https://autotable.tangzc.com/API参考/配置项)
- [最佳实践](https://autotable.tangzc.com/最佳实践/生产环境部署)

## 💬 交流

<img src="https://autotable.tangzc.com/wechat.png" width="200px" alt="微信" />

## 🙏 致谢

### 贡献者

感谢所有为 AutoTable 做出贡献的开发者！

<a href="https://gitee.com/dromara/auto-table/contributors">
  <img src="https://contrib.rocks/image?repo=dromara/auto-table" />
</a>

---

<p align="center">
  如果 AutoTable 对你有帮助，请给我们一个 ⭐ Star！
</p>
