---
name: autotable-usage
description: 当用户询问 AutoTable 的使用方法、注解配置、如何定义实体类建表、@AutoTable / @AutoColumn / @Index / @PrimaryKey 等注解用法、多数据库适配、Doris/MySQL 专属注解、数据库类型常量、Spring Boot 配置、自动建表、表结构维护时，激活本 skill。帮助用户正确使用 AutoTable 框架进行数据库表结构的自动维护。
---

# AutoTable 用法指南

AutoTable 是一个自动维护数据库表结构的 Java 框架。你只需维护实体类，数据库表结构会自动同步。

## 核心依赖

Spring Boot 项目：
```xml
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-spring-boot-starter</artifactId>
    <version>最新版本</version>
</dependency>
```

## 快速开始

### 1. 启用 AutoTable

```java
import org.dromara.autotable.springboot.EnableAutoTable;

@EnableAutoTable
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 2. 定义实体

```java
import org.dromara.autotable.annotation.*;

@Data
@AutoTable(comment = "用户表")
public class User {

    @PrimaryKey(autoIncrement = true)
    private Long id;

    @AutoColumn(comment = "用户名", notNull = true)
    private String username;

    @AutoColumn(comment = "邮箱")
    @Index
    private String email;

    @ColumnComment("状态")
    @ColumnDefault("0")
    private Integer status;
}
```

## 基础注解详解

### 表级注解

#### `@AutoTable`

标注在类上，声明该类需要自动建表。

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `value` | 表名，为空取类名 | `""` |
| `schema` | 表 schema | `""` |
| `comment` | 表注释，为空取类名 | `""` |
| `dialect` | 方言，参考 `DatabaseDialect` 常量 | `""` |
| `initSql` | 初始化 SQL 路径 | `""` |

```java
@AutoTable(value = "sys_user", comment = "用户表", schema = "public")
public class User { ... }
```

### 字段级注解

#### `@AutoColumn`

定义字段属性（最常用）。

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `value` | 列名 | `""`（取属性名） |
| `type` | 字段类型 | `""`（自动推断） |
| `length` | 长度 | `-1`（不限制） |
| `decimalLength` | 小数位数 | `-1` |
| `notNull` | 是否非空 | `false` |
| `defaultValue` | 默认值 | `""` |
| `defaultValueType` | 默认值类型（`UNDEFINED`/`NULL`/`EMPTY_STRING`） | `UNDEFINED` |
| `comment` | 注释 | `""` |
| `sort` | 排序（1=第一个，-1=最后一个） | `0` |
| `dialect` | 方言 | `""` |

```java
@AutoColumn(type = "varchar", length = 50, notNull = true, comment = "用户名")
private String username;
```

#### `@PrimaryKey`

标注主键。

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `autoIncrement` | 是否自增 | `false` |

```java
@PrimaryKey(autoIncrement = true)
private Long id;
```

#### `@AutoIncrement`

标注字段为自增（可替代 `@PrimaryKey(autoIncrement = true)`）。

```java
@AutoIncrement
private Long id;
```

#### `@Index`

为字段添加索引。

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `name` | 索引名 | `""`（自动生成） |
| `type` | 索引类型（`NORMAL`/`UNIQUE`/`FULL_TEXT`） | `NORMAL` |
| `method` | 索引方法（btree/hash） | `""` |
| `comment` | 索引注释 | `""` |

```java
@Index(type = IndexTypeEnum.UNIQUE, comment = "唯一索引")
private String email;
```

#### `@TableIndex`

在类上定义组合索引（支持重复注解）。

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `name` | 索引名 | `""` |
| `type` | 索引类型 | `NORMAL` |
| `method` | 索引方法 | `""` |
| `fields` | 字段名数组（按顺序） | `{}` |
| `indexFields` | 带排序的字段配置（优先级更高） | `{}` |
| `comment` | 注释 | `""` |

```java
@AutoTable
@TableIndex(name = "idx_name_age", fields = {"username", "age"}, type = IndexTypeEnum.NORMAL)
public class User { ... }
```

#### `@ColumnDefault`

设置默认值。

```java
@ColumnDefault("0")
private Integer status;

