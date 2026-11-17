---
title: YashanDB
description: YashanDB 数据库适配说明
---

# YashanDB

AutoTable 提供 `auto-table-strategy-yashandb`，用于维护 YashanDB 23.4.7.100 的表结构。该策略面向从 MySQL 迁移的业务模型，支持常见 MySQL 类型别名到 YashanDB DDL 类型的转换。

::: tip MySQL 兼容说明
这里的“MySQL 兼容”指类型和业务模型兼容，不代表使用 MySQL 协议连接。AutoTable 使用 YashanDB 原生 JDBC 驱动和 `jdbc:yasdb` URL；1690 等 MySQL 兼容协议端口不用于该策略的原生 JDBC 连接。
:::

## 依赖

引入策略模块：

```xml
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-strategy-yashandb</artifactId>
    <version>{{version}}</version>
</dependency>
```

策略模块已传递引入 `com.yashandb:yashandb-jdbc:1.10.7`，通常不需要重复声明 JDBC 驱动。如果项目排除了传递依赖，或通过安装包手工管理驱动，请确保应用运行时可以加载 `com.yashandb.jdbc.Driver`。

## 测试基线

- YashanDB：`23.4.7.100`
- JDBC 驱动：`1.10.7`
- Driver：`com.yashandb.jdbc.Driver`
- JDBC URL 前缀：`jdbc:yasdb:`

上述版本已在实际业务系统环境完成表创建、元数据读取、大小写不敏感访问和结构增量同步验证。

## 连接配置

使用 YashanDB 原生 JDBC URL 配置数据源：

```yaml
spring:
  datasource:
    url: jdbc:yasdb://127.0.0.1:1688/hmx_config
    driver-class-name: com.yashandb.jdbc.Driver
    username: JIANMU
    password: your-password
```

将主机、端口、数据库服务名（示例中的 `hmx_config`）、用户名和密码替换为实际环境值。策略会根据 JDBC 产品名或 URL 自动识别 YashanDB。

## 方言选择

通常不需要额外设置，AutoTable 会通过 JDBC 连接自动选择 YashanDB 策略。如果一个数据源中需要显式固定方言，可以在实体上指定：

```java
import org.dromara.autotable.annotation.AutoTable;
import org.dromara.autotable.core.constants.DatabaseDialect;

@AutoTable(dialect = DatabaseDialect.YashanDB)
public class User {
}
```

表级 `dialect` 决定整个数据源使用的策略；同一数据源中的实体应保持一致或全部留空。列级 `@AutoColumn(dialect = ...)` 仍可用于多数据库模型的类型差异配置。

## 类型映射

### Java 类型

| Java 类型 | YashanDB 类型 |
|-----------|---------------|
| `String` | `VARCHAR(255)` |
| `Character` / `char` | `CHAR(1)` |
| `Byte` / `byte` | `TINYINT` |
| `Short` / `short` | `SMALLINT` |
| `Integer` / `int` | `INTEGER` |
| `Long` / `long` / `BigInteger` | `BIGINT` |
| `Boolean` / `boolean` | `TINYINT` |
| `Float` / `float` | `FLOAT` |
| `Double` / `double` | `DOUBLE` |
| `BigDecimal` | `DECIMAL(19,4)` |
| `Date` / `Timestamp` / `LocalDateTime` | `DATETIME` |
| `java.sql.Date` / `LocalDate` | `DATE` |
| `java.sql.Time` / `LocalTime` | `TIME` |
| `byte[]` / `Blob` | `BLOB` |
| `Clob` | `CLOB` |

### MySQL 兼容类型

从 MySQL 模型迁移时，策略会在生成 DDL 和比较结构时规范化以下类型：

| MySQL 类型 | YashanDB DDL 类型 |
|------------|-------------------|
| `INT`、`MEDIUMINT`、`YEAR` | `INTEGER` |
| `DATETIME` | `DATETIME`（系统视图中显示为 `TIMESTAMP(n)`，比较时视为相同） |
| `BIT(1)`、`BOOL`、`BOOLEAN` | `TINYINT`（`BIT(n)`，n>1 保留为 `BIT(n)`） |
| `TEXT`、`TINYTEXT`、`MEDIUMTEXT`、`LONGTEXT`、`JSON` | `CLOB` |
| `TINYBLOB`、`MEDIUMBLOB`、`LONGBLOB` | `BLOB` |
| `DECIMAL`、`NUMERIC` | `NUMBER`（比较时归一化） |

未列出的类型会按 YashanDB 类型名原样规范化；使用数据库不支持的类型前，应先确认目标版本的兼容性。比较现有结构时，目录中的 `UNSIGNED`/`ZEROFILL` 修饰不视为差异。

