# AutoTable 多数据源

AutoTable 支持管理多个数据库，按数据源分组自动处理。

## 核心机制

1. 每个实体可通过 `IDataSourceHandler` 关联到特定数据源
2. 框架启动后按数据源分组，切换连接后逐个执行
3. 每个数据源可有不同的方言（MySQL + PostgreSQL 混合）

## Spring Boot + dynamic-datasource

最常用的多数据源方案：使用 `dynamic-datasource-spring-boot-starter`。

### 依赖

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>dynamic-datasource-spring-boot-starter</artifactId>
    <version>4.3.0</version>
</dependency>
```

### 配置

```yaml
spring:
  datasource:
    dynamic:
      primary: master
      datasource:
        master:
          url: jdbc:mysql://localhost:3306/db_master
          username: root
          password: xxx
        slave:
          url: jdbc:postgresql://localhost:5432/db_slave
          username: postgres
          password: xxx
```

### 实体指定数据源

```java
@AutoTable
@DS("master")  // 写入 MySQL 主库
public class User {
    @PrimaryKey(autoIncrement = true)
    private Long id;
    private String name;
}

@AutoTable
@DS("slave")   // 写入 PostgreSQL 从库
public class Log {
    @PrimaryKey(autoIncrement = true)
    private Long id;
    private String action;
}
```

## 自定义 IDataSourceHandler

如果不使用 dynamic-datasource，可实现 `IDataSourceHandler` 接口：

```java
@Component
public class MyDataSourceHandler implements IDataSourceHandler {

    @Autowired
    private Map<String, DataSource> dataSourceMap;

    @Override
    public String getDataSourceName(Class<?> entityClass) {
        // 从实体注解或类名判断属于哪个数据源
        DS ds = entityClass.getAnnotation(DS.class);
        return ds != null ? ds.value() : "default";
    }

    @Override
    public void useDataSource(String dataSourceName) {
        // 切换当前线程的数据源连接
        DynamicDataSourceContextHolder.push(dataSourceName);
    }

    @Override
    public void resetDataSource() {
        DynamicDataSourceContextHolder.poll();
    }
}
```

## Solon 多数据源

Solon 内置支持 `@DynamicDs` 注解：

```java
@AutoTable
@DynamicDs("order-db")
public class Order { ... }
```

Solon plugin 的 `SolonDataSourceHandler` 会自动识别 `@DynamicDs` 和 `DynamicDsKey.current()`。

## 注意事项

1. **同一数据源内表名不能重复**：框架会检查并报错
2. **方言自动检测**：通过 JDBC Connection 的 metadata 获取，无需手动指定
3. **数据初始化路径**：多数据源时，SQL 文件支持按数据源分目录：
   ```
   sql/
   ├── user.sql              # 默认数据源
   ├── _init_.sql            # 默认数据源全局初始化
   ├── slave/
   │   ├── log.sql           # slave 数据源
   │   └── _init_.sql        # slave 全局初始化
   └── slave.sql             # slave 数据源全局初始化（另一种命名）
   ```
4. **SQL 审计记录**：可配置独立的数据源存储审计记录：
   ```yaml
   auto-table:
     record-sql:
       enable: true
       record-type: db
       datasource:
         url: jdbc:mysql://localhost:3306/audit_db
         username: root
         password: xxx
   ```
