---
title: 框架集成
description: AutoTable 与第三方框架集成
---

# 框架集成

AutoTable 通过接口抽象，可以优雅地与各种 ORM 框架集成。

## 兼容性总览

| 框架 | 兼容性 | 适配方式 |
|------|:------:|----------|
| Mybatis | ✅ 完全兼容 | 原生支持 |
| [Mybatis-Plus](/框架集成/Mybatis-Plus) | ✅ 完全兼容 | 适配器 |
| [Mybatis-Flex](/框架集成/Mybatis-Flex) | ✅ 完全兼容 | 适配器 |
| JPA/Hibernate | ❌ 不兼容 | - |
| JOOQ | ⚠️ 未测试 | - |

## 功能对照

| 功能 | Mybatis-Plus | Mybatis-Flex | 说明 |
|------|:------------:|:------------:|------|
| 表名映射 | ✅ `@TableName` | ✅ `@Table` | 自动识别 |
| 字段名映射 | ✅ `@TableField` | ✅ `@Column` | 自动识别 |
| 主键识别 | ✅ `@TableId` | ✅ `@Id` | 自动识别 |
| 主键策略 | ✅ 支持 | ✅ 支持 | 自动识别 |
| 忽略字段 | ✅ `exist=false` | ✅ `ignore=true` | 自动识别 |
| 枚举处理 | ✅ `@EnumValue` | ✅ 支持 | 自动识别 |
| 多数据源 | ✅ 支持 | ✅ 支持 | 自动识别 |

## 使用方式

### 与 Mybatis-Plus 集成

推荐使用 [mybatis-plus-ext](https://gitee.com/dromara/mybatis-plus-ext)，已内置 AutoTable 支持。

或手动引入适配器：

```xml
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-adapter-mybatis-plus</artifactId>
    <version>2.5.10</version>
</dependency>
```

### 与 Mybatis-Flex 集成

推荐使用 [mybatis-flex-ext](https://gitee.com/tangzc/mybatis-flex-ext)，已内置 AutoTable 支持。

## 注解优先级

当同时使用 AutoTable 注解和 ORM 框架注解时：

1. **AutoTable 注解优先**：显式配置的 AutoTable 注解具有最高优先级
2. **ORM 注解次之**：如果没有 AutoTable 注解，则读取 ORM 框架注解
3. **默认规则兜底**：都没有时使用默认映射规则

示例：

```java
@Data
@TableName("sys_user")  // Mybatis-Plus 表名
@AutoTable(comment = "用户表")  // AutoTable 激活 + 注释
public class User {
    
    @TableId(type = IdType.AUTO)  // Mybatis-Plus 主键策略
    private Long id;
    
    @TableField("user_name")  // Mybatis-Plus 字段名
    @ColumnComment("用户名")   // AutoTable 注释
    private String username;
}
```

## SpringDoc 集成

AutoTable 支持 SpringDoc（Swagger），可自动从 AutoTable 注解生成 API 文档描述。

详见 [SpringDoc 集成](/框架集成/SpringDoc)。
