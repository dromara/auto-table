---
title: 葵花宝典
description:
---

## 集成springdoc/swagger

### 需求

实体类上使用了注解`@AutoColumn`来指定字段注释了，但是在swagger中，还需要通过swagger注解再指定一次，冗余了

### 实现

maven中额外引入依赖包即可省去写swagger注解的步骤

```xml

<dependency>
    <groupId>org.dromara.autotable</groupId>
    <artifactId>auto-table-support-springdoc</artifactId>
    <version>{最新版本}</version>
</dependency>
```
