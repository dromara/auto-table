---
title: 自定义SQL记录
description:
---

# 自定义SQL记录

当[默认sql记录方案](/指南/高级/开发生产环境.html#开发环境)不满足需要的情况下，

想实现个性化的SQL记录方案，可以通过自定义新的实现方式。

我们假设一个场景：在使用sql文件记录的过程中，我们可能在开发环境使用AutoTable，生产环境使用Flyway等工具进行sql脚本管理，希望两者可以很好的无缝结合，省去了手动构建Flyway脚本的工作。

### 方案讲解

> 因为同样也是文件存储的方式，所以可以在系统内置的文件存储方式的基础上做一个拓展

框架内部sql文件记录方式，预留了重写存储路径的方法。

```java
public class RecordSqlFileHandler implements RecordSqlHandler {
    
    ......

    /**
     * 希望自定义文件全路径的话，可以重写此方法
     */
    protected Path getFilePath(AutoTableExecuteSqlLog autoTableExecuteSqlLog) {
        ......
    }
}
```

因此，我们只需要重写该方法，同时把sql记录方案改为自定义即可。

### 实现步骤

> 

#### 1、修改配置

```yaml{8,9}
auto-table:
  enable: true
  record-sql:
    # 开启SQL记录
    enable: true
    # 当前版本（此处可保持与计划上线的版本号一致，方便管理SQL文件）
    version: 1.0.0
    # 自定义记录方式
    record-type: custom
```

#### 2、自定义实现类

::: code-group

```java [SpringBoot应用]
/**
 * 自定义sql记录，文件名以Flyway方式生成
 */
@Component
public class RecordSqlFlywayHandler extends RecordSqlFileHandler {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    protected Path getFilePath(AutoTableExecuteSqlLog autoTableExecuteSqlLog) {

        String sqlFilename = "V{version}_{time}__{table}.sql"
                .replace("{version}", autoTableExecuteSqlLog.getVersion())
                .replace("{time}", LocalDateTime.ofInstant(Instant.ofEpochMilli(autoTableExecuteSqlLog.getExecutionTime()), ZoneId.systemDefault()).format(dateTimeFormatter))
                .replace("{table}", autoTableExecuteSqlLog.getTableName());

        return Paths.get("/Users/don/Downloads/sqlLogs", sqlFilename);
    }
}
```

```java [普通java]
/**
 * 自定义sql记录，文件名以Flyway方式生成
 */
public class RecordSqlFlywayHandler extends RecordSqlFileHandler {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    protected Path getFilePath(AutoTableExecuteSqlLog autoTableExecuteSqlLog) {

        String sqlFilename = "V{version}_{time}__{table}.sql"
                .replace("{version}", autoTableExecuteSqlLog.getVersion())
                .replace("{time}", LocalDateTime.ofInstant(Instant.ofEpochMilli(autoTableExecuteSqlLog.getExecutionTime()), ZoneId.systemDefault()).format(dateTimeFormatter))
                .replace("{table}", autoTableExecuteSqlLog.getTableName());

        return Paths.get("/Users/don/Downloads/sqlLogs", sqlFilename);
    }
}

/* 注入自定义处理器 */
PropertyConfig.RecordSqlProperties recordSqlProperties = new PropertyConfig.RecordSqlProperties();
recordSqlProperties.setEnable(true);
recordSqlProperties.setVersion(Version.VALUE);
// 指定自定义的方式
recordSqlProperties.setRecordType(PropertyConfig.RecordSqlProperties.TypeEnum.custom);
// 指定自定义的实现
AutoTableGlobalConfig.setCustomRecordSqlHandler(new RecordSqlFlywayHandler());

AutoTableGlobalConfig.PropertyConfig autoTableProperties = new AutoTableGlobalConfig.PropertyConfig();
autoTableProperties.setRecordSql(recordSqlProperties);
AutoTableGlobalConfig.setAutoTableProperties(autoTableProperties);
```

:::
