---
title: SQLite
description: SQLite 数据库适配说明
---

# SQLite

SQLite 是一个轻量级的嵌入式数据库。

## 依赖

```xml
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-strategy-sqlite</artifactId>
    <version>2.5.10</version>
</dependency>
```

## 测试版本

- SQLite 3.35.5

## 连接配置

```yaml
spring:
  datasource:
    url: jdbc:sqlite:./data/test.db
    driver-class-name: org.sqlite.JDBC
```

## 类型映射

SQLite 使用动态类型系统，AutoTable 映射如下：

| Java 类型 | SQLite 类型 |
|-----------|-------------|
| `String` | `text` |
| `Integer` | `integer` |
| `Long` | `integer` |
| `Boolean` | `integer` |
| `Double` | `real` |
| `BigDecimal` | `real` |
| `LocalDateTime` | `text` |
| `LocalDate` | `text` |
| `byte[]` | `blob` |

## 注意事项

### ALTER TABLE 限制

SQLite 的 `ALTER TABLE` 功能有限：
- ✅ 可以添加列
- ❌ 不能删除列
- ❌ 不能修改列类型

AutoTable 在 SQLite 下通过 **重建表** 的方式处理复杂变更：
1. 创建临时表
2. 迁移数据
3. 删除原表
4. 重命名临时表

### 默认值

SQLite 支持函数作为默认值：

```java
@ColumnDefault("CURRENT_TIMESTAMP")
private String createTime;
```

## 适用场景

- 嵌入式应用
- 移动端数据存储
- 小型桌面应用
- 原型开发
