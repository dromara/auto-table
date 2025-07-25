---
title: 什么是AutoTable？
description: 根据 Java 实体，自动映射成数据库的表结构
---

# 什么是AutoTable？

顾名思义-自动表格，根据 Java 实体，自动映射成数据库的表结构。

用过 `JPA` 的都知道，`JPA` 有一项重要的能力就是表结构自动维护，这让我们可以专注于业务逻辑和实体，而不需要关心数据库的表、列的配置，尤其是对于开发阶段需要频繁的新增表及变更表结构，节省了大量手动工作。

但是在 `JPA` 圈子外，一直缺少这种体验，所以 `AutoTable` 应运而生了，功能更是强于 `JPA` ，所以使用 `JPA` 的小伙伴也可以考虑使用 `AutoTable`。

## 兼容性

目前在 `Mybatis` 生态中，存在着许多主流的ORM框架，他们在部分概念上与 `AutoTable`
有所重合，例如表名、字段名等，AutoTable在可能存在重合的多方面进行接口化抽象，可以优雅的拓展主流框架的建表能力，详细兼容情况可以查看 [第三方框架集成](/第三方框架集成/index.md)。

<!-- @include: @/common/ORM框架支持表格.md-->

## 支持的数据库

> 以下的测试版本是我本地的版本或者部分小伙伴测试过的版本，更低的版本未做详细测试，但不代表不能用，所以有测试过其他更低版本的小伙伴欢迎联系我修改相关版本号，感谢🫡

| 数据库                                             | 测试版本                  | 说明                                        |
|-------------------------------------------------|-----------------------|-------------------------------------------|
| ✅ MySQL                                         | 5.7                   |                                           |
| ✅ MariaDB                                       | 对应MySQL的版本            | 协议使用MySQL，即`jdbc:mysql://`                |
| ✅ PostgreSQL                                    | 15.5                  |                                           |
| ✅ SQLite                                        | 3.35.5                |                                           |
| ✅ H2                                            | 2.2.220               |                                           |
| ✅ Doris <Badge type="warning" text="^2.4.2" />  | 2.0                   | 成员[@lizhian](https://gitee.com/lizhian)开发 |
| ✅ Oracle <Badge type="warning" text="^2.5.0" /> | 11g && 23ai           | 成员[@lizhian](https://gitee.com/lizhian)开发 |
| ✅️ 达梦 <Badge type="warning" text="^2.5.0" />    | dm8(大小写不敏感,兼容mysql模式) | 成员[@minfc](https://gitee.com/minfc)开发     |
| ✅ 人大金仓 <Badge type="warning" text="^2.5.0" />   | V009R001C002B0014     | 成员[@minfc](https://gitee.com/minfc)开发     |
| 其他数据库                                           | 暂未支持                  | 期待你的PR😉                                  |
