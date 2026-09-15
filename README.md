# 极锋商城（Jifeng Mall）— 基于 Spring Cloud 的 B2C 微服务秒杀商城

微服务后端工程 + PC 商城前端。依据《需求规格说明书 V1.0.4》《详细设计说明书 V1.0.2》构建。

> **改名说明**：本工程原名 `shoplook2026`，2026-09-12 更名为 **Jifeng Mall / 极锋商城**。
> 代码标识符（Java 包、Maven 模块、Spring 服务名、Nacos 命名空间与 group）已全部更名；
> **数据库名 `shoplook2026` 与上传目录 `d:/shoplook2026/upload/` 有意保持不变**，避免数据迁移风险。

## 技术栈

| 项 | 选型 |
| --- | --- |
| Java / Spring Boot | 25 / 4.1.1 |
| Spring Cloud / Alibaba | 2025.1.3 / 2025.1.0.0 |
| 注册/配置中心 | Nacos 3.x（namespace=`jifeng-mall`，group=`jifeng-mall`） |
| 网关 | Spring Cloud Gateway（**WebFlux**） |
| 持久层 | MyBatis + PageHelper（非 MyBatis-Plus） |
| 服务调用 | OpenFeign + LoadBalancer |
| 缓存 | Redis（秒杀库存 + Lua 原子扣减） |
| 消息队列 | RabbitMQ（订单超时关单 / 秒杀异步下单） |
| 认证 | JWT（jjwt，HS384，admin 12h / member 7d）+ BCrypt |
| 数据库 | MySQL 8.0（库 `shoplook2026`，名称沿用旧名） |
| 前端 | Vue 3 + Vite + Pinia + Vue Router + Axios（+ Element Plus） |

## 模块结构

```
shoplook2026-parent/            # 仓库根目录名沿用旧名（改名会破坏 IDE 工程与运行中的进程）
├── jifeng-mall-common          # 统一响应 JsonResp/TypedJsonResp、分页、审计实体、JWT、密码、雪花、全局异常
├── jifeng-mall-spi             # 模型(实体/SearchBean) + 业务接口（各 api 服务契约层）
├── jifeng-mall-api             # 微服务实现（聚合）
│   ├── jifeng-mall-brand-api           10010  品牌
│   ├── jifeng-mall-category-api        10011  分类(树)
│   ├── jifeng-mall-good-api            10012  商品（含扣减/回补库存、首页轮播）
│   ├── jifeng-mall-member-api          10013  会员注册/登录
│   ├── jifeng-mall-member-address-api  10014  收货地址
│   ├── jifeng-mall-region-api          10015  行政区划
│   ├── jifeng-mall-cart-api            10016  购物车
│   ├── jifeng-mall-order-api           10017  订单/模拟支付/退款/超时关单/秒杀下单
│   ├── jifeng-mall-user-api            10018  后台用户/登录
│   ├── jifeng-mall-rbac-api            10019  RBAC 权限判定/动态菜单
│   ├── jifeng-mall-upload-api          10020  文件上传
│   └── jifeng-mall-seckill-api         10021  秒杀（Redis+Lua 抢购 + 异步下单）
├── jifeng-mall-gateway-webflux   8888  网关（JWT 校验 + RBAC 判定 + 限流 + CORS）
├── mall-web                      5173  PC 商城前端（会员端，Vue 3 + Vite）
└── admin-web                     5174  后台管理系统（管理端，Vue 3 + Element Plus + Vite）
```

> 两个前端用**不同的 localStorage 键**存令牌（`mall_token` / `admin_token`），
> 可以在同一个浏览器里同时登录会员与管理员，方便联调。

## 数据库

初始化脚本：`docs/sql/jifeng-mall-init.sql`（23 张数据表 + 索引 + 演示种子数据；
第 23 张 `banner` 是本期补充，设计说明书未建模）。
脚本内建的库名仍是 `shoplook2026`（有意保留），导入即可。

## 起步步骤

1. 启动中间件：MySQL、Redis、RabbitMQ、Nacos。
2. 导入 `docs/sql/jifeng-mall-init.sql`。
3. 在 Nacos 中准备配置：命名空间 `jifeng-mall`、group `jifeng-mall`，发布
   `jifeng-mall-common.yaml` 与 `gateway-sentinel-flow-rules.json`
   （源文件见 `jifeng-mall-api/docs/远程配置/`）。详见 `docs/Nacos配置导入清单.md`。
4. 依次启动网关与各业务服务（经 Nacos 注册）。
5. 启动前端（两个工程各自 `npm install` / `npm run dev`）：
   - `mall-web` → http://localhost:5173，演示账号 `member / 123456`
   - `admin-web` → http://localhost:5174，演示账号 `admin / 123456`
     （`operator / 123456` 未分配角色，登录后菜单为空，用于演示权限隔离）

> 以上是**开发形态**（各服务在 IDE 里跑、前端用 vite dev server）。
> **生产形态**（fat jar + Nginx 托管前端静态资源并反代网关）的完整步骤见
> `docs/部署文档.md`，Nginx 配置在 `deploy/nginx/nginx.conf`。

## 已实现能力