@ColumnDefault(type = DefaultValueEnum.EMPTY_STRING)
private String remark;
```

#### `@ColumnType`

设置字段类型和长度。

```java
@ColumnType(value = "varchar", length = 200)
private String description;
```

#### `@ColumnComment`

设置字段注释（可替代 `@AutoColumn.comment`）。

```java
@ColumnComment("用户状态：0-禁用，1-启用")
private Integer status;
```

#### `@ColumnName`

指定列名。

```java
@ColumnName("user_name")
private String userName;
```

#### `@ColumnNotNull`

指定字段非空。

```java
@ColumnNotNull
private String username;
```

#### `@Ignore`

忽略字段，不映射到数据库。

```java
@Ignore
private String tempField;
```

#### `@AutoColumns`

多策略自定义数据库字段，用于多数据库适配（详见下文"多数据库适配"章节）。

```java
@AutoColumns({
    @AutoColumn(type = "longtext", dialect = "MySQL"),
    @AutoColumn(type = "text", dialect = "PostgreSQL")
})
private String content;
```

### 枚举说明

#### `IndexTypeEnum`

索引类型：
- `NORMAL`：普通索引
- `UNIQUE`：唯一索引

#### `IndexSortTypeEnum`

索引字段排序方式：
- `ASC`：正序
- `DESC`：倒序

#### `DefaultValueEnum`

默认值类型：
- `UNDEFINED`：未定义（默认）
- `NULL`：NULL 值
- `EMPTY_STRING`：空字符串

## MySQL 专属注解

| 注解 | 作用 | 示例 |
|------|------|------|
| `@MysqlEngine("InnoDB")` | 指定存储引擎 | `@MysqlEngine("InnoDB")` |
| `@MysqlCharset("utf8mb4")` | 指定表字符集 | `@MysqlCharset("utf8mb4")` |
| `@MysqlColumnCharset("utf8mb4")` | 指定列字符集 | `@MysqlColumnCharset("utf8mb4")` |
| `@MysqlColumnZerofill` | 无符号零填充 | `@MysqlColumnZerofill` |
| `@MysqlColumnUnsigned` | 无符号 | `@MysqlColumnUnsigned` |

```java
@AutoTable
@MysqlEngine("InnoDB")
@MysqlCharset("utf8mb4")
public class Article {

    @PrimaryKey(autoIncrement = true)
    private Long id;

    @AutoColumn(comment = "标题")
    private String title;

