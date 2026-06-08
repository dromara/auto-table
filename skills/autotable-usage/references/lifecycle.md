# AutoTable 生命周期：回调与拦截器

AutoTable 提供 **4 个拦截器**（修改/阻断流程）和 **10 个回调**（事后通知），覆盖完整执行周期。

## 执行流程图

```
AutoTableBootstrap.start()
  │
  ├─ 1. 检查 enable / mode（none 则退出）
  ├─ 2. 打印 Banner
  ├─ 3. 注册 SPI 策略（IStrategy）+ 数据库构建器（DatabaseBuilder）
  ├─ 4. 扫描实体类（@AutoTable，排除 @Ignore）
  │     └─ [拦截器] AutoTableAnnotationInterceptor  ← 可修改包含/排除注解列表
  ├─ 5. 🔔 AutoTableReadyCallback（所有类扫描完成）
  │
  ├─ 6. 按数据源分组处理：
  │     ├─ 切换数据源
  │     ├─ 自动建库（如开启）
  │     │     └─ 🔔 CreateDatabaseFinishCallback
  │     ├─ 逐实体执行策略：
  │     │     ├─ 🔔 RunBeforeCallback
  │     │     ├─ 构建 TableMetadata
  │     │     │     └─ [拦截器] BuildTableMetadataInterceptor  ← 可修改元数据
  │     │     ├─ 按 mode 执行：
  │     │     │     ├─ validate → 🔔 ValidateFinishCallback
  │     │     │     ├─ create → DROP + CREATE
  │     │     │     │     ├─ [拦截器] CreateTableInterceptor
  │     │     │     │     └─ 🔔 CreateTableFinishCallback
  │     │     │     └─ update → 对比结构 → 修改
  │     │     │           ├─ 🔔 CompareTableFinishCallback
  │     │     │           ├─ [拦截器] ModifyTableInterceptor
  │     │     │           └─ 🔔 ModifyTableFinishCallback
  │     │     ├─ 数据初始化（仅 create 模式）
  │     │     └─ 🔔 RunAfterCallback
  │     ├─ 删除未注册表（如开启 auto-drop-table）
  │     │     └─ 🔔 DeleteTableFinishCallback
  │     └─ 初始化库级数据
  │
  └─ 7. 🔔 AutoTableFinishCallback（全部完成）
```

## 拦截器（4 个）

拦截器可以**修改执行流程或阻断操作**。

### `AutoTableAnnotationInterceptor`

**时机**：扫描实体类之前，可修改哪些注解被视为"需要建表"的标志。

```java
@Component
public class MyAnnotationInterceptor implements AutoTableAnnotationInterceptor {
    @Override
    public void intercept(List<Class<? extends Annotation>> includeAnnotations,
                          List<Class<? extends Annotation>> excludeAnnotations) {
        // 添加自定义注解作为建表标志
        includeAnnotations.add(MyCustomTableAnnotation.class);
    }
}
```

### `BuildTableMetadataInterceptor`

**时机**：TableMetadata 构建完成后、执行建表/改表之前。**最常用的拦截器**，可修改表结构。

```java
@Component
public class TenantColumnInterceptor implements BuildTableMetadataInterceptor {
    @Override
    public void intercept(String dialect, TableMetadata tableMetadata) {
        // 为所有表自动添加 tenant_id 字段
        tableMetadata.addColumn(new ColumnMetadata(
            "tenant_id", "bigint", null, null, true, null, "租户ID"
        ));
    }
}
```

### `CreateTableInterceptor`

**时机**：CREATE TABLE SQL 执行之前，可阻断建表。

```java
@Component
public class AuditCreateInterceptor implements CreateTableInterceptor {
    @Override
    public void beforeCreateTable(String dialect, TableMetadata tableMetadata) {
        log.info("即将创建表: {}", tableMetadata.getTableName());
        // 可抛异常阻断建表
    }
}
```

### `ModifyTableInterceptor`

**时机**：ALTER TABLE SQL 执行之前，可阻断修改。

```java
@Component
public class SafeModifyInterceptor implements ModifyTableInterceptor {
    @Override
    public void beforeModifyTable(String dialect, TableMetadata tableMetadata,
                                   CompareTableInfo compareTableInfo) {
        // 检查是否有 DROP COLUMN 操作
        if (compareTableInfo.hasDropColumns()) {
            log.warn("表 {} 将删除列，请确认", tableMetadata.getTableName());
        }
    }
}
```

