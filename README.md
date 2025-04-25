<p align="center"><img src="https://autotable.tangzc.com/logo.png" alt="logo" width="150px" /></p>

<p align="center">你只负责维护实体，数据表的事情交给我</p>

<p align="center">
<img src="https://img.shields.io/maven-central/v/com.baomidou/mybatis-plus.svg?style=for-the-badge" alt="img" /> 
<img src="https://img.shields.io/badge/license-Apache 2-4EB1BA.svg?style=for-the-badge" alt="img" />
</p>

## 什么是AutoTable？

顾名思义-自动表格，根据Bean实体，自动映射成数据库的表结构。

用过JPA的都知道，JPA有一项重要的能力就是表结构自动维护（JPA的也不完善），这让我们可以可以专注于业务逻辑和实体，而不需要关心数据库的表、列的配置，尤其是开发过程中频繁的新增表及变更表结构，节省了大量手动工作。

但是JAVA圈子这么大，在JPA之外一直缺少这种体验，所以AutoTable应运而生了。

## 兼容多款ORM框架

AutoTable分别在表名、字段名、主键、主键策略、枚举处理、多数据源等多方面进行接口化抽取，兼容了大部分主流Mybatis拓展框架，如Mybatis-plus、Mybatis-flex等。

### mybatis-plus拓展包

<a href="https://gitee.com/dromara/mybatis-plus-ext" target="_blank">mybatis-plus-ext</a>
是mybatis-plus框架的拓展包，在框架原有基础上做了进一步的轻度封装，增强内容：免手写Mapper、多数据源自动建表(auto-table)、数据自动填充、自动关联查询、冗余数据自动更新、动态查询条件等。

### mybatis-flex拓展包

<a href="https://gitee.com/tangzc/mybatis-flex-ext" target="_blank">mybatis-flex-ext</a>
是mybatis-flex框架的拓展包，在框架原有基础上做了进一步的轻度封装，增强内容：多数据源自动建表(auto-table)、数据自动填充（类似JPA中的审计）。

## 支持的数据库

> 以下的测试版本是我本地的版本或者部分小伙伴测试过的版本，更低的版本未做详细测试，但不代表不能用，所以有测试过其他更低版本的小伙伴欢迎联系我修改相关版本号，感谢🫡

| 数据库          | 测试版本                  | 说明                                           |
|--------------|-----------------------|----------------------------------------------|
| ✅ MySQL      | 5.7                   |                                              |
| ✅ MariaDB    | 对应MySQL的版本            | 协议使用MySQL，即`jdbc:mysql://`                   |
| ✅ PostgreSQL | 15.5                  |                                              |
| ✅ SQLite     | 3.35.5                |                                              |
| ✅ H2         | 2.2.220               |                                              |
| ☑️ 达梦        | dm8(大小写不敏感,兼容mysql模式) | 成员[@minfc](https://gitee.com/minfc)开发测试中     |
| ☑️ 人大金仓      | V009R001C002B0014     | 成员[@minfc](https://gitee.com/minfc)开发测试中     |
| ☑️ Doris     | 未知                    | 成员[@lizhian](https://gitee.com/lizhian)开发测试中 |
| 其他数据库        | 暂未支持                  | 期待你的PR😉                                     |

## 流程图

![框架运行图](https://autotable.tangzc.com/flow.png)

## 官方教程

<a style="font-size:20px" href="https://autotable.tangzc.com" target="_blank">AutoTable教程</a>

## 联系作者（微信）

![微信](https://autotable.tangzc.com/wechat.png)

## 特别感谢

> 感谢JetBrains提供的软件支持

<img width="200" src="https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.png" alt="JetBrains Logo (Main) logo.">
