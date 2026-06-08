# AutoTable 类型映射

Java 类型到数据库类型的自动转换机制。

## 映射原理

每个数据库策略（`IStrategy`）提供一个 `typeMapping()` 方法，返回 `Map<Class<?>, DefaultTypeEnumInterface>`，定义 Java 类型 → 数据库类型的映射关系。

解析优先级（从高到低）：

1. `@ColumnType` 注解（显式指定类型名）
2. `@AutoColumn.type` 注解
3. `@AutoColumns` 中匹配当前 dialect 的配置
4. ORM 适配器（如 MyBatis-Plus `@TableField`）
5. 类型映射表查找（Java Class → DB Type）
6. 兜底：String 类型 + 警告日志

## 默认类型映射

### 通用 Java 类型 → 各数据库

| Java 类型 | MySQL | PostgreSQL | Oracle | H2 | SQLite |
|-----------|-------|------------|--------|-----|--------|
| `String` | `varchar(255)` | `varchar(255)` | `varchar2(255)` | `varchar(255)` | `varchar(255)` |
| `Long` / `long` | `bigint` | `int8` | `number(19)` | `bigint` | `integer` |
| `Integer` / `int` | `int` | `int4` | `number(10)` | `integer` | `integer` |
| `Short` / `short` | `smallint` | `int2` | `number(5)` | `smallint` | `integer` |
| `Byte` / `byte` | `tinyint` | `int2` | `number(3)` | `tinyint` | `integer` |
| `Boolean` / `boolean` | `tinyint(1)` | `bool` | `number(1)` | `boolean` | `integer` |
| `Float` / `float` | `float` | `float4` | `binary_float` | `real` | `real` |
| `Double` / `double` | `double` | `float8` | `binary_double` | `double` | `real` |
| `BigDecimal` | `decimal` | `numeric` | `number` | `numeric` | `numeric` |
| `LocalDate` | `date` | `date` | `date` | `date` | `date` |
| `LocalTime` | `time` | `time` | `date` | `time` | `time` |
| `LocalDateTime` | `datetime` | `timestamp` | `timestamp` | `timestamp` | `datetime` |
| `Date` | `datetime` | `timestamp` | `date` | `timestamp` | `datetime` |
| `byte[]` | `blob` | `bytea` | `blob` | `blob` | `blob` |
| `Enum` | `varchar(255)` | `varchar(255)` | `varchar2(255)` | `varchar(255)` | `varchar(255)` |

> 上表为常见映射，实际以各策略的 `typeMapping()` 为准。

## 自定义类型

### 方式 1：注解显式指定

```java
@AutoColumn(type = "longtext")
private String content;

@ColumnType(value = "decimal", length = 10, decimalLength = 2)
private BigDecimal price;
```

### 方式 2：使用类型常量

```java
import static org.dromara.autotable.annotation.constants.MysqlTypeConstant.*;

@ColumnType(JSON)
private String metadata;

@ColumnType(LONGTEXT)
private String content;
```

### 方式 3：扩展类型映射

通过 `JavaTypeToDatabaseTypeConverter` 添加自定义映射：

```java
@Component
public class CustomTypeConfig {

    @PostConstruct
    public void init() {
        // 添加 MySQL 的自定义映射
        JavaTypeToDatabaseTypeConverter.addTypeMapping(
            "MySQL",
            MyCustomType.class,
            DatabaseTypeDefine.of("json", -1, -1)
        );
    }
}
```

或在 Spring Boot 中实现 `JavaTypeToDatabaseTypeConverter` 接口：

```java
@Component
public class MyTypeConverter implements JavaTypeToDatabaseTypeConverter {
    @Override
    public DatabaseTypeAndLength convert(String dialect, Class<?> entityClass, Field field) {
        // 自定义转换逻辑
        if (field.getType() == MyCustomType.class) {
            return DatabaseTypeAndLength.of("json");
        }
        return null; // 返回 null 则走默认映射
    }
}
```

## 枚举字段处理

Java 枚举默认映射为 `varchar(255)`，存储枚举的 `name()` 值。

### MySQL ENUM 类型

```java
@ColumnType(value = "enum", values = {"PENDING", "ACTIVE", "CLOSED"})
private OrderStatus status;
```

## 长度控制

```java
// 仅指定长度
@AutoColumn(length = 100)
private String name;  // → varchar(100)

// 指定精度
@ColumnType(value = "decimal", length = 10, decimalLength = 2)
private BigDecimal price;  // → decimal(10,2)

// 不指定长度（使用数据库默认）
@AutoColumn
private String description;  // → varchar(255) 或 text
```

## 类型常量类

| 类名 | 数据库 |
|------|--------|
| `MysqlTypeConstant` | MySQL / MariaDB |
| `PgsqlTypeConstant` | PostgreSQL |
| `OracleTypeConstant` | Oracle |
| `H2TypeConstant` | H2 |
| `DmTypeConstant` | 达梦 |
| `DorisTypeConstant` | Doris |

使用方式：

```java
import static org.dromara.autotable.annotation.constants.MysqlTypeConstant.*;
import static org.dromara.autotable.annotation.constants.PgsqlTypeConstant.*;

// MySQL
@ColumnType(JSON)
private String data;

// PostgreSQL
@ColumnType(JSONB)
private String data;
```