## 回调（10 个）

回调是**事后通知**，不能修改流程。所有回调接口都在 `org.dromara.autotable.core.callback` 包下。

### `AutoTableReadyCallback`

**时机**：所有实体类扫描完成，处理开始前。

```java
@Component
public class ScanCompleteCallback implements AutoTableReadyCallback {
    @Override
    public void ready(Set<Class<?>> entityClasses) {
        log.info("扫描到 {} 个实体类", entityClasses.size());
    }
}
```

### `RunBeforeCallback`

**时机**：每个实体开始处理前。

```java
@Component
public class EntityStartCallback implements RunBeforeCallback {
    @Override
    public void before(Class<?> entityClass) {
        log.info("开始处理实体: {}", entityClass.getSimpleName());
    }
}
```

### `RunAfterCallback`

**时机**：每个实体处理完成后。

```java
@Component
public class EntityDoneCallback implements RunAfterCallback {
    @Override
    public void after(Class<?> entityClass) {
        log.info("实体处理完成: {}", entityClass.getSimpleName());
    }
}
```

### `CreateDatabaseFinishCallback`

**时机**：自动创建数据库后。

```java
@Component
public class DbCreatedCallback implements CreateDatabaseFinishCallback {
    @Override
    public void afterCreateDatabase(String dataSource, Set<Class<?>> classes, Object dbInfo) {
        log.info("数据库创建完成: {}", dataSource);
    }
}
```

### `CreateTableFinishCallback`

**时机**：CREATE TABLE 完成后。

```java
@Component
public class TableCreatedCallback implements CreateTableFinishCallback {
    @Override
    public void afterCreateTable(String dialect, TableMetadata metadata) {
        log.info("表 {} 创建完成（方言: {}）", metadata.getTableName(), dialect);
    }
}
```

### `CompareTableFinishCallback`

**时机**：表结构对比完成后（update 模式）。

```java
@Component
public class TableComparedCallback implements CompareTableFinishCallback {
    @Override
    public void afterCompareTable(String dialect, TableMetadata metadata,
                                   CompareTableInfo compareInfo) {
        if (compareInfo.hasDifference()) {
            log.info("表 {} 结构有差异，即将修改", metadata.getTableName());
        }
    }
}
```

### `ModifyTableFinishCallback`

**时机**：ALTER TABLE 完成后。

```java
@Component
public class TableModifiedCallback implements ModifyTableFinishCallback {
    @Override
    public void afterModifyTable(String dialect, TableMetadata metadata,
                                  CompareTableInfo compareInfo) {
        log.info("表 {} 结构已更新", metadata.getTableName());
    }
}
```

### `DeleteTableFinishCallback`

**时机**：删除未注册表后。

```java
@Component
public class TableDeletedCallback implements DeleteTableFinishCallback {
    @Override
    public void afterDeleteTables(String schema, String tableName) {
        log.info("已删除未注册表: {}.{}", schema, tableName);
    }
}
```

### `ValidateFinishCallback`

**时机**：validate 模式下校验完成后。

```java
@Component
public class ValidateResultCallback implements ValidateFinishCallback {
    @Override
    public void validateFinish(ValidateStatus status, String dialect,
                                CompareTableInfo compareInfo) {
        if (status == ValidateStatus.MISMATCH) {
            log.error("表结构不匹配: {}", compareInfo.getTableName());
        }
    }
}
```

### `AutoTableFinishCallback`

**时机**：所有处理完成后（最末回调）。

```java
@Component
public class AllDoneCallback implements AutoTableFinishCallback {
    @Override
    public void finish(Set<Class<?>> entityClasses) {
        log.info("AutoTable 全部执行完成，共处理 {} 个实体", entityClasses.size());
    }
}
```

## Spring Boot 中注册

所有拦截器和回调只需实现接口并标注 `@Component`，Spring Boot starter 自动发现并注入。多个同类型实现时按 Spring 的 `@Order` 排序。

## Solon 中注册

实现接口后注册为 Bean 即可，Solon plugin 自动通过 `context.getBeansOfType()` 发现。

## 自定义注解场景（InitializeBeans）

Spring Boot 中，实现 `InitializeBeans` 接口的 Bean 会在 AutoTable 启动前先初始化：

```java
@Component
public class PreSetupBean implements InitializeBeans {
    // 在 AutoTable 执行前完成准备工作
}
```
