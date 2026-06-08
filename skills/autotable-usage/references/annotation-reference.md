# AutoTable 注解完整参考

共 29 个注解，分 3 类：核心(15)、MySQL(8)、Doris(6)。

## 核心注解

### 类级注解

#### `@AutoTable`

声明该类需要自动建表。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | `String` | `""` | 表名，空则取类名（驼峰→下划线） |
| `schema` | `String` | `""` | 表 schema |
| `comment` | `String` | `""` | 表注释，空则取类名 |
| `dialect` | `String` | `""` | 方言，参考 `DatabaseDialect` 常量 |
| `initSql` | `String` | `""` | 初始化 SQL 文件路径，支持 `{dialect}` 占位符 |

```java
@AutoTable(value = "sys_user", comment = "用户表", schema = "public")
public class User { ... }
```

#### `@TableIndex`（可重复，容器 `@TableIndexes`）

类级组合索引定义。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | `String` | `""` | 索引名，空则自动生成（含 `auto_idx_` 前缀） |
| `type` | `IndexTypeEnum` | `NORMAL` | `NORMAL` / `UNIQUE` |
| `method` | `String` | `""` | 索引方法：btree、hash、GiST、GIN |
| `fields` | `String[]` | `{}` | 字段名数组（按顺序） |
| `indexFields` | `IndexField[]` | `{}` | 带排序的字段配置（优先级高于 `fields`） |
| `comment` | `String` | `""` | 索引注释 |

```java
@AutoTable
@TableIndex(name = "idx_name_age", fields = {"username", "age"})
@TableIndex(name = "idx_email", fields = {"email"}, type = IndexTypeEnum.UNIQUE)
public class User { ... }

// 带排序的组合索引
@AutoTable
@TableIndex(
    name = "idx_time_status",
    indexFields = {
        @IndexField(field = "createTime", sort = IndexSortTypeEnum.DESC),
        @IndexField(field = "status", sort = IndexSortTypeEnum.ASC)
    }
)
public class Order { ... }
```

#### `@Ignore`（类级）

标注在类上，跳过该类不建表。

#### `@PrimaryKey`（字段级/类级）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `autoIncrement` | `boolean` | `false` | 是否自增 |

```java
@PrimaryKey(autoIncrement = true)
private Long id;
```

#### `@MysqlEngine`（MySQL 类级）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | `String` | 必填 | 存储引擎：InnoDB、MyISAM 等 |

#### `@MysqlCharset`（MySQL 类级）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `charset` | `String` | 必填 | 字符集：utf8mb4 |
| `collate` | `String` | 必填 | 排序规则：utf8mb4_general_ci |

#### `@MysqlTableFullTextIndex`（MySQL 类级，可重复，容器 `@MysqlTableFullTextIndexes`）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | `String` | `""` | 索引名 |
| `comment` | `String` | `""` | 索引注释 |
| `parser` | `String` | `""` | 分词器，如 `ngram` |
| `fields` | `String[]` | `{}` | 多字段组合全文索引 |

### 字段级注解

#### `@AutoColumn`（最常用，聚合配置）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | `String` | `""` | 列名，空取属性名（驼峰→下划线） |
| `type` | `String` | `""` | 数据库类型，空则自动推断 |
| `length` | `int` | `-1` | 长度（<0 不限制） |
| `decimalLength` | `int` | `-1` | 小数位数 |
| `notNull` | `boolean` | `false` | 非空约束 |
| `defaultValue` | `String` | `""` | 默认值字符串 |
| `defaultValueType` | `DefaultValueEnum` | `UNDEFINED` | 默认值类型 |
| `comment` | `String` | `""` | 注释 |
| `sort` | `int` | `0` | 排序（1=第一个，-1=最后一个） |
| `dialect` | `String` | `""` | 方言 |

```java
@AutoColumn(type = "varchar", length = 50, notNull = true, comment = "用户名")
private String username;
```

#### `@AutoColumns`（多数据库适配容器）

包含多个 `@AutoColumn`，通过 `dialect` 区分。未指定 dialect 的作为默认值。

