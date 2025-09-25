---
title: 葵花宝典
description:
---

## 自定义sql记录数据源

### 需求

不想污染当前数据库的表，希望把修改表sql记录到其他数据源中

当多数据源的场景下，我们需要记录sql的时候，不希望表的修改sql记录到对应的数据源中，而是希望统一记录到某个数据源中，便于集中管理。

### 功能

框架提供了配置项[record-sql.datasource](/配置.html#datasource)


