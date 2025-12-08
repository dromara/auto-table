---
title: 数据库适配
description: AutoTable 支持的数据库一览
---

# 数据库适配

AutoTable 通过 **策略模式 + SPI 机制** 支持多种数据库，每种数据库都有独立的策略实现。

## 支持的数据库

| 数据库 | 策略模块 | 测试版本 | 状态 |
|--------|----------|----------|------|
| [MySQL](/数据库适配/MySQL) | `auto-table-strategy-mysql` | 5.7+ | ✅ 稳定 |
| MariaDB | `auto-table-strategy-mysql` | 对应 MySQL | ✅ 稳定 |
| [PostgreSQL](/数据库适配/PostgreSQL) | `auto-table-strategy-pgsql` | 15.5 | ✅ 稳定 |
| [Oracle](/数据库适配/Oracle) | `auto-table-strategy-oracle` | 11g / 23ai | ✅ 稳定 |
| [达梦](/数据库适配/达梦) | `auto-table-strategy-dm` | dm8 | ✅ 稳定 |
| [人大金仓](/数据库适配/人大金仓) | `auto-table-strategy-kingbase` | V009R001C002B0014 | ✅ 稳定 |
| [H2](/数据库适配/H2) | `auto-table-strategy-h2` | 2.2.220 | ✅ 稳定 |
| [SQLite](/数据库适配/SQLite) | `auto-table-strategy-sqlite` | 3.35.5 | ✅ 稳定 |
| [Doris](/数据库适配/Doris) | `auto-table-strategy-doris` | 2.0 | ✅ 稳定 |

## 按需引入

默认情况下，`auto-table-spring-boot-starter` 不包含具体数据库策略，需要按需引入：

```xml
<!-- 引入所有策略 -->
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-strategy-all</artifactId>
    <version>2.5.10</version>
</dependency>

<!-- 或单独引入 -->
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-strategy-mysql</artifactId>
    <version>2.5.10</version>
</dependency>
```

## 自动识别

AutoTable 会根据 JDBC 连接自动识别数据库类型，无需手动配置。

也可以在实体上显式指定：

```java
@AutoTable(dialect = "MySQL")  // 强制使用 MySQL 策略
public class User {
    // ...
}
```

## 扩展新数据库

如需支持新的数据库，可以实现 `IStrategy` 接口并通过 SPI 注册。

详见 [自定义策略](/高级功能/自定义策略)。
