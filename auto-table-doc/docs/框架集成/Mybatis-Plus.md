---
title: MybatisPlus
description:
aside: false
---

<div style="display: flex; justify-content: center;">
    <img src="/mpe-logo.png" style="max-height: 150px"/>
</div>

## [MybatisPlusExt](https://gitee.com/dromara/mybatis-plus-ext)

### 为简化开发工作、提高生产率而生

尽管[MybatisPlus](https://gitee.com/baomidou/mybatis-plus)
（后文简称MP）相比较Mybatis丝滑了很多，但是日常使用中，是否偶尔仍会怀念JPA（Hibernate）的那种纵享丝滑的感受，更好的一心投入业务开发中，如果你也是如此，那么恭喜你发现了[MybatisPlusExt](https://gitee.com/dromara/mybatis-plus-ext)
（后文简称MPE）。

MPE对MP做了进一步的拓展封装，即保留MP原功能，又添加更多有用便捷的功能。同样坚持MP的原则，只做增强不做改变，所以，即便是在使用MPE的情况下，也可以百分百的只使用MP的方式，因此MP能做的，MPE不仅能做还能做的更多。

增强功能具体体现在几个方面：`代码（字段字符串、Mapper、Repository/Service）预生成`、`自动建表`、`数据自动填充（类似JPA中的审计）`、`关联查询（类似sql中的join）`、`冗余数据自动更新`、`动态查询条件`。

::: warning 特别强调

如果你使用的ORM框架是MybatisPlus，那么强烈建议去掉MybatisPlus的引入直接引入MybatisPlusExt。

:::
