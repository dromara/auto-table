# AutoTable 内部架构与扩展

## 模块结构

```
auto-table
├── auto-table-annotation      # 注解定义（零依赖）
├── auto-table-core            # 核心引擎（依赖 commons-dbutils + jsqlparser）
├── auto-table-strategy        # 数据库策略（每种库一个子模块）
│   ├── auto-table-strategy-mysql
│   ├── auto-table-strategy-pgsql
│   ├── auto-table-strategy-oracle
│   ├── auto-table-strategy-sqlite
│   ├── auto-table-strategy-h2
│   ├── auto-table-strategy-doris
│   ├── auto-table-strategy-dm
│   ├── auto-table-strategy-kingbase
│   └── auto-table-strategy-all   # 聚合包（引入全部策略）
├── auto-table-spring-boot-starter  # Spring Boot 集成
├── auto-table-solon-plugin         # Solon 集成
└── auto-table-support              # 第三方支持（springdoc）
```

## 执行流程

```
AutoTableBootstrap.start()
  │
  ├─ 检查 enable / mode
  ├─ 打印 Banner
  ├─ SpiLoader.loadAll(IStrategy.class)        ← Java SPI 加载策略
  ├─ SpiLoader.loadAll(DatabaseBuilder.class)  ← 加载数据库构建器
  ├─ AutoTableClassScanner.scan()              ← 扫描 @AutoTable 实体
  │     └─ 排除 @Ignore 标注的类
  ├─ fire AutoTableReadyCallback
  │
  └─ for each DataSource:
       ├─ 分组实体（IDataSourceHandler.getDataSourceName）
       ├─ 检查重名表
       ├─ 切换数据源
       ├─ 自动建库（DatabaseBuilder，如开启）
       ├─ 选择 IStrategy（按方言匹配）
       ├─ for each Entity:
       │    ├─ fire RunBeforeCallback
       │    ├─ IStrategy.start(entityClass)
       │    │    ├─ analyseClass() → TableMetadata
       │    │    ├─ fire BuildTableMetadataInterceptor
       │    │    └─ switch(mode):
       │    │         ├─ validate → compareTable → ValidateFinishCallback
       │    │         ├─ create → dropTable → createTable
       │    │         │    ├─ fire CreateTableInterceptor
       │    │         │    ├─ fire CreateTableFinishCallback
       │    │         │    └─ InitDataHandler.initTableData()
       │    │         └─ update → compareTable
       │    │              ├─ fire CompareTableFinishCallback
       │    │              ├─ if hasDifference → modifyTable
       │    │              │    ├─ fire ModifyTableInterceptor
       │    │              │    └─ fire ModifyTableFinishCallback
       │    │              └─ else → skip
       │    └─ fire RunAfterCallback
       ├─ if autoDropTable → drop unregistered tables
       │    └─ fire DeleteTableFinishCallback
       └─ initDbData()（新建库时）
  │
  └─ fire AutoTableFinishCallback
```

## SPI 扩展点

### IStrategy（数据库策略）

每种数据库实现一个策略，通过 `META-INF/services/org.dromara.autotable.core.strategy.IStrategy` 注册。

```java
public interface IStrategy {

    /** 该策略支持的数据库方言 */
    String databaseDialect();

    /** Java 类型 → 数据库类型映射表 */
    Map<Class<?>, DefaultTypeEnumInterface> typeMapping();

    /** 解析实体类为表元数据 */
    TableMetadata analyseClass(Class<?> entityClass);

    /** 删除表 */
    void dropTable(String schema, String tableName);

    /** 创建表 */
    void createTable(TableMetadata metadata);

    /** 对比表结构差异 */
    CompareTableInfo compareTable(TableMetadata metadata);

    /** 修改表结构 */
    void modifyTable(TableMetadata metadata, CompareTableInfo diff);

    // 可选方法（有默认实现）
    void createSchema(String schema);
    String identifier(String name);        // 标识符转义（如 MySQL 反引号）
    int indexNameMaxLength();              // 索引名最大长度
    boolean checkTableNotExist(String schema, String tableName);
    List<String> listAllTables(String schema);
}
```

### 自定义策略示例

```java
public class ClickHouseStrategy implements IStrategy {

    @Override
    public String databaseDialect() {
        return "ClickHouse";
    }

    @Override
    public Map<Class<?>, DefaultTypeEnumInterface> typeMapping() {
        Map<Class<?>, DefaultTypeEnumInterface> map = new HashMap<>();
        map.put(String.class, () -> "String");
        map.put(Long.class, () -> "Int64");
        map.put(Integer.class, () -> "Int32");
        map.put(LocalDateTime.class, () -> "DateTime");
        // ...
        return map;
    }

    @Override
    public TableMetadata analyseClass(Class<?> entityClass) {
        // 使用 DefaultTableMetadataBuilder 或自定义实现
        return DefaultTableMetadataBuilder.build(entityClass);
    }

    @Override
    public void createTable(TableMetadata metadata) {
        // 生成 ClickHouse 特有的 CREATE TABLE 语法
        String sql = buildCreateTableSql(metadata);
        executeSql(sql);
    }

    // ... 其他方法实现
}
```