```java
@AutoColumns({
    @AutoColumn(type = "longtext", dialect = "MySQL"),
    @AutoColumn(type = "text", dialect = "PostgreSQL"),
    @AutoColumn(type = "clob", dialect = "Oracle")
})
private String content;
```

#### `@PrimaryKey`

```java
@PrimaryKey(autoIncrement = true)
private Long id;

// 联合主键：在类级使用
@PrimaryKey
@AutoTable
public class UserRole {
    private Long userId;
    private Long roleId;
}
```

#### `@AutoIncrement`

独立标记自增（可替代 `@PrimaryKey(autoIncrement=true)`）：

```java
@AutoIncrement
private Long id;
```

#### `@Index`

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | `String` | `""` | 索引名，空自动生成 |
| `type` | `IndexTypeEnum` | `NORMAL` | `NORMAL` / `UNIQUE` |
| `method` | `String` | `""` | btree / hash |
| `comment` | `String` | `""` | 索引注释 |

```java
@Index(type = IndexTypeEnum.UNIQUE, comment = "用户名唯一索引")
private String username;
```

#### `@ColumnDefault`

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type` | `DefaultValueEnum` | `UNDEFINED` | 默认值类型 |
| `value` | `String` | `""` | 默认值字符串 |

```java
@ColumnDefault("0")
private Integer status;

@ColumnDefault(type = DefaultValueEnum.EMPTY_STRING)
private String remark;

@ColumnDefault(type = DefaultValueEnum.NULL)
private String optional;
```

#### `@ColumnType`

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | `String` | `""` | 数据库类型 |
| `length` | `int` | `-1` | 长度 |
| `decimalLength` | `int` | `-1` | 小数位数 |
| `values` | `String[]` | `{}` | 枚举/SET 值（MySQL） |

```java
@ColumnType(value = "varchar", length = 200)
private String description;