- 会员注册/登录（BCrypt + JWT 签发 member 令牌）；后台登录（签发 admin 令牌）
- 登录防爆破（需求 6.2 / NFR-002）：同一账号连续失败 5 次锁定 15 分钟（Redis 计数 + TTL，
  到期自动解锁），锁定期内即使密码正确也拒绝；登录成功清零；账号不存在同样计数以防账号枚举。
  与后台用户的**手工锁定** `user.locked`（后台用户管理里切换、不自动解除）是两回事
- 网关：JWT GlobalFilter（白名单 + 身份注入 `X-User-Id/Name/Audience`）、CORS、RBAC 判定过滤器
- RBAC：权限判定 `/api/rbac/check`、动态菜单 `/api/menus/mine`
- 商品中心：品牌/分类/商品 CRUD、商品详情 Feign 组装、扣减/回补库存、首页轮播 CRUD
- 首页轮播（需求 5.3 / FR-102「可配置图片与跳转链接」）：`banner` 表 + `GET /api/goods/banners`。
  **详细设计说明书没有给轮播建服务与表**（2.4 节 13 个服务、3.3 节 14 组表里都没有），
  为不擅自新增服务，轮播并入 good-api；因此路径挂在 `/api/goods/**` 下，
  直接复用已有的公开读白名单与商品管理权限资源，不必改网关、也不必补 RBAC 种子数据。
  详见 `docs/接口文档.md` 的轮播一节与 `docs/首页轮播图来源.md`
- 交易中心：下单（购物车/立即购买，事务扣库存+快照+清购物车）、订单状态机、模拟支付、超时关单（RabbitMQ 延时消息）
- 退款（模拟，FR-206）：后台两步式 `PUT /api/orders/{id}/refund` → `/{id}/refund/confirm`，
  `refund_status` 无退款 → 退款中 → 已退款；确认后订单转「已取消」并按订单类型回补库存
  （秒杀单回补秒杀库存并释放该会员的抢购名额），操作人记入 `updated_by`；
  **退款在途（退款中/已退款）的订单不能再发货、也不能确认收货**
- 秒杀：活动/秒杀商品 CRUD、Redis+Lua 原子扣减（防超卖）、限购防重、RabbitMQ 异步下单、抢购结果查询
- 秒杀库存一致性：成交侧 DB 原子扣减（`decreaseStock`，条件更新兜底防超卖）、异步下单按 `seckillNo` 幂等、
  超时关单同步回补 Redis 与 `seckill_good.stock/sold` **并释放该会员的抢购名额**、
  秒杀库存定时预热（见 `docs/接口文档.md` 5.3 说明）

## 前端

### mall-web（PC 商城 / 会员端）

16 个页面：登录、注册、首页、商品列表、商品详情、购物车、订单确认、模拟收银台、
支付结果、帮助中心、个人中心（个人信息/收货地址/我的订单/订单详情/修改密码）、秒杀会场、404。

### admin-web（后台管理端）

21 个页面：登录、工作台、品牌、分类（树+级联删除）、商品（图片上传/上下架/热销）、
轮播（首页 Banner：图片上传/跳转链接/排序/启用开关）、
订单（详情含明细、发货、取消、发起退款/确认退款、退款状态列）、会员、秒杀活动、秒杀商品、系统管理（用户/用户组/角色/权限/资源/菜单
及四张关联表）、地区（省/市/区树，层级自动推导、级联删除）、个人中心。

侧边栏由 `/api/menus/mine` **动态渲染**，菜单 url 与前端路由对齐；
`t_rbac_menu` 里存的是栏目地址（`/goods`），栏目下的具体页面在 `src/config/menu.js` 里声明。

## 待完善 / 后续

- 监控（Prometheus+Grafana）、链路追踪（P2）
- 商品列表的价格区间筛选与排序目前在前端做：后端 `GoodSearchBean` 不支持
  `keyword`/`priceLow`/`priceHigh`/`sort`（接口文档写了但实现里没有），
  商品量大时应补后端查询参数

## 文档

- `docs/部署文档.md` — **环境搭建与部署步骤**（Nacos / MySQL / Redis / RabbitMQ /
  Nginx / 13 个服务 / 2 个前端），每条命令均实机跑通
- `docs/测试报告.md` — **功能 / 接口 / 安全测试报告**（11 个脚本、346 条断言）
- `docs/运维手册.md` — **监控、备份、故障排查**（含 9 条已知风险清单）
- `docs/交付物清单.md` — **交付物对照 + SRS 10.2 验收六条逐项核对**
- `docs/sql/upgrade/` — 数据库**增量变更脚本**（3 个，幂等）
- `docs/接口文档.md` — 前后端接口契约
- `docs/Nacos配置导入清单.md` — Nacos 配置发布步骤
- `docs/秒杀压测报告.md` — 秒杀并发压测（防超卖/防重复/最终一致 + 性能对照 NFR-001）
- `docs/商品图来源.md` — 演示商品图的来源与版权提示
- `docs/首页轮播图来源.md` — 首页轮播图的生成方式与前端显示约定
- `tools/verify/README.md` — **验证脚本使用说明**（前置条件、执行顺序、数据重置）
- `deploy/nginx/nginx.conf` — 生产形态的 Nginx 配置（已实测）
- `deploy/windows/{start,stop}_all_services.bat` — **不依赖 IDEA 的一键启停**（13 个服务走 fat jar）
