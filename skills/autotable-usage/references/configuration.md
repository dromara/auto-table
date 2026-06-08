# AutoTable 配置参考

所有配置项前缀：`auto-table`

## 基础配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enable` | `Boolean` | `true` | 总开关 |
| `mode` | `RunMode` | `update` | 运行模式：`none`/`validate`/`create`/`update` |
| `show-banner` | `Boolean` | `true` | 启动时打印 banner |
| `model-package` | `String[]` | `{}` | 实体扫描包路径（空则取主类所在包） |
| `model-class` | `Class[]` | `{}` | 直接指定实体类 |
| `index-prefix` | `String` | `"auto_idx_"` | 自动索引名前缀 |

## 自动删建配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `auto-build-database` | `Boolean` | `false` | 自动创建数据库（需配 admin-user/password） |
| `auto-drop-table` | `Boolean` | `false` | 删除未在实体中声明的表（⚠️ 危险） |
| `auto-drop-table-prefix` | `String[]` | `{}` | 只删除匹配这些前缀的表 |
| `auto-drop-table-ignores` | `String[]` | `{}` | 不删除这些表名 |
| `auto-drop-column` | `Boolean` | `false` | 删除未在实体中声明的列（⚠️ 丢数据） |
| `logic-drop-column-prefix` | `String` | `null` | 逻辑删除：重命名而非删除（如 `deleted_`） |
| `auto-drop-index` | `Boolean` | `true` | 删除不在实体中的自动前缀索引 |
| `auto-drop-custom-index` | `Boolean` | `false` | 删除不在实体中的自定义索引 |

## 继承与排序

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `strict-extends` | `Boolean` | `true` | 只继承 public/protected 字段 |
| `super-insert-position` | `enum` | `after` | 父类字段位置：`before`/`after` 子类字段 |

## MySQL 专属

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `mysql.table-default-charset` | `String` | `null` | 表默认字符集 |
| `mysql.table-default-collation` | `String` | `null` | 表默认排序规则 |
| `mysql.column-default-charset` | `String` | `null` | 列默认字符集 |
| `mysql.column-default-collation` | `String` | `null` | 列默认排序规则 |
| `mysql.admin-user` | `String` | `null` | 自动建库管理员账号 |
| `mysql.admin-password` | `String` | `null` | 自动建库管理员密码 |
| `mysql.alter-table-separate-drop` | `boolean` | `false` | ALTER TABLE 中 DROP 语句单独执行 |

> **`alter-table-separate-drop`**：部分云数据库（如阿里云 RDS）安全策略禁止 ALTER TABLE 中混合 DROP 和 ADD，开启此选项将 DROP COLUMN 单独拆成独立 SQL。

## PostgreSQL 专属

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `pgsql.pk-auto-increment-type` | `enum` | `byDefault` | 自增主键类型：`always`/`byDefault` |
| `pgsql.admin-user` | `String` | `null` | 自动建库管理员 |
| `pgsql.admin-password` | `String` | `null` | 自动建库密码 |

> **`always` vs `byDefault`**：`always` 禁止手动插入 ID；`byDefault` 允许手动指定（推荐，兼容性更好）。

## Oracle / 达梦 / 人大金仓 / H2 专属

各有独立的 `admin-user` / `admin-password` 配置，用于自动建库：

```yaml
auto-table:
  oracle:
    admin-user: sys
    admin-password: xxx
  dm:
    admin-user: SYSDBA
    admin-password: xxx
  kingbase:
    admin-user: system
    admin-password: xxx
  h2:
    admin-user: sa
    admin-password: ""
```

## Doris 专属

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `doris.rollup-prefix` | `String` | `"auto_rlp_"` | 物化视图名前缀 |
| `doris.rollup-auto-name-max-length` | `int` | `100` | 自动物化视图名最大长度 |
| `doris.update-limit-table-data-length` | `long` | `1073741824` (1GB) | 超过此大小的表跳过 update |
| `doris.update-backup-old-table` | `boolean` | `false` | update 时备份旧表 |

## SQL 审计记录

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `record-sql.enable` | `boolean` | `false` | 启用 SQL 记录 |
| `record-sql.record-type` | `enum` | `db` | `db`/`file`/`custom` |
| `record-sql.version` | `String` | `null` | 版本号标签 |
| `record-sql.table-name` | `String` | `null` | 记录到 DB 时的表名 |
| `record-sql.folder-path` | `String` | `null` | 记录到文件时的目录 |
| `record-sql.datasource.*` | - | `null` | 独立数据源配置（记录到其他库） |

## 数据初始化

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `init-data.enable` | `boolean` | `true` | 启用数据初始化 |
| `init-data.base-path` | `String` | `"classpath:sql"` | SQL 文件根目录 |
| `init-data.default-init-file-name` | `String` | `"_init_"` | 全局初始化文件名 |

## 完整 YAML 示例

```yaml
auto-table:
  enable: true
  mode: update
  show-banner: true
  model-package: com.example.entity
  index-prefix: "auto_idx_"
  auto-build-database: false
  auto-drop-table: false
  auto-drop-column: false
  logic-drop-column-prefix: "deleted_"
  auto-drop-index: true
  auto-drop-custom-index: false
  strict-extends: true
  super-insert-position: after

  mysql:
    table-default-charset: utf8mb4
    table-default-collation: utf8mb4_general_ci
    column-default-charset: utf8mb4
    column-default-collation: utf8mb4_general_ci

  pgsql:
    pk-auto-increment-type: byDefault

  doris:
    rollup-prefix: "auto_rlp_"
    update-limit-table-data-length: 2147483648

  record-sql:
    enable: true
    record-type: file
    version: 1.0.0
    folder-path: ./flyway-migrations

  init-data:
    enable: true
    base-path: "classpath:sql"
    default-init-file-name: "_init_"
```

## Spring Boot properties 格式

```properties
auto-table.enable=true
auto-table.mode=update
auto-table.model-package=com.example.entity
auto-table.auto-drop-table=false
auto-table.auto-drop-column=false
auto-table.mysql.table-default-charset=utf8mb4
auto-table.record-sql.enable=true
auto-table.record-sql.record-type=db
```
