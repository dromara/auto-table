# AutoTable 最佳实践

## 生产环境部署

### 推荐工作流

```
开发环境（update）→ SQL 审计记录 → 生成 Flyway 脚本 → 生产环境（validate）
```

### 1. 开发环境：update 模式

```yaml
auto-table:
  mode: update
  auto-drop-column: true        # 开发期可开启，自动清理废弃字段
  auto-drop-index: true
```

### 2. SQL 审计 → Flyway 脚本

开启 SQL 记录，自动生成迁移脚本：

```yaml
auto-table:
  record-sql:
    enable: true
    record-type: file
    version: 1.0.0
    folder-path: ./src/main/resources/db/migration
```

自定义 `RecordSqlHandler` 生成 Flyway 命名格式：

```java
@Component
public class FlywayRecordSqlHandler implements RecordSqlHandler {

    private final String version = "1.0.0";

    @Override
    public void record(String dialect, List<String> sqlList, TableMetadata metadata) {
        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        );
        String fileName = String.format("V%s__%s_%s.sql",
            version, timestamp, metadata.getTableName()
        );
        Path filePath = Path.of("./src/main/resources/db/migration", fileName);
        Files.write(filePath, String.join(";\n", sqlList).getBytes());
    }
}
```

### 3. 生产环境：validate 模式

```yaml
auto-table:
  mode: validate          # 只校验不修改，不一致则启动失败
  auto-drop-table: false  # 绝对禁止
  auto-drop-column: false # 绝对禁止
  auto-build-database: false
```

## 单元测试

### Spring Boot 测试

```java
@EnableAutoTableTest
@SpringBootTest
class UserServiceTest {

    @Test
    void testCreateUser() {
        // 表已在测试启动时自动创建
        // 使用 H2 内存数据库
    }
}
```

配合 H2：

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL
    driver-class-name: org.h2.Driver

auto-table:
  mode: create  # 测试环境可重建
```

### 纯 Java 测试

```java
public class AutoTableTest {

    private static DataSource dataSource;

    @BeforeAll
    static void setup() {
        // 创建 H2 内存数据源
        dataSource = new JdbcDataSource();
        ((JdbcDataSource) dataSource).setURL("jdbc:h2:mem:test");

        AutoTableGlobalConfig config = AutoTableGlobalConfig.instance();
        PropertyConfig props = new PropertyConfig();
        props.setMode(RunMode.create);
        config.setPropertyConfig(props);
        config.setDataSource(dataSource);

        AutoTableBootstrap.start(config, Set.of(User.class, Order.class));
    }
}
```

## 继承设计

### 基类抽取公共字段

```java
@Data
public abstract class BaseEntity {
    @AutoColumn(comment = "创建时间", sort = -2)
    private LocalDateTime createTime;

    @AutoColumn(comment = "更新时间", sort = -1)
    private LocalDateTime updateTime;
}

@Data
@AutoTable(comment = "用户表")
public class User extends BaseEntity {
    @PrimaryKey(autoIncrement = true)
    private Long id;

    @AutoColumn(comment = "用户名", notNull = true)
    private String username;
}
```

**注意**：
- `strict-extends: true`（默认）只继承 public/protected 字段
- `super-insert-position: after`（默认）父类字段排在子类字段后面
- 想让父类字段排前面：设置 `super-insert-position: before`

### 多层继承

```java
public class IdEntity {
    @PrimaryKey(autoIncrement = true)
    private Long id;
}

public class TimestampEntity extends IdEntity {
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

@AutoTable
public class User extends TimestampEntity {
    private String username;
}
// 最终列顺序取决于 super-insert-position 配置
```

## 逻辑删除列

不想真正删除列（保留数据），使用逻辑删除：

```yaml
auto-table:
  auto-drop-column: true
  logic-drop-column-prefix: "deleted_"
```

效果：实体中移除 `remark` 字段后，数据库中的 `remark` 列会被重命名为 `deleted_remark` 而不是 DROP。

## 索引命名规范

- 自动生成：`auto_idx_{表名}_{字段名}`
- 超长时自动 hash 缩短
- 可自定义前缀：`index-prefix: "idx_"`
- 组合索引用 `@TableIndex` 并指定 `name`

## 常见陷阱

### 1. `create` 模式丢数据

```yaml
# 开发完忘记切回来 → 重启后所有表被删重建
mode: create  # ⚠️ 危险！
```

**预防**：CI/CD 中按 profile 设置：
```yaml
# application-dev.yml: mode: update
# application-prod.yml: mode: validate
```

### 2. 云数据库 ALTER TABLE 限制

阿里云 RDS 等可能禁止 ALTER TABLE 中混合 DROP 和 ADD：

```yaml
auto-table:
  mysql:
    alter-table-separate-drop: true
```

### 3. 字段名驼峰 vs 下划线

AutoTable 默认将驼峰转为下划线：`userName` → `user_name`。

如需保留驼峰：
```java
@ColumnName("userName")
private String userName;
```

### 4. 父类 private 字段不继承

`strict-extends: true`（默认）只继承 public/protected 字段。如果基类用 `private` + getter/setter（如 Lombok `@Data`），字段不会被继承。

**解决**：基类字段用 `protected`，或设置 `strict-extends: false`。

### 5. Doris 大表更新超时

Doris ALTER TABLE 是全量重建，大表非常慢：

```yaml
auto-table:
  doris:
    update-limit-table-data-length: 2147483648  # 调大阈值
    update-backup-old-table: true  # 开启备份
```

### 6. 枚举类型存储

Java 枚举默认存 `name()`（如 `ACTIVE`），如需存 code：
- 使用 `Integer` 类型字段 + `@ColumnType("tinyint")`
- 或使用 MyBatis 的 `@EnumValue` 注解（框架不处理枚举映射，交给 ORM）

## SpringDoc 集成

引入 `auto-table-support-springdoc` 后，`@ColumnComment` 和 `@AutoColumn(comment=)` 自动映射为 Swagger/OpenAPI 的 `description`，无需重复标注 `@Schema`：

```xml
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-support-springdoc</artifactId>
    <version>2.5.17</version>
</dependency>
```

效果：实体上的 `@ColumnComment("用户名")` 在 Swagger UI 中自动显示为字段描述。
