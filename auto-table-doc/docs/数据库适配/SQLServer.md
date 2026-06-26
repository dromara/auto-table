---
title: SQL Server
description: SQL Server 数据库适配说明
---

# SQL Server

<Badge type="warning" text="^2.6.0" />

AutoTable 提供 SQL Server 数据库策略，支持建表、改表、索引、注释的自动维护。

## 依赖

```xml
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-strategy-sqlserver</artifactId>
    <version>{{version}}</version>
</dependency>
```

## 测试版本

- 驱动：`mssql-jdbc 12.6.1.jre8`
- 兼容 SQL Server 2016+（删表使用 `DROP TABLE IF EXISTS` 语法）

## 连接配置

```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=mydb;encrypt=false;trustServerCertificate=true
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
    username: SA
    password: your_password
```

::: tip 连接参数
- `databaseName` 指定目标数据库
- `encrypt=false;trustServerCertificate=true` 用于跳过开发环境下的证书校验
:::

## Schema 支持

SQL Server 支持 Schema，可在 `@AutoTable` 中指定：

```java
@AutoTable(schema = "dbo", comment = "用户表")
public class User {
}
```

未显式指定时，默认使用当前连接的 Schema（通常为 `dbo`）。若 Schema 不存在，AutoTable 会自动执行 `CREATE SCHEMA` 创建。

## 类型常量

SQL Server 策略未提供专属注解，通过 `SqlServerTypeConstant` 指定特有类型：

```java
import org.dromara.autotable.annotation.sqlserver.SqlServerTypeConstant;

@ColumnType(SqlServerTypeConstant.NVARCHAR)
private String name;

@ColumnType(SqlServerTypeConstant.DATETIME2)
private LocalDateTime createTime;

@ColumnType(value = SqlServerTypeConstant.DECIMAL, length = 18, decimalLength = 4)
private BigDecimal price;
```

常用类型：

- 整数：`BIT`, `TINYINT`, `SMALLINT`, `INT`, `BIGINT`
- 小数：`REAL`, `FLOAT`, `DECIMAL`, `NUMERIC`
- 字符串：`CHAR`, `VARCHAR`, `NCHAR`, `NVARCHAR`, `TEXT`, `NTEXT`
- 日期：`DATE`, `TIME`, `DATETIME`, `DATETIME2`, `SMALLDATETIME`
- 二进制：`BINARY`, `VARBINARY`, `IMAGE`

## 类型映射

| Java 类型 | SQL Server 类型 |
|-----------|----------------|
| `String` | `nvarchar(255)` |
| `Character` / `char` | `nchar(255)` |
| `Byte` / `byte` | `tinyint` |
| `Short` / `short` | `smallint` |
| `Integer` / `int` | `int` |
| `Long` / `long` / `BigInteger` | `bigint` |
| `Boolean` / `boolean` | `bit` |
| `Float` / `float` | `real` |
| `Double` / `double` | `float` |
| `BigDecimal` | `decimal(19,4)` |
| `LocalDateTime` / `Date` / `Timestamp` | `datetime2` |
| `LocalDate` / `java.sql.Date` | `date` |
| `LocalTime` / `java.sql.Time` / `OffsetTime` | `time` |

::: warning 关于 byte[]
SQL Server 策略**未为 `byte[]` 提供默认映射**。如需存储二进制数据，请通过 `@ColumnType(SqlServerTypeConstant.VARBINARY)` 显式指定。
:::

## 主键自增

SQL Server 使用 `IDENTITY(1,1)` 实现自增：

```java
@PrimaryKey(autoIncrement = true)
private Long id;
```

生成的列定义：`[id] bigint IDENTITY(1,1) NOT NULL`。自增列会自动设为非空，且忽略 `NOT NULL` / `DEFAULT` 配置。

## 标识符

SQL Server 使用方括号 `[]` 包裹表名、列名、索引名等标识符，名称内含 `]` 时转义为 `]]`。AutoTable 统一通过策略接口处理，无需手动包裹。

## 索引

支持普通索引和唯一索引：

```java
@Index  // 普通索引
private String email;

@Index(unique = true)  // 唯一索引
private String username;
```

SQL Server 索引名称最大长度为 **128 字符**（其他多数数据库为 63 字符），超长时 AutoTable 会自动用哈希值缩短名称。

索引比较时，仅当实体显式指定 `sort` 时才比较排序方向（`ASC` / `DESC`），避免无意义的频繁更新。

## 默认值

支持列默认值，SQL Server 通过约束（`DEFAULT` 约束）实现：

```java
@ColumnDefault("0")
private Integer status;

@ColumnDefault("getdate()")
private LocalDateTime createTime;
```

## 配置项

SQL Server 策略**无专属配置项**，复用全局配置。常用全局配置：

```yaml
auto-table:
  # 自动建库（创建 database）
  auto-build-database: false
  # 自动删除实体中不存在的列（默认关闭）
  auto-drop-column: false
  # 自动删除以 index-prefix 开头的多余索引（默认开启）
  auto-drop-index: true
```

## 自动建库

SQL Server 的"建库"即创建 database。开启 `auto-build-database` 后，AutoTable 会从 JDBC URL 解析 `databaseName`，将连接切换到 `master` 库后执行 `CREATE DATABASE`。

::: warning 关于建库账号
首版未引入独立的 `admin-user` / `admin-password` 配置，建库时**直接复用数据源账号**。若该账号无 `master` 库的建库权限，建库会失败，但不影响已存在库的表结构维护。
:::

## 结构同步

修改表时，AutoTable 通过以下 SQL 维护表结构：

- 新增列：`ALTER TABLE ... ADD`
- 修改列类型/非空：`ALTER TABLE ... ALTER COLUMN`
- 重命名列：`EXEC sp_rename`
- 默认值变更：先 `DROP CONSTRAINT` 再 `ADD DEFAULT ... FOR`
- 主键变更：`ADD PRIMARY KEY` / `DROP CONSTRAINT`
- 表/列/索引注释：`sp_addextendedproperty` / `sp_updateextendedproperty`

## 注意事项

### Unicode 字符串

SQL Server 字符串默认使用 `NVARCHAR` / `NCHAR`（Unicode），可正常存储中文等多字节字符，无需额外配置字符集。

### 注释维护

SQL Server 没有 MySQL 那样的 `COMMENT` 语法，表与列注释通过 `sp_addextendedproperty` 系统存储过程维护，AutoTable 已自动处理。

### 标识符大小写

SQL Server 默认对标识符大小写不敏感（取决于数据库排序规则）。AutoTable 使用方括号包裹标识符，保持与实体定义一致。