注册 SPI：

```
# META-INF/services/org.dromara.autotable.core.strategy.IStrategy
com.example.ClickHouseStrategy
```

### DatabaseBuilder（自动建库）

```java
public interface DatabaseBuilder {
    String databaseDialect();
    void buildDatabase(DataSource dataSource, String dbName, String adminUser, String adminPassword);
}
```

### AutoTableMetadataAdapter（ORM 适配器）

桥接 MyBatis-Plus / JPA 等 ORM 的注解：

```java
public interface AutoTableMetadataAdapter {
    /** 获取 ORM 定义的表名 */
    String getTableName(Class<?> entityClass);

    /** 获取 ORM 定义的列名 */
    String getColumnName(Class<?> entityClass, Field field);

    /** 获取 ORM 定义的列类型 */
    String getColumnType(Class<?> entityClass, Field field);
}
```

### JavaTypeToDatabaseTypeConverter（类型转换器）

```java
public interface JavaTypeToDatabaseTypeConverter {

    DatabaseTypeAndLength convert(String dialect, Class<?> entityClass, Field field);

    /** 静态方法：添加自定义映射 */
    static void addTypeMapping(String dialect, Class<?> javaType, DefaultTypeEnumInterface typeDefine);
}
```

### AutoTableClassScanner（类扫描器）

```java
public interface AutoTableClassScanner {
    Set<Class<?>> scan(String[] basePackages, Class<?>[] classes);
}
```

### AutoTableAnnotationFinder（注解查找器）

```java
public interface AutoTableAnnotationFinder {
    <A extends Annotation> A findAnnotation(AnnotatedElement element, Class<A> annotationType);
}
```

Spring Boot 默认使用 `CustomAnnotationFinder`（基于 `AnnotatedElementUtils`），支持元注解合并。

## SQL 执行机制

- 底层使用 `commons-dbutils` 的 `QueryRunner`
- 事务批处理：`setAutoCommit(false)` → 执行所有 SQL → `commit()`
- 多语句 SQL 文件用 `JSqlParser` 解析拆分
- SQL 执行前可选记录（`RecordSqlHandler`）

## 元数据构建

### TableMetadata

```java
public interface TableMetadata {
    String getTableName();
    String getSchema();
    String getComment();
    String getDialect();
    List<ColumnMetadata> getColumns();
    List<IndexMetadata> getIndexes();
    Map<String, Object> getCustomProperties();  // 数据库专属属性
}
```

### ColumnMetadata

```java
public interface ColumnMetadata {
    String getName();
    String getType();
    Integer getLength();
    Integer getDecimalLength();
    boolean isNotNull();
    String getDefaultValue();
    String getComment();
    boolean isPrimaryKey();
    boolean isAutoIncrement();
}
```

### IndexMetadata

```java
public interface IndexMetadata {
    String getName();
    IndexTypeEnum getType();
    List<String> getColumns();
    String getMethod();
    String getComment();
}
```

### CompareTableInfo

```java
public interface CompareTableInfo {
    boolean hasDifference();
    boolean hasDropColumns();
    List<ColumnMetadata> getAddColumns();
    List<ColumnMetadata> getModifyColumns();
    List<String> getDropColumns();
    List<IndexMetadata> getAddIndexes();
    List<IndexMetadata> getDropIndexes();
}
```

## Spring Boot 自动装配

`AutoTableAutoConfig` 通过构造器注入所有组件：

1. `PropertyConfig` ← `@ConfigurationProperties("auto-table")`
2. `DataSource` ← 唯一数据源自动注入
3. 所有 `IStrategy` Bean ← 通过 SPI + Spring Bean 双通道
4. 所有 `Interceptor` Bean ← 按 `@Order` 排序
5. 所有 `Callback` Bean ← 按 `@Order` 排序
6. 可选组件：`IDataSourceHandler`、`JavaTypeToDatabaseTypeConverter`、`AutoTableMetadataAdapter` 等

## Solon 插件装配

`AutoTablePlugin` 通过生命周期钩子实现：

```java
context.lifecycle(-100, () -> {
    // 优先级 -100，确保在其他插件之前执行
    // 从 context 获取所有 Bean 并注入 AutoTableGlobalConfig
    AutoTableBootstrap.start();
});
```

差异：
- `@EnableAutoTable` 无 `basePackages` 属性（Solon 自动扫描全部）
- 无单元测试支持注解
- 手动 `context.getBean()` 替代 Spring 的 `ObjectProvider`