    @AutoColumn(comment = "内容")
    @MysqlFullTextIndex
    private String content;
}
```

#### `@MysqlFullTextIndex`（字段级）

指定 MySQL 字段的全文索引。

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `name` | 索引名 | `""`（自动生成） |
| `comment` | 索引注释 | `""` |
| `parser` | 分词器，例如 `ngram` | `""` |

```java
@MysqlFullTextIndex(parser = "ngram", comment = "全文索引")
private String content;
```

#### `@MysqlTableFullTextIndex`（表级）

指定 MySQL 表级别的全文索引，可组合多个字段。

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `name` | 索引名 | `""` |
| `comment` | 索引注释 | `""` |
| `parser` | 分词器 | `""` |
| `fields` | 字段名数组 | `{}` |

```java
@AutoTable
@MysqlTableFullTextIndex(name = "ft_title_content", fields = {"title", "content"}, parser = "ngram")
public class Article { ... }
```

## Doris 专属注解

#### `@DorisTable`

Doris 表级配置。

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `indexes` | 索引配置数组 | `{}` |
| `engine` | 表引擎 | `"olap"` |
| `duplicate_key` | 明细模型 key | `{}` |
| `unique_key` | 主键模型 key | `{}` |
| `aggregate_key` | 聚合模型 key | `{}` |
| `auto_partition` | 是否开启自动分区 | `false` |
| `auto_partition_time_unit` | 自动分区时间单位 | `none` |
| `partition_by_range` | range 分区列 | `{}` |
| `partition_by_list` | list 分区列 | `{}` |
| `partitions` | 手动分区配置 | `{}` |
| `dynamic_partition` | 动态分区配置 | 默认关闭 |
| `distributed_by_hash` | hash 分桶列 | `{}` |
| `distributed_buckets` | 分桶数量 | `-1`（自动） |
| `rollup` | 物化视图配置 | `{}` |
| `properties` | 建表 properties | `{}` |

```java
@DorisTable(
    duplicate_key = {"id", "name"},
    distributed_by_hash = {"id"},
    distributed_buckets = 3,
    dynamic_partition = @DorisDynamicPartition(enable = true, time_unit = day, end = "3", start = "-7")
)
public class EventLog { ... }
```

#### `@DorisColumn`

Doris 字段级配置。

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `aggregateFun` | 聚合函数 | `none` |
| `autoIncrementStartValue` | 自增起始值 | `-1` |
| `onUpdateCurrentTimestamp` | 是否更新为当前时间戳 | `false` |

```java
@DorisColumn(aggregateFun = AggregateFun.sum)
private BigDecimal amount;
```

#### `@DorisIndex`

Doris 索引配置。

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `name` | 索引名 | `""` |
| `column` | 索引列 | 必填 |
| `using` | 索引类型 | `inverted` |
| `properties` | properties 数组 | `{}` |
| `comment` | 注释 | `""` |

```java
@DorisTable(indexes = {
    @DorisIndex(column = "content", using = DorisIndexType.inverted)
})
public class Article { ... }
```

#### `@DorisRollup`

Doris 物化视图配置。

| 属性 | 说明 |
|------|------|
| `name` | 名称 |
| `columns` | 列名数组 |
| `properties` | properties 数组 |

#### `@DorisPartition`

Doris 分区配置。

| 属性 | 说明 |
|------|------|
| `partition` | 分区名称 |
| `values_left_include` | range 分区左闭区间 |
| `values_right_exclude` | range 分区右开区间 |
| `values_less_than` | range 分区上界 |
| `from` | range 批量分区左闭区间 |
| `to` | range 批量分区右开区间 |
| `interval` | range 批量分区步长 |
| `unit` | range 批量分区步长单位 |
| `values_in` | list 分区 value |

#### `@DorisDynamicPartition`

Doris 动态分区配置。

| 属性 | 说明 |
|------|------|
| `enable` | 是否启用 |
| `time_unit` | 时间单位 |
| `start` | 起始值 |
| `end` | 结束值 |
| `prefix` | 前缀 |
| `buckets` | 分桶数 |
| `replication_num` | 副本数 |
| `create_history_partition` | 是否创建历史分区 |
| `start_day_of_week` | 周起始日 |
| `start_day_of_month` | 月起始日 |
| `reserved_history_periods` | 保留历史周期 |
| `time_zone` | 时区 |

## 数据库类型常量（TypeConstant）

### MySQL (`MysqlTypeConstant`)

```java
// 整数
MysqlTypeConstant.INT
MysqlTypeConstant.TINYINT
MysqlTypeConstant.SMALLINT
MysqlTypeConstant.MEDIUMINT
MysqlTypeConstant.BIGINT

// 小数
MysqlTypeConstant.FLOAT
MysqlTypeConstant.DOUBLE
MysqlTypeConstant.DECIMAL

// 字符串
MysqlTypeConstant.CHAR
MysqlTypeConstant.VARCHAR
MysqlTypeConstant.TEXT
MysqlTypeConstant.TINYTEXT
MysqlTypeConstant.MEDIUMTEXT
MysqlTypeConstant.LONGTEXT

// 枚举
MysqlTypeConstant.ENUM
MysqlTypeConstant.SET

// 日期
MysqlTypeConstant.YEAR
MysqlTypeConstant.TIME
MysqlTypeConstant.DATE
MysqlTypeConstant.DATETIME
MysqlTypeConstant.TIMESTAMP

// 二进制
MysqlTypeConstant.BIT
MysqlTypeConstant.BINARY
MysqlTypeConstant.VARBINARY
MysqlTypeConstant.BLOB
MysqlTypeConstant.TINYBLOB
MysqlTypeConstant.MEDIUMBLOB
MysqlTypeConstant.LONGBLOB