@ColumnType(value = "enum", values = {"male", "female"})
private String gender;
```

#### `@ColumnComment`

```java
@ColumnComment("用户状态：0-禁用，1-启用")
private Integer status;
```

#### `@ColumnName`

```java
@ColumnName("user_name")
private String userName;
```

#### `@ColumnNotNull`

```java
@ColumnNotNull
private String username;
```

#### `@Ignore`（字段级）

```java
@Ignore
private String tempField;  // 不映射到数据库
```

#### `@MysqlColumnCharset`（MySQL 字段级）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | `String` | 必填 | 字符集 |
| `collate` | `String` | 必填 | 排序规则 |

#### `@MysqlColumnUnsigned`（MySQL 字段级，标记）

无属性，标记数值列 UNSIGNED。

#### `@MysqlColumnZerofill`（MySQL 字段级，标记）

无属性，标记数值列 ZEROFILL。

#### `@MysqlFullTextIndex`（MySQL 字段级）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | `String` | `""` | 索引名 |
| `comment` | `String` | `""` | 索引注释 |
| `parser` | `String` | `""` | 分词器，如 `ngram` |

## Doris 注解

### `@DorisTable`（类级）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `indexes` | `DorisIndex[]` | `{}` | 索引配置 |
| `engine` | `String` | `"olap"` | 表引擎 |
| `duplicate_key` | `String[]` | `{}` | 明细模型 key |
| `unique_key` | `String[]` | `{}` | 主键模型 key |
| `aggregate_key` | `String[]` | `{}` | 聚合模型 key |
| `auto_partition` | `boolean` | `false` | 自动分区 |
| `auto_partition_time_unit` | `DorisTimeUnit` | `none` | 自动分区时间单位 |
| `partition_by_range` | `String[]` | `{}` | range 分区列 |
| `partition_by_list` | `String[]` | `{}` | list 分区列 |
| `partitions` | `DorisPartition[]` | `{}` | 手动分区配置 |
| `dynamic_partition` | `DorisDynamicPartition` | 默认关闭 | 动态分区 |
| `distributed_by_hash` | `String[]` | `{}` | hash 分桶列 |
| `distributed_buckets` | `int` | `-1` | 分桶数（-1=自动） |
| `rollup` | `DorisRollup[]` | `{}` | 物化视图 |
| `properties` | `String[]` | `{}` | 建表 properties |

### `@DorisColumn`（字段级）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `aggregateFun` | `AggregateFun` | `none` | 聚合函数 |
| `autoIncrementStartValue` | `long` | `-1` | 自增起始值 |
| `onUpdateCurrentTimestamp` | `boolean` | `false` | 更新时设为当前时间戳 |

### `@DorisIndex`（嵌套在 @DorisTable 内）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | `String` | `""` | 索引名 |
| `column` | `String` | 必填 | 索引列 |
| `using` | `DorisIndexType` | `inverted` | inverted / ngram_bf / bitmap |
| `properties` | `String[]` | `{}` | properties |
| `comment` | `String` | `""` | 注释 |

### `@DorisPartition`（嵌套在 @DorisTable 内）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `partition` | `String` | `""` | 分区名 |
| `values_left_include` | `String[]` | `{}` | range 左闭值 |
| `values_right_exclude` | `String[]` | `{}` | range 右开值 |
| `values_less_than` | `String[]` | `{}` | range 上界 |
| `from` / `to` | `String` | `""` | 批量分区区间 |
| `interval` | `int` | `-1` | 步长 |
| `unit` | `DorisTimeUnit` | `none` | 步长单位 |
| `values_in` | `String[]` | `{}` | list 分区值 |

### `@DorisDynamicPartition`（嵌套在 @DorisTable 内）

| 属性 | 类型 | 说明 |
|------|------|------|
| `enable` | `boolean` | 是否启用 |
| `time_unit` | `DorisTimeUnit` | 时间单位 |
| `start` | `String` | 起始偏移 |
| `end` | `String` | 结束偏移 |
| `prefix` | `String` | 分区名前缀 |
| `buckets` | `String` | 分桶数 |
| `replication_num` | `String` | 副本数 |
| `create_history_partition` | `String` | 是否创建历史分区 |
| `start_day_of_week` | `String` | 周起始日 |
| `start_day_of_month` | `String` | 月起始日 |
| `reserved_history_periods` | `String` | 保留历史周期 |
| `time_zone` | `String` | 时区 |

### `@DorisRollup`（嵌套在 @DorisTable 内）

| 属性 | 类型 | 说明 |
|------|------|------|
| `name` | `String` | 物化视图名 |
| `columns` | `String[]` | 列名数组 |
| `properties` | `String[]` | properties |

## 枚举

### `IndexTypeEnum`

| 值 | 说明 |
|----|------|
| `NORMAL` | 普通索引（允许重复和 null） |
| `UNIQUE` | 唯一索引 |

### `IndexSortTypeEnum`

| 值 | 说明 |
|----|------|
| `ASC` | 正序 |
| `DESC` | 倒序 |

### `DefaultValueEnum`

| 值 | 说明 |
|----|------|
| `UNDEFINED` | 未定义（默认） |
| `NULL` | NULL 值 |
| `EMPTY_STRING` | 空字符串 |

### `DorisTimeUnit`

`none` / `year` / `month` / `week` / `day` / `hour`

### `DorisIndexType`

`inverted` / `ngram_bf` / `bitmap`

### `AggregateFun`

`none` / `sum` / `min` / `max` / `replace` / `replace_if_not_null` / `hll_union` / `bitmap_union`

## 注解优先级规则

当多个注解同时存在时，合并规则：

1. `@AutoColumn` 是聚合注解，等价于同时标注 `@ColumnName` + `@ColumnType` + `@ColumnNotNull` + `@ColumnDefault` + `@ColumnComment`
2. 独立注解（如 `@ColumnType`）与 `@AutoColumn` 的对应属性冲突时，**独立注解优先**
3. `@AutoColumns` 中匹配当前 dialect 的配置优先于无 dialect 的默认配置
4. ORM 框架适配器（如 MyBatis-Plus `@TableField`）的优先级低于 AutoTable 原生注解
