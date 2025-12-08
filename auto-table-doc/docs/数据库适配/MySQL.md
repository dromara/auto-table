---
title: MySQL
description: MySQL 数据库适配说明
---

# MySQL

MySQL 是 AutoTable 支持最完善的数据库。

## 依赖

```xml
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-strategy-mysql</artifactId>
    <version>2.5.10</version>
</dependency>
```

## 专属注解

### @MysqlEngine

指定表引擎：

```java
@AutoTable
@MysqlEngine("InnoDB")  // 或 "MyISAM"
public class User {
}
```

### @MysqlCharset

指定表字符集：

```java
@AutoTable
@MysqlCharset(charset = "utf8mb4", collate = "utf8mb4_general_ci")
public class User {
}
```

### @MysqlColumnCharset

指定列字符集：

```java
@MysqlColumnCharset(value = "utf8mb4", collate = "utf8mb4_bin")
private String name;
```

### @MysqlColumnUnsigned

无符号数字（范围从 0 开始）：

```java
@MysqlColumnUnsigned
private Integer age;  // 0 ~ 4294967295
```

### @MysqlColumnZerofill

数字前置补零：

```java
@MysqlColumnZerofill
@ColumnType(length = 10)
private Integer orderNo;  // 12345 → 0000012345
```

## 类型常量

使用 `MysqlTypeConstant` 指定 MySQL 特有类型：

```java
import org.dromara.autotable.annotation.mysql.MysqlTypeConstant;

@ColumnType(MysqlTypeConstant.LONGTEXT)
private String content;

@ColumnType(MysqlTypeConstant.JSON)
private String jsonData;

@ColumnType(MysqlTypeConstant.TINYINT)
private Integer status;
```

常用类型：
- 整数：`TINYINT`, `SMALLINT`, `MEDIUMINT`, `INT`, `BIGINT`
- 小数：`FLOAT`, `DOUBLE`, `DECIMAL`
- 字符串：`CHAR`, `VARCHAR`, `TEXT`, `MEDIUMTEXT`, `LONGTEXT`
- 日期：`DATE`, `DATETIME`, `TIMESTAMP`, `TIME`, `YEAR`
- 二进制：`BLOB`, `MEDIUMBLOB`, `LONGBLOB`
- 其他：`JSON`, `ENUM`, `SET`

## 配置项

```yaml
auto-table:
  mysql:
    # 表默认字符集
    table-default-charset: utf8mb4
    # 表默认排序规则
    table-default-collation: utf8mb4_general_ci
    # 列默认字符集
    column-default-charset: 
    # 列默认排序规则
    column-default-collation:
    # 自动建库管理员账号（可选）
    admin-user:
    admin-password:
    # 修改表时分离删除 SQL（适用于云数据库安全限制）
    alter-table-separate-drop: false
```

## 字段顺序

MySQL 策略支持保持字段顺序与实体定义一致（使用 `AFTER` 关键字）。

## MariaDB

MariaDB 使用 MySQL 策略，连接协议使用 `jdbc:mysql://`。

## 常见问题

### 字符集问题

如果出现中文乱码，建议配置：

```yaml
auto-table:
  mysql:
    table-default-charset: utf8mb4
    table-default-collation: utf8mb4_general_ci
```

### 云数据库限制

部分云数据库对 `ALTER TABLE` 的 `DROP` 操作有安全限制，可开启：

```yaml
auto-table:
  mysql:
    alter-table-separate-drop: true
```