// JSON
MysqlTypeConstant.JSON
```

### PostgreSQL (`PgsqlTypeConstant`)

```java
PgsqlTypeConstant.INT2      // int2 小整数 (16位)
PgsqlTypeConstant.INT4      // int4 整数 (32位)
PgsqlTypeConstant.INT8      // int8 大整数 (64位)
PgsqlTypeConstant.BOOL      // bool 布尔类型
PgsqlTypeConstant.FLOAT4    // float4 单精度浮点数
PgsqlTypeConstant.FLOAT8    // float8 双精度浮点数
PgsqlTypeConstant.MONEY     // money 货币类型
PgsqlTypeConstant.NUMERIC   // numeric 精确小数类型
PgsqlTypeConstant.BYTEA     // bytea 二进制数据类型
PgsqlTypeConstant.CHAR      // char 定长字符类型
PgsqlTypeConstant.VARCHAR   // varchar 可变长度字符类型
PgsqlTypeConstant.TEXT      // text 大文本类型
PgsqlTypeConstant.TIME      // time 时间类型 (无时区)
PgsqlTypeConstant.TIMETZ    // timetz 时间类型 (带时区)
PgsqlTypeConstant.DATE      // date 日期类型
PgsqlTypeConstant.TIMESTAMP     // timestamp 时间戳 (无时区)
PgsqlTypeConstant.TIMESTAMPTZ   // timestamptz 时间戳 (带时区)
PgsqlTypeConstant.BIT       // bit 定长位串类型
PgsqlTypeConstant.VARBIT    // varbit 可变长度位串类型
PgsqlTypeConstant.JSON      // json JSON 数据类型
PgsqlTypeConstant.JSONB     // jsonb 二进制 JSON 数据类型
PgsqlTypeConstant.XML       // xml XML 数据类型
PgsqlTypeConstant.INTERVAL  // interval 时间间隔类型
PgsqlTypeConstant.CIDR      // cidr IPv4 或 IPv6 网络类型
PgsqlTypeConstant.INET      // inet IPv4 或 IPv6 地址类型
PgsqlTypeConstant.MACADDR   // macaddr MAC 地址类型
PgsqlTypeConstant.TSQUERY   // tsquery 全文检索查询类型
PgsqlTypeConstant.TSVECTOR  // tsvector 全文检索向量类型
```

### Oracle (`OracleTypeConstant`)

```java
// 字符类型
OracleTypeConstant.CHAR             // 固定长度字符串，最大2000字节
OracleTypeConstant.VARCHAR2         // 可变长度字符串，最大4000字节
OracleTypeConstant.NCHAR            // Unicode字符，最大2000字节
OracleTypeConstant.NVARCHAR2        // 可变Unicode字符，最大4000字节

// 数值类型
OracleTypeConstant.NUMBER             // 通用数值类型
OracleTypeConstant.BINARY_FLOAT     // 32位浮点数
OracleTypeConstant.BINARY_DOUBLE    // 64位浮点数

// 日期时间类型
OracleTypeConstant.DATE             // 含世纪、年、月、日、时、分、秒
OracleTypeConstant.TIMESTAMP        // 更精确的时间戳
OracleTypeConstant.TIMESTAMP_WITH_TIME_ZONE        // 带时区信息的时间戳
OracleTypeConstant.TIMESTAMP_WITH_LOCAL_TIME_ZONE  // 本地时区时间戳

// 大对象类型
OracleTypeConstant.BLOB             // 二进制大对象，最大支持4GB
OracleTypeConstant.CLOB             // 字符大对象
OracleTypeConstant.NCLOB            // Unicode字符大对象
OracleTypeConstant.BFILE            // 存储在外部文件中的二进制数据

// 行标识类型
OracleTypeConstant.ROWID            // 物理行标识符
OracleTypeConstant.UROWID           // 通用行标识符

// 特殊数据类型
OracleTypeConstant.RAW                // 可变长度二进制数据，最大2000字节
```

### 达梦（`DmTypeConstant`）

```java
// 数值类型
DmTypeConstant.SERIAL
DmTypeConstant.INTEGER
DmTypeConstant.BIGINT
DmTypeConstant.TINYINT
DmTypeConstant.SMALLINT
DmTypeConstant.DECIMAL
DmTypeConstant.FLOAT
DmTypeConstant.DOUBLE
DmTypeConstant.NUMBER

// 字符类型
DmTypeConstant.CHAR
DmTypeConstant.VARCHAR
DmTypeConstant.VARCHAR2
DmTypeConstant.CLOB
DmTypeConstant.TEXT

// 日期时间
DmTypeConstant.DATE
DmTypeConstant.TIME
DmTypeConstant.DATETIME
DmTypeConstant.TIMESTAMP

// 二进制类型
DmTypeConstant.BLOB
DmTypeConstant.BINARY
DmTypeConstant.VARBINARY
DmTypeConstant.IMAGE

// 特殊类型
DmTypeConstant.BIT
DmTypeConstant.XML
DmTypeConstant.FILE
DmTypeConstant.ARRAY
DmTypeConstant.OBJECT
```

### H2 (`H2TypeConstant`)

```java
// 整数
H2TypeConstant.INTEGER
H2TypeConstant.TINYINT
H2TypeConstant.SMALLINT
H2TypeConstant.BIGINT

