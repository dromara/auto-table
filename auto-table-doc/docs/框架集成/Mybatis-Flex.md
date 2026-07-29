---
title: MybatisFlex
description: 整合 Mybatis-Flex 实现自动建表能力
---

<div style="display: flex; justify-content: center;">
    <img src="/mfe-logo.png" style="max-height: 150px"/>
</div>

## 概述

AutoTable 提供了完善的 Mybatis-Flex 支持，让您可以在 Mybatis-Flex 项目中使用 AutoTable 的自动建表功能。

## 快速开始

### 1. 引入依赖

#### Maven

```xml
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-adapter-mybatis-flex</artifactId>
    <version>{{version}}</version>
</dependency>

<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-adapter-mybatis-flex-spring-boot-starter</artifactId>
    <version>{{version}}</version>
</dependency>
```

#### Gradle

```groovy
implementation 'org.dromara.autotable:auto-table-adapter-mybatis-flex:{{version}}'
implementation 'org.dromara.autotable:auto-table-adapter-mybatis-flex-spring-boot-starter:{{version}}'
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

```java
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.annotation.PrimaryKey;
import org.dromara.autotable.annotation.ColumnComment;
import org.dromara.autotable.annotation.ColumnName;

@Data
@AutoTable(comment = "用户表")
public class User {
    
    @PrimaryKey(autoIncrement = true)
    private Long id;
    
    @ColumnComment("用户名")
    @ColumnName("username")
    private String username;
    
    @ColumnComment("邮箱")
    @ColumnName("email")
    private String email;
}
```

### 4. 配置数据库连接

确保正确配置了数据库连接：

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

```java
@Override
public void executeSqlRecord(ExecuteSqlContext context) {
    // 保存 SQL 到数据库或文件
}
```

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
    
    @Override
    public void afterUpdateTable(UpdateContext context) {
        // 更新后的处理
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

注册回调：

```java
AutoTableGlobalConfig config = AutoTableGlobalConfig.instance();
config.registerFinishCallback(new MyCallback());
```

## 自定义类型映射

扩展 Java 类型与数据库类型的映射关系：

```java
converter.customFieldTypeHandler((field, clazz) -> {
    if (field.getType() == Date.class) {
        return DatabaseType.VARCHAR + "(20)";
    }
    return null;
});
```

## 注意事项

### AutoTable 2.6.2 重要变更

> 💡 **关于 AutoTable 2.6.2 的重大变更**
> 
> 在 2.6.2 版本中，AutoTable 对 MyBatis-Plus 适配器进行了重构，移除了扩展注解体系。**如果您使用的是 MyBatis-Plus，请务必阅读 [MyBatis-Plus 适配器升级说明](/框架集成/Mybatis-Plus) 和 [完整迁移指南](file:///Users/don/Code/个人/auto-table/MIGRATION-FROM-MYBATIS-PLUS-EXT.md)**
> 
> #### 🎯 MyBatis-Flex 用户
> 
> 您无需担心！MyBatis-Flex 适配器从一开始就使用标准注解（如 `@AutoTable`、`@AutoColumn` 等），不存在兼容性问题。

## 多数据源场景

在多数据源环境下，每个数据源的 SQL 脚本路径为：
```
classpath:sql/[dsName]/_init_.sql
```

## Schema 支持

对于 PostgreSQL、Oracle、Doris 等多 Schema 数据库，可通过 `@AutoTable(schema = "myschema")` 指定 Schema：

```java
@AutoTable(schema = "public", comment = "用户表")
public class User {
    // ...
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

- [GitHub 仓库](https://github.com/dromara/auto-table)
- [Gitee 仓库](https://gitee.com/tangzc/auto-table)
- [更新日志](/更新日志)
- [最佳实践](/最佳实践/生产环境部署)

## 社区支持

如有问题或建议，欢迎：
- 提交 Issue：[Gitee Issues](https://gitee.com/tangzc/auto-table/issues)
- 参与讨论：[贡献指南](/社区/贡献指南)

感谢每一位贡献者！🌟
