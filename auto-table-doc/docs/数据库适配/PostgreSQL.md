---
title: PostgreSQL
description: PostgreSQL 数据库适配说明
---

# PostgreSQL

## 依赖

```xml
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-strategy-pgsql</artifactId>
    <version>2.5.10</version>
</dependency>
```

## Schema 支持

PostgreSQL 支持 schema，可在 `@AutoTable` 中指定：

```java
@AutoTable(schema = "my_schema", comment = "用户表")
public class User {
}
```

如果 schema 不存在，AutoTable 会自动创建。

## 类型常量

使用 `PgsqlTypeConstant` 指定 PostgreSQL 特有类型：

```java
import org.dromara.autotable.annotation.pgsql.PgsqlTypeConstant;

@ColumnType(PgsqlTypeConstant.TEXT)
private String content;

@ColumnType(PgsqlTypeConstant.JSONB)
private String jsonData;

@ColumnType(PgsqlTypeConstant.UUID)
private String uuid;
```

常用类型：
- 整数：`SMALLINT`, `INTEGER`, `BIGINT`, `SERIAL`, `BIGSERIAL`
- 小数：`REAL`, `DOUBLE_PRECISION`, `NUMERIC`
- 字符串：`CHAR`, `VARCHAR`, `TEXT`
- 日期：`DATE`, `TIME`, `TIMESTAMP`, `TIMESTAMPTZ`
- JSON：`JSON`, `JSONB`
- 其他：`BOOLEAN`, `UUID`, `BYTEA`, `ARRAY`

## 配置项

```yaml
auto-table:
  pgsql:
    # 主键自增方式：always | byDefault（默认）
    pk-auto-increment-type: byDefault
    # 自动建库管理员账号
    admin-user:
    admin-password:
```

### 主键自增方式

| 方式 | 说明 | 建议场景 |
|------|------|----------|
| `always` | 始终由数据库生成，不允许手动指定 | 更安全 |
| `byDefault` | 默认由数据库生成，允许手动指定 | 更灵活（默认） |

## 类型映射

| Java 类型 | PostgreSQL 类型 |
|-----------|----------------|
| `String` | `varchar(255)` |
| `Integer` | `integer` |
| `Long` | `bigint` |
| `Boolean` | `boolean` |
| `Double` | `double precision` |
| `BigDecimal` | `numeric(10,2)` |
| `LocalDateTime` | `timestamp` |
| `LocalDate` | `date` |
| `byte[]` | `bytea` |

## 注意事项

### 标识符大小写

PostgreSQL 对大小写敏感，AutoTable 会使用双引号包裹标识符，保持与实体定义一致。

### 分区表

判断表是否存在时，AutoTable 兼容分区表的情况。