// 小数
H2TypeConstant.NUMERIC
H2TypeConstant.REAL

// 字符串
H2TypeConstant.CHARACTER
H2TypeConstant.CHARACTER_VARYING
H2TypeConstant.VARCHAR_IGNORECASE
H2TypeConstant.UUID
H2TypeConstant.CHARACTER_LARGE_OBJECT

// 日期
H2TypeConstant.TIME
H2TypeConstant.DATE
H2TypeConstant.TIMESTAMP

// 二进制
H2TypeConstant.BINARY
H2TypeConstant.BLOB

// 布尔
H2TypeConstant.BOOLEAN

// 其他
H2TypeConstant.OTHER
H2TypeConstant.ARRAY
```

### Doris (`DorisTypeConstant`)

```java
// 布尔值
DorisTypeConstant.BOOLEAN

// 整数
DorisTypeConstant.TINYINT
DorisTypeConstant.SMALLINT
DorisTypeConstant.INT
DorisTypeConstant.BIGINT
DorisTypeConstant.LARGEINT

// 小数
DorisTypeConstant.FLOAT
DorisTypeConstant.DOUBLE
DorisTypeConstant.DECIMAL

// 日期
DorisTypeConstant.DATE
DorisTypeConstant.DATETIME

// 字符串
DorisTypeConstant.CHAR
DorisTypeConstant.VARCHAR
DorisTypeConstant.STRING

// 半结构类型
DorisTypeConstant.ARRAY
DorisTypeConstant.MAP
DorisTypeConstant.STRUCT
DorisTypeConstant.JSON
DorisTypeConstant.VARIANT

// 聚合类型
DorisTypeConstant.HLL
DorisTypeConstant.BITMAP
DorisTypeConstant.QUANTILE_STATE
DorisTypeConstant.AGG_STATE

// IP 类型
DorisTypeConstant.IPv4
DorisTypeConstant.IPv6
```

## Spring Boot 配置（YAML）

```yaml
auto-table:
  # 是否启用
  enable: true
  # 启动模式：none/create/update
  mode: update
  # 实体类扫描包路径
  model-package: com.example.entity
  # 是否自动创建数据库
  auto-build-database: false
  # 是否自动删除没有声明的表（危险！）
  auto-drop-table: false
  # 是否自动删除没有声明的列
  auto-drop-column: false
  # 逻辑删除列前缀（不删除而是重命名）
  logic-drop-column-prefix: "deleted_"
  # 是否自动删除不匹配的索引
  auto-drop-index: true
  # 严格继承模式
  strict-extends: true
  # 父类字段排序位置：after/before
  super-insert-position: after
  # 索引前缀
  index-prefix: "auto_idx_"

  # MySQL 配置
  mysql:
    table-default-charset: utf8mb4
    table-default-collation: utf8mb4_general_ci
    column-default-charset: utf8mb4
    column-default-collation: utf8mb4_general_ci

  # PostgreSQL 配置
  pgsql:
    pk-auto-increment-type: byDefault  # always/byDefault

  # SQL 审计记录
  record-sql:
    enable: false
    record-type: db  # db/file/custom
    version: 1.0.0

  # 数据初始化
  init-data:
    enable: true
    base-path: "classpath:sql"
    default-init-file-name: "_init_"
```

## 多数据库适配（Dialect）

同一实体适配多种数据库：

```java
@AutoTable
public class Article {

    @PrimaryKey(autoIncrement = true)
    private Long id;

    // 不同数据库使用不同的大文本类型
    @AutoColumns({
        @AutoColumn(type = "longtext", dialect = "MySQL"),
        @AutoColumn(type = "text", dialect = "PostgreSQL"),
        @AutoColumn(type = "clob", dialect = "Oracle")
    })
    private String content;

    // 不同数据库使用不同的字段长度
    @AutoColumns({
        @AutoColumn(length = 100, dialect = "MySQL"),
        @AutoColumn(length = 200, dialect = "PostgreSQL")
    })
    private String summary;
}
```

支持的方言常量（`DatabaseDialect`）：
- `MySQL`
- `MariaDB`
- `PostgreSQL`
- `Oracle`
- `DM`（达梦）
- `Kingbase`（人大金仓）
- `H2`
- `SQLite`
- `Doris`

## 完整示例

### 用户表

```java
@Data
@AutoTable(value = "sys_user", comment = "用户表")
public class User {

