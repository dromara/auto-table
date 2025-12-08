---
title: SpringDoc 集成
description: AutoTable 与 SpringDoc/Swagger 集成
---

# SpringDoc 集成

AutoTable 提供 SpringDoc 支持模块，可以在不写 Swagger 注解的情况下，自动从 AutoTable 注解生成 API 文档描述。

## 依赖

```xml
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-support-springdoc</artifactId>
    <version>2.5.10</version>
</dependency>
```

## 效果

### 实体定义

```java
@Data
@AutoTable(comment = "用户信息")
public class User {
    
    @PrimaryKey(autoIncrement = true)
    @ColumnComment("用户ID")
    private Long id;
    
    @ColumnComment("用户名")
    @ColumnNotNull
    private String username;
    
    @ColumnComment("邮箱地址")
    private String email;
}
```

### Swagger UI 展示

无需添加 `@Schema` 注解，Swagger UI 会自动显示：

- **User** - 用户信息
  - `id` (integer, required) - 用户ID
  - `username` (string, required) - 用户名
  - `email` (string) - 邮箱地址

## 映射规则

| AutoTable 注解 | SpringDoc 效果 |
|----------------|----------------|
| `@AutoTable(comment=)` | Schema 类描述 |
| `@ColumnComment` | Schema 字段描述 |
| `@ColumnNotNull` | required = true |
| `@ColumnType(length=)` | maxLength |

## 配置

默认启用，无需额外配置。如需禁用：

```yaml
auto-table:
  support:
    springdoc:
      enabled: false
```
