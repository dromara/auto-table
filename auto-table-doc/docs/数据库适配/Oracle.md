---
title: Oracle
description: Oracle 数据库适配说明
---

# Oracle

<Badge type="warning" text="^2.5.0" />

## 依赖

```xml
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-strategy-oracle</artifactId>
    <version>2.5.10</version>
</dependency>
```

## 测试版本

- Oracle 11g
- Oracle 23ai

## 配置项

```yaml
auto-table:
  oracle:
    # 自动建库（用户）管理员账号
    admin-user: system
    admin-password: 
```

## 类型映射

| Java 类型 | Oracle 类型 |
|-----------|-------------|
| `String` | `varchar2(255)` |
| `Integer` | `number(10)` |
| `Long` | `number(19)` |
| `Boolean` | `number(1)` |
| `Double` | `number` |
| `BigDecimal` | `number(10,2)` |
| `LocalDateTime` | `timestamp` |
| `LocalDate` | `date` |
| `byte[]` | `blob` |

## 自动建用户

Oracle 中，"建库" 实际上是创建用户。配置管理员账号后可自动创建：

```yaml
auto-table:
  auto-build-database: true
  oracle:
    admin-user: system
    admin-password: your_password
```

## 注意事项

### 索引名称长度

Oracle 索引名称最大长度为 **30 字符**（其他数据库为 63 字符）。

AutoTable 会自动处理，超长时使用哈希值缩短名称。

### 标识符

Oracle 使用双引号 `"` 包裹标识符。