    @PrimaryKey(autoIncrement = true)
    private Long id;

    @AutoColumn(comment = "用户名", length = 50, notNull = true)
    @Index(type = IndexTypeEnum.UNIQUE, comment = "用户名唯一索引")
    private String username;

    @AutoColumn(comment = "密码", length = 100, notNull = true)
    private String password;

    @AutoColumn(comment = "邮箱", length = 100)
    @Index
    private String email;

    @AutoColumn(comment = "手机号", length = 20)
    private String phone;

    @ColumnComment("状态：0-禁用，1-启用")
    @ColumnDefault("1")
    private Integer status;

    @ColumnComment("创建时间")
    private LocalDateTime createTime;

    @ColumnComment("更新时间")
    private LocalDateTime updateTime;

    @ColumnComment("是否删除：0-否，1-是")
    @ColumnDefault("0")
    @Ignore
    private Integer deleted;
}
```

### 组合索引

```java
@Data
@AutoTable(comment = "订单表")
@TableIndex(name = "idx_user_status", fields = {"userId", "status"}, comment = "用户+状态索引")
public class Order {

    @PrimaryKey(autoIncrement = true)
    private Long id;

    @AutoColumn(comment = "用户ID", notNull = true)
    private Long userId;

    @AutoColumn(comment = "订单号", notNull = true)
    @Index(type = IndexTypeEnum.UNIQUE)
    private String orderNo;

    @AutoColumn(comment = "状态", notNull = true)
    private Integer status;

    @AutoColumn(comment = "金额")
    private BigDecimal amount;
}
```

### 多数据源

```java
@AutoTable
@DS("master")
public class User { ... }

@AutoTable
@DS("slave")
public class Log { ... }
```

## 生命周期回调

实现回调接口，在表结构变更前后执行自定义逻辑：

| 回调接口 | 触发时机 |
|----------|----------|
| `AutoTableReadyCallback` | AutoTable 准备就绪 |
| `RunBeforeCallback` | 执行前 |
| `RunAfterCallback` | 执行后 |
| `CreateTableFinishCallback` | 创建表完成后 |
| `ModifyTableFinishCallback` | 修改表完成后 |
| `DeleteTableFinishCallback` | 删除表完成后 |
| `CreateDatabaseFinishCallback` | 创建数据库完成后 |
| `CompareTableFinishCallback` | 对比表完成后 |
| `ValidateFinishCallback` | 校验完成后 |
| `AutoTableFinishCallback` | AutoTable 全部执行完成后 |

```java
@Component
public class MyCallback implements CreateTableFinishCallback {
    @Override
    public void afterCreateTable(String dialect, TableMetadata metadata) {
        log.info("表 {} 创建完成", metadata.getTableName());
    }
}
```

## 数据初始化

### 方式 1：SQL 文件

将 SQL 文件放在 `src/main/resources/sql/` 目录下：

```
src/main/resources/sql/
├── user.sql          # 匹配表名的 SQL 文件
├── _init_.sql        # 全局初始化脚本
└── ...
```

### 方式 2：Java 方法

```java
@AutoTable
public class User {

    @PrimaryKey(autoIncrement = true)
    private Long id;

    private String name;

    // 静态方法返回初始数据
    @InitDataList
    public static List<User> initData() {
        return Arrays.asList(
            new User("admin"),
            new User("guest")
        );
    }
}
```

### 方式 3：指定 SQL 文件

```java
@AutoTable(initSql = "classpath:sql/{dialect}/user.sql")
public class User { ... }
```

## 注意事项

1. **谨慎使用删除模式**：`auto-drop-table` 和 `auto-drop-column` 会造成数据丢失，生产环境务必关闭。
2. **逻辑删除列前缀**：配置 `logic-drop-column-prefix` 可在删除字段时保留原数据（重命名而非删除）。
3. **字段排序**：`sort` 属性控制字段顺序，但不是所有数据库都支持。
4. **索引命名**：索引名会自动添加前缀 `auto_idx_`，超长时会自动 hash 处理。
5. **启动模式**：
   - `none`：不做任何处理
   - `create`：删除所有表后重建（会丢失数据！）
   - `update`：增量更新（推荐生产环境使用）
