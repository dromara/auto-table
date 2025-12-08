---
title: 葵花宝典
description:
---

## Invalid value type for attribute ‘factoryBeanObjectType‘: java.lang.String

> 来自用户[@m774461674](https://gitee.com/m774461674)的投稿

#### 背景

项目中使用 mybatis-plus-boot-starter 当前最新版本 3.5.4.1 ，其中依赖的 mybatis-spring 版本为 2.1.1。

#### 分析

mybatis-spring 2.1.1 版本的 ClassPathMapperScanner#processBeanDefinitions 方法里将 BeanClassName 赋值给 String
变量，并将beanClassName 赋值给 factoryBeanObjectType，但是在 Spring Boot 3.2
版本中FactoryBeanRegistrySupport#getTypeForFactoryBeanFromAttributes方法已变更，如果 factoryBeanObjectType 不是
ResolvableType 或 Class 类型会抛出 IllegalArgumentException 异常。此时因为 factoryBeanObjectType 是 String 类型，不符合条件而抛出异常。

#### 解决

Mybatis-Plus 于 2023年12月24日发布 mybatis-plus v3.5.5 版本，发布日志声明升级spring-boot3版本mybatis-spring至3.0.3。所以升级
Mybatis-Plus 版本为 3.5.5 版本即可，需要注意下 Maven 的坐标标识是mybatis-plus-spring-boot3-starter，这点和SpringBoot 2
的依赖坐标mybatis-plus-boot-starter有所区别。手动将myabtis-spring降为3.0.3
