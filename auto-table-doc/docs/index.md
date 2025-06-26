---
# https://vitepress.dev/reference/default-theme-home-page
layout: home

hero:
  name: "AutoTable"
  #  text: "--自动维护表结构"
  tagline: --你只负责维护实体，数据库的事交给我
  image:
    src: /home-bg.png
    alt: AutoTable
  actions:
    - theme: brand
      text: 什么是AutoTable？
      link: /指南/介绍/什么是AutoTable
    - theme: alt
      text: 快速上手
      link: /指南/基础/快速上手
    - theme: alt
      text: 🌟支持一下
      link: https://gitee.com/tangzc/auto-table

features:
  - icon: { src: '/全面.png', width: '100px', height: '100px' }
    title: 数据库全面支持
    details: 支持数据库MySQL、MariaDB、PostgreSQL、SQLite、H2 (其他还在积极适配ing~)
  - icon: { src: '/兼容适配.png', width: '100px', height: '100px' }
    title: 三方框架兼容适配
    details: 抽象了能力接口，可以对接多种ORM框架，例如MybatisPlus、MybatisFlex等
  - icon: { src: '/智能.png', width: '100px', height: '100px' }
    title: 表结构智能同步
    details: 根据不同数据源，自动维护数据库（mysql可保持字段顺序与列顺序一致）、索引等信息
---

<style>

.VPHome {

    margin-bottom: 0 !important;
    height: calc(100vh);

    .VPHomeHero {
        .container {
            margin-top: 100px;
            .main {
                .name {
                    .clip {
                        font-size: 100px;
                    }
                }
            }
        }
    }

    .VPHomeFeatures {
        height: calc(100vh - 508px);
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: flex-end;
    }

    /* 针对手机端的样式 */
    @media (max-width: 768px) {
        .VPHomeHero {
            .container {
                margin-top: unset;
            }
        }

        .VPHomeFeatures {
            height: calc(100vh - 200px);
        }

        .name {
            line-height: 64px;
        }
    }
}

:root {
  --vp-home-hero-name-color: transparent;
  --vp-home-hero-name-background: -webkit-linear-gradient(120deg, #00FFFF, #8A2BE2);
  --vp-home-hero-image-background-image: linear-gradient(-45deg, #8A2BE2 50%, #00FFFF 50%);
  --vp-home-hero-image-filter: blur(100px);
}

</style>

