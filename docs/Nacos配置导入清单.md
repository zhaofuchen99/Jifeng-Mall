# 启动前置配置清单（Nacos / MySQL / Redis / RabbitMQ）

> 本文档整理「从零到服务可启动」需要准备的中间件与配置，用于本机/已有环境部署。
> 对应版本：Nacos 3.x、MySQL 9.2.7、Redis、RabbitMQ 3.x；后端 Java 25。

---

## 1 关键约定（务必与代码一致）

| 项 | 值 |
| --- | --- |
| Nacos 地址 | `127.0.0.1:8848`，账号 `nacos`，密码填你自己设的 |
| **namespace** | `jifeng-mall`（12 个 api 服务 + 网关统一使用） |
| **group** | `jifeng-mall` |
| 配置格式 | `jifeng-mall-common.yaml` 为 **YAML**；`gateway-sentinel-flow-rules.json` 为 **JSON** |

> ⚠️ 本次已修复：原先有 7 个服务（member/member-address/region/order/user/rbac/upload）使用了**另一个 namespace**
> `0b29d6ab-a008-4147-a036-3a7cc4f05221`，与网关/其余服务隔离，导致跨服务发现与 Feign 调用不通。现已全部统一为上表 namespace。

---

## 2 Nacos 准备

### 2.1 建 namespace

控制台 → 命名空间 → 新建，**命名空间 ID 必须填** `jifeng-mall`
（ID 必须完全一致；若你想换 ID，需全局替换所有 `application.yaml` 与本文档中的该 ID）。
group `jifeng-mall` 无需预建，发布配置时填写即可。

### 2.2 导入配置（共 2 个 dataId，均在上述 namespace + group 下）

| dataId | 格式 | 内容来源 |
| --- | --- | --- |
| `jifeng-mall-common.yaml` | YAML | 仓库 `jifeng-mall-api/docs/远程配置/` 下的 common 配置 |
| `gateway-sentinel-flow-rules.json` | JSON | 仓库 `jifeng-mall-api/docs/远程配置/gateway-sentinel-flow-rules.json` |

`jifeng-mall-common.yaml` 是**公共配置单一来源**，包含：数据源、MyBatis、PageHelper、Redis、RabbitMQ。
各 api 服务的 `application.yaml` 通过 `spring.config.import: nacos:jifeng-mall-common.yaml` 引入，**只保留服务名与端口**。

> ⚠️ **该配置有两个版本，别拿错**：
> - `jifeng-mall-common.yaml` —— **模板**，密码全是 `<你的XXX密码>` 占位符，提交进仓库用；
> - `jifeng-mall-common.local.yaml` —— **本机真实值**，已被 `.gitignore` 忽略不会提交。
>
> **发布到 Nacos 请用 `.local.yaml` 那一份**；如果你拿的是模板，把它拷成 `.local.yaml`、填上自己的密码再发布。
> 直接发布模板会连不上 MySQL（认证失败，报错很直白，不会静默出错）。

> 若本机 Redis 未设密码：把 `spring.data.redis.password` 删掉或留空（当前就是留空）。

---

## 3 中间件准备

### 3.1 MySQL

- 建库：`shoplook2026`（utf8mb4 / InnoDB）
- 导入：`docs/sql/jifeng-mall-init.sql`（23 张表 + 索引 + 种子数据）
- 账号密码填你自己的，必须与 Nacos 里 common 配置的 `spring.datasource` 一致。
  建议**不要**用 root 跑应用，建一个只授权本库的专用账号：
  ```sql
  CREATE USER 'jifeng'@'localhost' IDENTIFIED BY '你的密码';
  GRANT ALL PRIVILEGES ON `shoplook2026`.* TO 'jifeng'@'localhost';
  ```
  这样即使配置泄露，丢的也只是这一个库，不是整个 MySQL。

种子账号（密码均为 `123456`，BCrypt 存储）：

| 账号 | 用途 |
| --- | --- |
| `admin` | 后台超级管理员 |
| `operator` | 后台运营 |
| `member` | 前台演示会员 |

### 3.2 Redis

- 地址 `127.0.0.1:6379`，密码见 §2.2 说明（当前未设密码，配置里留空）
- 用于：秒杀库存 `seckill:stock:*`、限购 `seckill:user:*`、订单映射 `seckill:order:*`、
  登录失败计数 `login:fail:*`

### 3.3 RabbitMQ

- 地址 `127.0.0.1:5672`，账号 `guest`，密码填你自己设的（RabbitMQ 默认 `guest`，
  且默认只允许 localhost 登录）
- 建议开启 Management 插件（控制台 15672）
- 用于：订单超时关单（TTL + 死信）、秒杀异步下单

---

## 4 启动顺序建议

1. Nacos → MySQL → Redis → RabbitMQ
2. 后端服务（顺序不严格，建议先被依赖方）：
   `rbac-api`、`user-api`、`member-api`、`region-api`、`member-address-api`、`brand-api`、`category-api`、`good-api`、`cart-api`、`order-api`、`upload-api`、`seckill-api`
3. 最后启动 `gateway-webflux`（8888）

> 网关需在服务注册后方能路由；秒杀（seckill-api）与订单（order-api）存在互相 Feign 调用，两者都启动后功能才完整。

---

## 5 启动后自检

- Nacos「服务列表」应看到 13 个实例（12 api + gateway），命名空间为 `4efec2d9-…`
- 网关连通性：`GET http://localhost:8888/api/goods?pageNo=1&pageSize=5` 应返回统一 `JsonResp`
- 秒杀库存预热：启动 seckill-api 后约 10 秒，日志出现「秒杀库存预热完成」；
  Redis 中应存在 `seckill:stock:{seckillGoodId}`（间隔可通过 `jifeng-mall.seckill.preheat-interval-ms` 调整）

---

（完）
