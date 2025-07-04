import {defineConfig} from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
    title: "Dromara AutoTable",
    titleTemplate: "Auto Table",
    description: "自动维护数据库",
    head: [
        ['link', {rel: 'icon', href: '/logo.png'}]
    ],
    lastUpdated: true,
    build: {
        rollupOptions: {
            external: ['/flow.png']
        }
    },
    // https://vitepress.dev/reference/default-theme-config
    themeConfig: {
        logo: '/logo.png',
        search: {
            provider: 'local',
            options: {
                translations: {
                    button: {
                        buttonText: '搜索文档'
                    }
                }
            }
        },
        docFooter: {
            prev: '上一页',
            next: '下一页'
        },
        nav: [
            {text: '指南', link: '/指南/介绍/什么是AutoTable'},
            {text: '配置', link: '/配置'},
            {text: '葵花宝典', link: '/葵花宝典/说明'},
            {
                text: '数据库特性', items: [
                    {text: 'Doris', link: '/数据库特性/Doris/说明'},
                    {text: 'Oracle', link: '/数据库特性/Oracle/说明'},
                ]
            },
            {text: '变更日志', link: '/变更日志'},
            {text: '第三方框架集成', link: '/第三方框架集成/index'},
            {text: '支持/联系', link: '/支持联系/支持我'}
        ],
        outline: {
            level: 'deep',
            label: '目录',
        },
        sidebar: {
            // 当用户位于 `/指南` 目录时，会显示此侧边栏
            '/指南/': [
                {
                    text: '介绍',
                    items: [
                        {text: '什么是AutoTable', link: '/指南/介绍/什么是AutoTable'},
                        {text: '工作流程', link: '/指南/介绍/工作流程'},
                        {text: '贡献者', link: '/指南/介绍/贡献者'},
                    ]
                },
                {
                    text: '基础',
                    items: [
                        {text: '快速上手', link: '/指南/基础/快速上手'},
                    ]
                },
                {
                    text: '进阶',
                    items: [
                        {text: '定义表', link: '/指南/进阶/定义表'},
                        {text: '定义列', link: '/指南/进阶/定义列'},
                        {text: '定义索引', link: '/指南/进阶/定义索引'},
                    ]
                },
                {
                    text: '高级',
                    items: [
                        {text: '单元测试', link: '/指南/高级/单元测试'},
                        {text: '生产环境', link: '/指南/高级/开发生产环境'},
                        {text: '多数据源', link: '/指南/高级/多数据源'},
                        {text: '拦截器', link: '/指南/高级/拦截器'},
                        {text: '事件回调', link: '/指南/高级/事件回调'},
                        {text: '多库适配', link: '/指南/高级/多库适配.md'},
                        {text: '自动建库', link: '/指南/高级/自动建库.md'},
                        {text: '自动初始化数据', link: '/指南/高级/自动初始化数据.md'},
                    ]
                },
                {
                    text: '自定义',
                    items: [
                        {text: 'SQL记录', link: '/指南/自定义/SQL记录'},
                        {text: '类型映射', link: '/指南/自定义/类型映射'},
                        {text: '数据库策略', link: '/指南/自定义/数据库策略'},
                    ]
                },
            ],
            '/第三方框架集成/': [
                {
                    text: '第三方集成',
                    items: [
                        {text: 'MybatisPlus', link: '/第三方框架集成/MybatisPlus'},
                        {text: 'MybatisFlex', link: '/第三方框架集成/MybatisFlex'},
                    ]
                },
            ],
            '/葵花宝典/': [
                {
                    items: [
                        {text: '说明', link: '/葵花宝典/说明'},
                        {text: '字段排序', link: '/葵花宝典/字段排序'},
                        {text: '父类字段没有创建', link: '/葵花宝典/父类字段没有创建'},
                        {text: 'Invalid value type', link: '/葵花宝典/Invalid value type'},
                        {text: '生成Flyway脚本', link: '/葵花宝典/生成Flyway脚本'},
                        {text: '启动顺序(时机)', link: '/葵花宝典/启动顺序(时机)'},
                        {text: '字段删除', link: '/葵花宝典/字段删除'},
                        {text: '自定义类型映射', link: '/葵花宝典/自定义类型映射'},
                        {text: '没有创建表', link: '/葵花宝典/没有创建表'},
                        {text: '集成springdoc', link: '/葵花宝典/集成springdoc.md'},
                    ]
                },
            ],
            '/数据库特性/': [
                {
                    text: 'Doris',
                    items: [
                        {text: '说明', link: '/数据库特性/Doris/说明'},
                        {text: '配置', link: '/数据库特性/Doris/配置'},
                    ]
                },
                {
                    text: 'Oracle',
                    items: [
                        {text: '说明', link: '/数据库特性/Oracle/说明'},
                    ]
                }
            ],
            '/支持联系/': [
                {
                    items: [
                        {text: '支持我', link: '/支持联系/支持我'},
                        {text: '联系我', link: '/支持联系/联系我'},
                    ]
                },
            ]
        },
        socialLinks: [
            {
                icon: {
                    svg: "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" version=\"1.1\" id=\"Layer_1\" x=\"0px\" y=\"0px\" width=\"20px\" height=\"20px\" viewBox=\"0 0 20 20\" enable-background=\"new 0 0 20 20\" xml:space=\"preserve\">  <image id=\"image0\" width=\"20\" height=\"20\" x=\"0\" y=\"0\" xlink:href=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAUCAMAAAC6V+0/AAAAIGNIUk0AAHomAACAhAAA+gAAAIDo AAB1MAAA6mAAADqYAAAXcJy6UTwAAAIBUExURQAAAMjIyMbGxsyjOM2YEc2UAMjDts2VBMfU9s2T AMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjI yMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjI yMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIycjJzcjIyMjHxcnDtM2TAM2RAMjDtMjG wsuoTsuqVMq3h82VA82WBs2TAM6MAM2SAMuqVsqwa8yfKM2RAM2TAM2YD82UAM2UAM2WCM2UAM2U AM2UAMycIM2UAM2UAM2TAM2UAcycIMuoTM2TAM2UAM2UAM2RAM2QAM2RAM2UAM2XC82ZE82ZEs2X DM2VBc2UAM2TAM2TAM2UAM2UAM2UAMjIyMjIycjIxsjJy8jHxMumR82YEMm8msm8m8usW8m5kMyc HsnBrsnCr8ukPsyeJ8m4jMjHw8m5j8yhMMq1gcjIysq4i8yjO8nBrcjHwsq4isupUMurWMqyc8q2 hMq4icq2gsqxbsupT8ygMMukP8m5jsq1fsqxccnCscjGwsnBrMqydMqvaMquZsqxcMjEucm9nMuq Vs2YD8qzdsm7lMm+o8q0ecurV8ygLc2XC////3VSHUMAAABwdFJOUwAAAAAAAAAAAAAotCUdDX/6 RAyyYg/HoUg9fP2dOPD59rcGeMIKCbvECy3pwP6ZrgPFpgEg458ZSfeZG7/erB4HY/7R2fjetGAM c+H8oRnsp5H56Qpm0v26GV6n2vX2si0EIU19qMnf7PLw5MydVg9N357GAAAAAWJLR0SqvgZXvgAA AAlwSFlzAAALEgAACxIB0t1+/AAAAAd0SU1FB+gLHRAnGtjhZXMAAAQhelRYdFJhdyBwcm9maWxl IHR5cGUgeG1wAABYha1YW7KjOgz81ypmCSDJMiyHw+Nvqu7nXf60bEgcYh7JTKhDTrDd3ZJlyYb+ //0f/cKHJSrJGDsZYmdTbGyyENVabvy3jTZH8TaZmK01tcXYggz5+aP3wswNPWHw8MeHhE6n0HCj YkvEQG7EeJYm/bHMzcCNX5DAADcZAquS2o4/N7qGLiquRgZwLjF9eI7oxHOiiLxIK71fvJA0wnjA uE8ZBN8Se8BCdux4cgJvfmrZK1K1QFFM8KBPpvXwwgyFawf4hcENT7hCOOIFqLxiR0+jQgO4zgJ+ T9CiLj+AC3DTianrKCqHwXY42CZ0S+53HyQXQ6fDYZYEA1v0Cnt99CqwnHSHeleSPDY/g2Ejp8Te luznviio3Ke2zTDdYQPZOuiY7tA0+KPzwXeNI+fbBpV8+6jZYudobilN7hAEQD7VgkYEfOrSBgMF R8nxJbOIjLzUoWiNk5tQxyrpMAR30CGm+FcHVswy4mlEu6RxvfsIPLEm1hfKcmzKno7u8Z2b9bZE bpnld0skQWadfMEou2mL/axmtNLxiPttc0oaqvF8FkEZnE6MWIIgdlpB7KZAPdVaDch3uGuNdOUB DSkXMToHU5mDIkADHLBT9zHQkTb6dHaywcHjzPtbqieTzJ6zPRLcE19N+wZMe+Rvpt4JqGAIQbWX LStWV2BcB7+30lo9PNsJZgOVwksjewlHtHuxLM1BT5jj7gdoXzrj3lpbtZ6ZTd841rX6YkKVE+0V QSGRxD+GHcBfzVlk2mP/i1nbp3r+RCXVIP51GvHF2WMYDEahjFioJkEqC/YRR7dSbAFbU0x3HFnk gvz/vO4FeqdA5g4aSEVa/IN8rYEnFVymLU+iinqCbejkLYodMFqBj5nFHRPRCzAxts2hQ3+vJxtK n0dMATymKEy1j/KEW49d9egbTzjzwxjPcHS/lt4q2aiVLQIS7WmKw7b5hFZNS9Qh0Id9I7ZYXBOM Z9fRxs+dfUjXDPS9Ma9BSykNvO3MUFct1dbb6YWq+eWLRUwXwsW6nPS1S/7p8oIo10PW/EmBrIA+ tVIpdj02DOrnpVAAL7hG1P2Kr7jJORxAe9dewdU9Ro9h6wnV1o3xukEOKeXicIj93QQqzwtY+XAS o2gMSBZ+b0SJf/DVpR+veg6BrxQ9DQm31TDuKGYuhqpqqqB1Jdu5ifyw7XXNHemn1xyEwdcRwnMr So+z2WFvyt3P2aDRNz39mh2LxJM3qDmxnXa8Igj9ppBKib4ZSG8B2p05fc2gVxqq8nT1gQc0qTed 8zxpHkfT4v1AwLYDz9LmjIosZIiJ6GxXg2oJhi5ZuxoAoo9fSel60P7VCi/ly5Xt3Qo9X66Ub4ae nf3dEEoz2nLs719apcT/B3GTS4hO/VjoAAAAAW9yTlQBz6J3mgAAAR1JREFUGNNjYAABRkYubh5G RkZePkZGBhhgZOQXEGRkFBIWQRYUFSsQl5CUkpZBCDIyysoVFMgrSBcoKsEFmRiVCyBARRWhUk0d KqihyQjXraUNFdRRQwiK6EIFEWYyMupBxQr0DeCCfIYwQSNjRogyRkYTU5igGRNU0NxC2RImaMXM wsrKwMBmbWNqCxMrtLN3YAeKcjg6ObsUQQWLXd3cPTw5Gby8S0rLyouhghWVVT6+fgz+1TW1dfUN jcUgUNjU3NLaFsAQGNTe0dnV3dPb1z9h4qTJU6YWTAtmCAkNC58+Y+ashtlz5s7rmjV/wcJFEQys rJFR0TGxi5csXbqgatnyFSvj4hOAgqysiUnJKalp6RmZWdk5uXn5rADCXWWho1QI1AAAAMZlWElm TU0AKgAAAAgABgESAAMAAAABAAEAAAEaAAUAAAABAAAAVgEbAAUAAAABAAAAXgEoAAMAAAABAAIA AAExAAIAAAAWAAAAZodpAAQAAAABAAAAfAAAAAAAAABIAAAAAQAAAEgAAAABUGl4ZWxtYXRvciBQ cm8gMy42LjEwAAAEkAQAAgAAABQAAACyoAEAAwAAAAEAAQAAoAIABAAAAAEAAAM7oAMABAAAAAEA AAJ9AAAAADIwMjQ6MTE6MjkgMjM6NDI6MzYAD0oZlAAAACV0RVh0ZGF0ZTpjcmVhdGUAMjAyNC0x MS0yOVQxNjozOToyNiswMDowMJIXkdIAAAAldEVYdGRhdGU6bW9kaWZ5ADIwMjQtMTEtMjlUMTY6 Mzk6MjYrMDA6MDDjSiluAAAAKHRFWHRkYXRlOnRpbWVzdGFtcAAyMDI0LTExLTI5VDE2OjM5OjI2 KzAwOjAwtF8IsQAAABF0RVh0ZXhpZjpDb2xvclNwYWNlADEPmwJJAAAAKnRFWHRleGlmOkRhdGVU aW1lRGlnaXRpemVkADIwMjQ6MTE6MjkgMjM6NDI6MzbJx0c8AAAAE3RFWHRleGlmOkV4aWZPZmZz ZXQAMTI0qBfuEAAAABh0RVh0ZXhpZjpQaXhlbFhEaW1lbnNpb24AODI35bGMwwAAABh0RVh0ZXhp ZjpQaXhlbFlEaW1lbnNpb24ANjM3aztx/gAAACN0RVh0ZXhpZjpTb2Z0d2FyZQBQaXhlbG1hdG9y IFBybyAzLjYuMTD2Hj05AAAANXRFWHRQaXhlbG1hdG9yVGVhbTpTaWRlY2FyQmFzZUZpbGVuYW1l AGRyb21hcmEtbG9nby1zbWFsbPxYHwwAAAAjdEVYdFBpeGVsbWF0b3JUZWFtOlNpZGVjYXJEYXRh VmVyc2lvbgAzV+10rwAAACJ0RVh0UGl4ZWxtYXRvclRlYW06U2lkZWNhckVuYWJsZWQAVHJ1ZYdd Tp8AAABFdEVYdFBpeGVsbWF0b3JUZWFtOlNpZGVjYXJJZGVudGlmaWVyAEVFQzFFQThFLUNDRkEt NEJBNi04RUI3LUVGRDVGMjk2RjFBML4DzTkAAAAldEVYdFBpeGVsbWF0b3JUZWFtOlNpZGVjYXJM b2NhdGlvbgBpQ2xvdWRxUojTAAAAKHRFWHRQaXhlbG1hdG9yVGVhbTpTaWRlY2FyU2hvcnRIYXNo AEVFQzFFQThF5SEByQAAAFN0RVh0UGl4ZWxtYXRvclRlYW06U2lkZWNhclVUSQBjb20ucGl4ZWxt YXRvcnRlYW0ucGl4ZWxtYXRvci5kb2N1bWVudC1wcm8tc2lkZWNhci5iaW5hcnliPETaAAAAH3RF WHRQaXhlbG1hdG9yVGVhbTpTaWRlY2FyVmVyc2lvbgAy114zawAAADV0RVh0UGl4ZWxtYXRvclRl YW06U2lkZWNhcldyaXRlckFwcGxpY2F0aW9uAHBpeGVsbWF0b3JQcm+NlAJCAAAAKXRFWHRQaXhl bG1hdG9yVGVhbTpTaWRlY2FyV3JpdGVyQnVpbGQAZjdlNjU1Zewt+nAAAAAxdEVYdFBpeGVsbWF0 b3JUZWFtOlNpZGVjYXJXcml0ZXJEZXZpY2UATWFjQm9va1BybzE4LDG3HGQcAAAAJXRFWHRQaXhl bG1hdG9yVGVhbTpTaWRlY2FyV3JpdGVyT1MAMTUuMS4xO1mJmQAAACp0RVh0UGl4ZWxtYXRvclRl YW06U2lkZWNhcldyaXRlclBsYXRmb3JtAG1hY09TekrFCgAAACt0RVh0UGl4ZWxtYXRvclRlYW06 U2lkZWNhcldyaXRlclByb2Nlc3NJRAA3MzM2MMgdFKwAAAAqdEVYdFBpeGVsbWF0b3JUZWFtOlNp ZGVjYXJXcml0ZXJWZXJzaW9uADMuNi4xMLH85p8AAAASdEVYdHRpZmY6T3JpZW50YXRpb24AMber /DsAAAAodEVYdHhtcDpDcmVhdGVEYXRlADIwMjQtMTEtMjlUMjM6NDI6MzYrMDg6MDDipQtKAAAA JXRFWHR4bXA6Q3JlYXRvclRvb2wAUGl4ZWxtYXRvciBQcm8gMy42LjEwwGsoXgAAACp0RVh0eG1w Ok1ldGFkYXRhRGF0ZQAyMDI0LTExLTI5VDIzOjQzOjA0KzA4OjAwnC0ZOQAAABd0RVh0eG1wOlBp eGVsWERpbWVuc2lvbgA4Mjf/vsFOAAAAF3RFWHR4bXA6UGl4ZWxZRGltZW5zaW9uADYzN3E0PHMA AAAASUVORK5CYII=\"/>\n" +
                        "<script xmlns=\"\"/></svg>"
                }, link: 'https://dromara.org',
                ariaLabel: "Dromara"
            },
            {
                icon: {
                    svg: "<svg xmlns=\"http://www.w3.org/2000/svg\" id=\"Group\" viewBox=\"0 0 89.7088726 89.7088726\" width=\"20\" height=\"20\">\n" +
                        "    <g>\n" +
                        "        <circle id=\"Combined-Shape\" fill=\"#C71D23\" cx=\"44.8544363\" cy=\"44.8544363\" r=\"44.8544363\"/>\n" +
                        "        <path d=\"M67.558546,39.8714292 L42.0857966,39.8714292 C40.8627004,39.8720094 39.8710953,40.8633548 39.8701949,42.0864508 L39.8687448,47.623783 C39.867826,48.8471055 40.8592652,49.8390642 42.0825879,49.8393845 C42.0827874,49.8393846 42.0829869,49.8393846 42.0831864,49.8387862 L57.5909484,49.838657 C58.8142711,49.8386283 59.8059783,50.830319 59.8059885,52.0536417 C59.8059885,52.0536479 59.8059885,52.053654 59.8059701,52.0536602 L59.8059701,52.6073539 L59.8059701,52.6073539 L59.8059701,53.161115 C59.8059701,56.8310831 56.8308731,59.80618 53.160905,59.80618 L32.1165505,59.80618 C30.8934034,59.806119 29.9018373,58.8145802 29.9017425,57.5914331 L29.9011625,36.5491188 C29.9008781,32.8791508 32.8758931,29.9039718 36.5458611,29.9038706 C36.5459222,29.9038706 36.5459833,29.9038706 36.5460443,29.9040538 L67.5523638,29.9040538 C68.77515,29.9026795 69.7666266,28.9118177 69.7687593,27.6890325 L69.7721938,22.1516997 C69.774326,20.928378 68.7832423,19.9360642 67.5599198,19.9353054 C67.5594619,19.9353051 67.5590039,19.935305 67.558546,19.9366784 L36.5479677,19.9366784 C27.3730474,19.9366784 19.935305,27.3744208 19.935305,36.549341 L19.935305,67.558546 C19.935305,68.7818687 20.927004,69.7735676 22.1503267,69.7735676 L54.8224984,69.7735676 C63.079746,69.7735676 69.7735676,63.079746 69.7735676,54.8224984 L69.7735676,42.0864509 C69.7735676,40.8631282 68.7818687,39.8714292 67.558546,39.8714292 Z\" id=\"G\" fill=\"#FFFFFF\"/>\n" +
                        "    </g>\n" +
                        "</svg>"
                }, link: 'https://gitee.com/tangzc/auto-table',
                ariaLabel: "Gitee"
            },
            {
                icon: "github",
                link: 'https://github.com/dromara/auto-table',
                ariaLabel: "GitHub"
            },
            {
                icon: {
                    svg: "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"20\" height=\"20\" viewBox=\"0 0 28 28\" fill=\"none\">\n" +
                        "    <path fill-rule=\"evenodd\" clip-rule=\"evenodd\" d=\"M18.059 5.80628C18.2234 5.71425 18.3973 5.61696 18.585 5.51037C18.6076 5.63327 18.6297 5.74058 18.6497 5.83784C18.685 6.00943 18.714 6.15059 18.728 6.29005C18.8392 7.50125 19.448 8.39222 20.3108 8.59286C21.5726 8.88583 22.7623 8.40159 23.4033 7.33318C24.1733 6.05123 23.839 4.4812 22.5279 3.53618C18.8826 0.907049 14.8777 0.18191 10.5636 1.44819C1.2616 4.1927 -1.92121 15.6199 4.68062 22.6274C7.50507 25.6249 11.0914 26.9182 15.1624 26.8204C20.3774 26.6979 24.1333 24.099 26.5309 19.5947C28.2308 16.3988 26.3829 12.9055 22.8439 12.1795C20.8227 11.7726 18.7559 11.6405 16.6993 11.7869C16.0151 11.8526 15.3509 12.0547 14.7459 12.3811C14.0691 12.7324 13.8734 13.4614 13.9493 14.1838C14.02 14.8421 14.5247 15.2369 15.1258 15.3362C16.3361 15.5256 17.5609 15.6357 18.7833 15.7361C19.1371 15.7659 19.4942 15.7694 19.8507 15.773C20.3623 15.7781 20.873 15.7832 21.3718 15.8657C22.7949 16.1009 23.2836 17.2557 22.5517 18.4911C22.3724 18.7882 22.1633 19.0662 21.9277 19.3209C20.9703 20.3738 19.7183 21.1144 18.3344 21.4465C15.8084 22.0649 13.2798 22.0996 10.7655 21.3054C7.90238 20.4021 6.19549 18.2991 6.13552 15.4682C6.1131 13.7223 6.55634 12.002 7.41963 10.4843C7.80967 9.77685 8.02376 9.04827 7.96359 8.24664C7.93826 7.90488 7.92423 7.56273 7.90915 7.19506C7.90113 6.99938 7.89281 6.79647 7.88233 6.58254C8.17231 6.6434 8.45871 6.72023 8.74022 6.81271C9.83531 7.2523 10.9132 7.45284 12.0986 7.13019C12.7728 6.96895 13.4697 6.92433 14.159 6.99829C15.269 7.08878 16.3785 6.81759 17.3215 6.22521C17.5569 6.08724 17.7963 5.9533 18.059 5.80628Z\" fill=\"#DA203E\"/>\n" +
                        "</svg>"
                }, link: 'https://gitcode.com/dromara/auto-table', ariaLabel: "GitCode"
            }
        ]
    },
    markdown: {
        container: {
            tipLabel: '提示',
            warningLabel: '警告',
            dangerLabel: '危险',
            infoLabel: '信息',
            detailsLabel: '详细信息'
        }
    }
})