### 默认值

与 MySQL 策略一致：字符类型的字面量默认值自动加单引号（如 `defaultValue = "EXA"` 生成 `DEFAULT 'EXA'`），日期时间字面量加单引号，`CURRENT_TIMESTAMP` 等函数保持原样；布尔类型的 `true`/`false` 和 MySQL 位字面量 `b'0'`/`b'1'` 转为 `0`/`1`。

## MySQL 兼容模式下的 DDL 约定

本策略面向以 `--mode mysql` 安装的 YashanDB 实例（默认兼容模式不支持 `AUTO_INCREMENT`）。在该模式下实测的约束与策略对应处理：

| 现象 | 策略处理 |
|------|----------|
| `TIMESTAMP NOT NULL` 无默认值时会被隐式加上 `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`，同表第二个此类列报 YAS-09402 | 时间类型生成 `DATETIME`（保留小数秒精度，如 `DATETIME(3)`） |
| 应用用户通常按 MySQL 方式授权（`GRANT ... ON db.*`），不是库的属主，`COMMENT ON` 报 YAS-02213 | 表注释、字段注释内联在 `CREATE TABLE`/`ADD`/`MODIFY` 中；表注释变更用 `ALTER TABLE ... COMMENT '...'`；仅注释变化的字段用 `MODIFY` 完整定义 |
| `DROP INDEX schema.index` 报 YAS-04256 | 生成 `DROP INDEX index ON schema.table` |
| 字符列上的索引在 `ALL_IND_COLUMNS` 中显示为隐藏列（如 `$20001`，实际是 `__MY_COLLATE_SORT(col, n)` 函数索引） | 索引元数据改从 `information_schema.statistics` 读取 |
| 同一列组合不允许建第二个索引（YAS-02043），从 MySQL 迁移的库常已有同列不同名的索引 | 期望索引按名字找不到时，若存在列、顺序、唯一性都相同的索引则直接复用，不再新建 |

另需注意：YashanDB 对含 NULL 的复合唯一索引也判重（MySQL 允许多个 NULL）。依赖“NULL 可重复”的唯一索引需由业务方评估改为普通索引。

## 标识符和大小写

YashanDB 未加引号的表名、字段名和索引名按数据库规则折叠，策略在查询和比较时对对象名使用不区分大小写的匹配。因此实体中的 `user_name`、`USER_NAME` 和数据库目录中的对应名称不会被误判为不同字段。

索引名称会沿用 AutoTable 的命名规则，并清理 YashanDB 未加引号标识符不接受的字符。需要保留大小写或特殊字符时，应显式使用数据库支持的引用标识符并在部署前验证目标数据库行为。

## 自动建表和结构同步

YashanDB 策略支持 AutoTable 的标准运行模式：

```yaml
auto-table:
  mode: update
```

- `update`（默认）：创建缺失的表，增量同步字段、主键、索引和注释。
- `validate`：只比较实体与数据库结构，不执行 DDL，适合生产环境。
- `create`：删除并重建实体表，会清空数据，只应在测试环境使用。
- `none`：停用 AutoTable 结构维护。

默认情况下，`update` 不删除数据库中多余字段；是否删除字段和索引仍由 AutoTable 的通用 `auto-drop-*` 配置控制。

## 元数据读取

策略直接查询 YashanDB 系统目录读取现有结构，不通过扫描实体类推断数据库现状：

- `ALL_TABLES`：表是否存在及表清单
- `ALL_TAB_COLUMNS`、`ALL_COL_COMMENTS`：字段类型、长度、精度、可空、默认值、自增标记和字段注释
- `ALL_TAB_COMMENTS`：表注释
- `ALL_CONSTRAINTS`、`ALL_CONS_COLUMNS`：主键及其列顺序
- `information_schema.statistics`：索引唯一性、列顺序和排序方向（MySQL 兼容模式下字符列索引在 `ALL_IND_COLUMNS` 中显示为隐藏列，不能直接使用）

未指定实体 schema 时，策略使用当前 JDBC 会话 schema；指定 schema 后会对目录查询进行不区分大小写匹配。

## 限制和注意事项

- 本策略只负责表结构维护，不创建 YashanDB 实例、数据库服务、用户或网络监听器。
- 运行时必须提供 YashanDB JDBC 驱动，并确保应用可以访问目标数据库端口。
- `create` 模式会删除并重建表，可能造成数据丢失；生产环境建议使用 `validate` 或经评估的 `update`。
- 从 MySQL 迁移的存量表应先核对字符集、特殊类型、默认表达式和自增列语义，再启用结构更新。
- 复杂的厂商扩展类型或特殊分区、对象权限，需要根据目标 YashanDB 版本单独验证。
