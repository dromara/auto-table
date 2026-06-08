# AutoTable 快速开始

## 安装

### Maven

```xml
<!-- Spring Boot（兼容 2.x 和 3.x） -->
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-spring-boot-starter</artifactId>
    <version>2.5.17</version>
</dependency>

<!-- Solon 框架 -->
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-solon-plugin</artifactId>
    <version>2.5.17</version>
</dependency>

<!-- 纯 Java（无框架） -->
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-core</artifactId>
    <version>2.5.17</version>
</dependency>
<!-- + 按需引入数据库策略包 -->
<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-strategy-mysql</artifactId>
    <version>2.5.17</version>
</dependency>
```

### Gradle

```groovy
implementation 'org.dromara.autotable:auto-table-spring-boot-starter:2.5.17'
```

## 启用

### Spring Boot

```java
import org.dromara.autotable.springboot.EnableAutoTable;

@EnableAutoTable(basePackages = "com.example.entity")  // 可选，默认扫描主类所在包
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

`@EnableAutoTable` 属性：
- `basePackages`：扫描包路径（不指定则自动取 `@SpringBootApplication` 所在包）
- `classes`：直接指定实体类数组

### Solon

```java
import org.dromara.autotable.solon.EnableAutoTable;

@EnableAutoTable  // Solon 版无 basePackages 属性
@SolonApp
public class Application {
    public static void main(String[] args) {
        Solon.start(Application.class, args);
    }
}
```

### 纯 Java

```java
AutoTableGlobalConfig config = AutoTableGlobalConfig.instance();
config.setPropertyConfig(new PropertyConfig());
config.setDataSource(dataSource);  // javax.sql.DataSource

// 注册策略（通过 SPI 自动加载，或手动添加）
AutoTableBootstrap.start(config, Set.of(User.class, Order.class));
```

## 第一个实体

```java
import org.dromara.autotable.annotation.*;
import lombok.Data;

@Data
@AutoTable(comment = "用户表")
public class User {

    @PrimaryKey(autoIncrement = true)
    private Long id;

    @AutoColumn(comment = "用户名", length = 50, notNull = true)
    @Index(type = IndexTypeEnum.UNIQUE)
    private String username;

    @AutoColumn(comment = "邮箱", length = 100)
    @Index
    private String email;

    @ColumnComment("状态：0-禁用，1-启用")
    @ColumnDefault("1")
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

启动后自动生成：

```sql
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `status` int DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `auto_idx_user_username` (`username`),
  INDEX `auto_idx_user_email` (`email`)
) COMMENT='用户表';
```

## 修改实体 → 自动同步

后续修改实体（新增字段、修改类型、添加索引），重启应用后表结构自动增量更新：

```java
@AutoColumn(comment = "手机号", length = 20)
private String phone;  // 新增字段 → 自动 ALTER TABLE ADD COLUMN
```

## 数据初始化

### 方式 1：自动匹配 SQL 文件

```
src/main/resources/sql/
├── user.sql          # 表名匹配的 SQL
└── _init_.sql        # 全局初始化（建库后执行）
```

### 方式 2：指定 SQL 路径

```java
@AutoTable(initSql = "classpath:sql/{dialect}/user.sql")
public class User { ... }
```

`{dialect}` 占位符会被替换为当前数据库方言（如 `mysql`、`pgsql`）。

### 方式 3：Java 方法

```java
@AutoTable
public class User {
    // ... 字段定义 ...

    @InitDataList
    public static List<User> initData() {
        return Arrays.asList(
            new User("admin", "admin@example.com"),
            new User("guest", "guest@example.com")
        );
    }
}
```

## 基础配置

```yaml
auto-table:
  enable: true                    # 总开关
  mode: update                    # none / validate / create / update
  model-package: com.example.entity  # 实体扫描包
  show-banner: true               # 打印启动 banner
```

## 单元测试

Spring Boot 提供 `@EnableAutoTableTest`，在测试中自动建表但不影响主流程：

```java
@EnableAutoTableTest
@SpringBootTest
class UserServiceTest {
    @Test
    void testCreateUser() {
        // 表已在测试前自动创建
    }
}
```
