---
title: MybatisPlus
description: 整合 MyBatis-Plus 实现自动建表能力（含 2.6.2 版本升级说明）
---

<div style="display: flex; justify-content: center;">
    <img src="/mpe-logo.png" style="max-height: 150px"/>
</div>

## 概述

AutoTable 提供了对 MyBatis-Plus 的完善支持，让您可以在 MyBatis-Plus 项目中使用 AutoTable 的自动建表功能。

## ⚠️ 重要提示：2.6.2 版本重大变更

> ### 🔴 **请立即查看升级指南！**
> 
> AutoTable 2.6.2 版本对 MyBatis-Plus 适配器进行了**重大重构**，移除了扩展注解体系。
> 
> #### 🎯 您需要做什么？
> 
> - **如果您从未使用过扩展注解**：无需任何操作，继续正常使用标准注解即可。
> - **如果您使用了扩展注解**：**必须立即迁移到标准注解**，否则将无法正常工作。
> 
> #### 📖 迁移资源
> 
> - [完整迁移指南](file:///Users/don/Code/个人/auto-table/MIGRATION-FROM-MYBATIS-PLUS-EXT.md) ← **强烈推荐阅读**
> - [常见问题中的说明](/常见问题/说明#262-升级注意事项)
> - [2.6.2 更新日志](/更新日志#262)

### ❌ 已移除的扩展注解

| 旧版扩展注解 | ✅ 新版标准注解 | 说明 |
|-------------|----------------|------|
| `@Column` | `@AutoColumn` | 多数据库字段配置 |
| `@ColumnId` | `@PrimaryKey` | 主键定义 |
| `@Table` | `@AutoTable` | 表配置 |
| `@UniqueIndex` | `@Index` + `@TableIndex` | 唯一索引 |

### ✅ 保留的标准注解

以下注解完全不受影响，可以继续使用：

- `@AutoTable` - 表配置注解
- `@AutoColumn` - 字段配置注解
- `@PrimaryKey` - 主键注解
- `@Ignore` - 忽略字段
- `@Index` - 索引注解
- `@MysqlColumnUnsigned` - MySQL 无符号数字
- `@MysqlColumnZerofill` - MySQL 补零显示
- 所有其他 `org.dromara.autotable.annotation.*` 包下的标准注解

## 快速开始

### 1. 引入依赖

#### Maven

```xml
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-adapter-mybatis-plus</artifactId>
    <version>{{version}}</version>
</dependency>

<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-adapter-mybatis-plus-spring-boot-starter</artifactId>
    <version>{{version}}</version>
</dependency>
```

#### Gradle

```groovy
implementation 'org.dromara.autotable:auto-table-adapter-mybatis-plus:{{version}}'
implementation 'org.dromara.autotable:auto-table-adapter-mybatis-plus-spring-boot-starter:{{version}}'
```

### 2. 启用自动建表

在启动类上添加 `@EnableAutoTable` 注解：

```java
import org.dromara.autotable.EnableAutoTable;

@SpringBootApplication
@EnableAutoTable
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 定义实体类

使用**标准注解**（而非已移除的扩展注解）：

```java
import org.dromara.autotable.annotation.*;

@Data
@AutoTable(comment = "用户表")
public class User {
    
    @PrimaryKey(autoIncrement = true)
    private Long id;
    
    @ColumnComment("用户名")
    @ColumnNotNull
    private String username;
    
    @ColumnComment("邮箱")
    @Index
    private String email;
}
```

### 4. 配置数据库连接

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auto-table?useSSL=false&serverTimezone=UTC
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

## 运行模式

| 模式 | 说明 | 应用场景 |
|------|------|---------|
| `validate`（默认） | 只校验不修改 | 生产环境安全运行 |
| `update` | 自动更新差异字段 | 开发测试环境 |
| `create` | 创建缺失的表 | 初始化环境或测试 |

配置方式：

```yaml
auto-table:
  mode: update  # validate / update / create
```

或者使用全局配置：

```java
AutoTableGlobalConfig globalConfig = AutoTableGlobalConfig.instance();
globalConfig.setMode(RunMode.UPDATE);
```

## 高级特性

### SQL 执行记录

AutoTable 会自动记录执行的 SQL，支持多种持久化方式：

- **数据库记录**：存储在 auto_table_sql_record 表中
- **文件记录**：保存到指定路径的文件中
- **自定义**：通过实现接口自定义存储逻辑

### 拦截器机制

通过实现 `AutoTableInterceptor` 接口，在表生命周期各阶段插入自定义逻辑：

```java
public class CustomInterceptor implements AutoTableInterceptor {
    
    @Override
    public boolean supports(AutoTableMetadata metadata) {
        return true;
    }
    
    @Override
    public void beforeCreateTable(CreateContext context) {
        // 创建前的处理
    }
}
```

### 事件回调

实现 `AutoTableFinishCallback` 接口监听任务完成：

```java
public class MyCallback implements AutoTableFinishCallback {
    @Override
    public void onComplete(FinishContext context) {
        System.out.println("AutoTable 执行完成");
    }
}
```

## 问题排查

### 表未创建？

1. 检查是否添加了 `@EnableAutoTable`
2. 确认包扫描路径包含实体类
3. 查看日志是否有错误信息
4. 验证数据库连接是否正常

### 字段未更新？

1. 确认运行模式是否为 `update`
2. 检查字段是否被 `@Ignore` 标记
3. 确认字段修饰符不是 `static` 或 `final`

### Invalid value type 错误？

通常是类型映射问题：
1. 检查是否使用了不支持的 Java 类型
2. 可使用 `@ColumnType` 指定数据库类型

## 相关资源

- [完整迁移指南](file:///Users/don/Code/个人/auto-table/MIGRATION-FROM-MYBATIS-PLUS-EXT.md) ← **必读**
- [GitHub 仓库](https://github.com/dromara/auto-table)
- [Gitee 仓库](https://gitee.com/tangzc/auto-table)
- [更新日志](/更新日志)
- [最佳实践](/最佳实践/生产环境部署)

## 社区支持

如有问题或建议，欢迎：
- 提交 Issue：[Gitee Issues](https://gitee.com/tangzc/auto-table/issues)
- 参与讨论：[贡献指南](/社区/贡献指南)

感谢每一位贡献者！🌟
