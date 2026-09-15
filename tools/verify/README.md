# 验证脚本（功能 / 接口 / 并发）

联调期写的自动化验证脚本，覆盖《需求规格说明书》的验收标准 1~5。
每一条断言都对应一个真实接口调用，跑完会打印 `合计 N 项：通过 X，失败 Y`。

**这些脚本是《测试报告》的证据来源** —— 报告里的每个数字都能在这里复现。

## 一、脚本清单

| 脚本 | 覆盖范围 | 断言 | 对应需求 | 需要 Redis/MQ |
| --- | --- | --- | --- | --- |
| `fe_verify.py` | 前台契约：登录、商品、分类、地址 CRUD、购物车、下单两条路径、支付、发货、确认收货、状态机、秒杀异步下单 | 43 | 验收 1 | 是 |
| `admin_verify.py` | 后台契约：登录、动态菜单、品牌/分类/商品/秒杀/RBAC 全套 CRUD、四张关联表、密码 BCrypt | 53 | 验收 2 | 否 |
| `rbac_verify.py` | 越权拒绝：零授权/会员令牌的后台访问边界、公开路径放行（**纯只读**） | 15 | 验收 2、6 | 否 |
| `owner_verify.py` | 数据归属：订单、明细、会员资料、购物车、收货地址的越权访问 | 23 | 需求 7.2-3 | 否 |
| `refund_verify.py` | 订单退款：权限、状态机、退款状态流转、库存回补、秒杀退款后名额释放 | 43 | FR-206 | 是 |
| `banner_verify.py` | 首页轮播：游客可读、写权限、排序、停用不可见、图片可访问 | 26 | FR-102 / 5.3 | 否 |
| `member_password_verify.py` | 会员改密 / 后台重置密码、密码强度校验、防爆破锁联动 | 33 | FR-207 / FR-101 | 是 |
| `region_verify.py` | 地区管理：层级推导、编码唯一、成环检测、级联删除、**缓存失效**、菜单挂载 | 42 | FR-210 | 是 |
| `login_guard_verify.py` | 登录防爆破：阈值、锁定期、TTL 递减、成功清零、账号隔离（**约 2 分钟**） | 24 | 需求 6.2 / NFR-002 | 是 |
| `order_timeout_verify.py` | 超时关单**兜底扫描**：未超时不误关、超时关闭+回补、已支付不误关、幂等（**约 4 分钟**） | 17 | 设计 6.2 | 否 |
| `delete_reference_verify.py` | 品牌/分类**删除禁引校验**：有商品拒删、子分类有商品拒删父分类、空树级联仍可用 | 21 | 设计 5.3 | 否 |
| `seckill_bench.py` | 秒杀并发压测：防超卖、防少卖、防重复下单、库存最终一致、延迟分位 | 6 组 | 验收 3、5 | 是 |
| | **合计** | **346** | | |

`seckill_bench.py` 还额外输出 P50/P95/P99/max 与吞吐，详见 `docs/秒杀压测报告.md`。

## 二、怎么跑

### 前置

1. 13 个后端服务 + 网关（8888）已启动；
2. 数据库已导入 `docs/sql/jifeng-mall-init.sql`，并已执行过 RBAC 补丁（见 §四）；
3. **被测地址可达** —— 二选一：
   - 开发形态：`mall-web` 的 vite dev server 在跑（5173），脚本走它的代理；
   - **部署形态：Nginx 在跑（8090）**，脚本走 `dist` 托管 + 反代，与浏览器同路径。
4. 秒杀相关脚本要求活动未过期（见 §五）。

### 命令

脚本默认打 `http://172.22.96.1:5173`（WSL 里访问 Windows 宿主要用宿主 IP，
`localhost` 在 WSL 里走不通）。**用环境变量换成 Nginx 的地址即可**：

```bash
cd tools/verify

# 打开发形态的 vite dev server
python3 fe_verify.py

# 打部署形态的 Nginx（本机 8090 = 前台 mall-web）
JIFENG_BASE=http://172.22.96.1:8090 python3 fe_verify.py

# 压测直连网关（不经前端，另有独立变量）
JIFENG_GATEWAY=http://172.22.96.1:8888 python3 seckill_bench.py 200
```

可用变量：

| 变量 | 默认值 | 用途 |
| --- | --- | --- |
| `JIFENG_BASE` | `http://172.22.96.1:5173` | 所有契约脚本的被测地址（vite 或 Nginx） |
| `JIFENG_GATEWAY` | `http://172.22.96.1:8888` | 压测直连网关 |
| `JIFENG_ORDER_API` | `http://172.22.96.1:10017` | `owner_verify.py` 直连 order-api 模拟服务间调用 |

脚本只用 Python 标准库，**没有任何第三方依赖**。

### 需要写库的脚本

`order_timeout_verify.py` 要改订单的 `created_time`（模拟"这单 40 分钟前就该关了"），
是本目录唯一需要访问数据库的脚本。它不直连 —— MySQL 的 root 只允许 localhost 登录，
WSL 直连会被拒 —— 而是把 SQL 写进临时文件、交给
`tools/verify/sql/run_sql.bat` 执行（该 bat 跑在 Windows 侧，天然是 localhost）。

**所以临时 SQL 文件必须落在 Windows 也能看到的路径上**，
默认 `/mnt/d/shoplook2026/_verify_tmp.sql`（即 `D:\shoplook2026\_verify_tmp.sql`）。
换机器时用环境变量调整：`JIFENG_RUN_SQL_BAT`、`JIFENG_SQL_TMP_WSL`、`JIFENG_SQL_TMP_WIN`。

## 三、执行顺序与数据重置

每轮测试前后各执行一次重置，保证从同一个已知状态出发：

```
tools/verify/sql/reset_to_baseline.bat      # Windows 侧执行（cmd 或双击）
```

它做两件事，**缺一不可**：

1. **MySQL**：删除测试订单/临时账号/购物车，把商品库存、秒杀库存、
   演示地址还原成种子基线；
2. **Redis**：删除 `seckill:user:*` 与 `seckill:order:*`，并把
   `seckill:stock:1` 写回 50。

> ⚠️ **只跑同目录的 `.sql` 是不够的。** 秒杀状态有一部分只存在 Redis 里，
> SQL 够不着。上一轮压测留下的限购标记 `seckill:user:{memberId}:{goodId}`
> **没有 TTL**，不清掉的话下一轮压测会直接报 7003「已参与过该秒杀」——
> 表现为「明明重置了库存却抢不到」，极难排查。
> 这正是本套脚本原先不可复现的原因，现已修掉。

`.bat` 会自检并在失败时以非零码退出（残留键 → 2，库存不对 → 3），
不会出现「报成功但没清干净」。

推荐顺序（秒杀相关脚本放后面，压测最后）：

```bash
reset_to_baseline.bat
python3 rbac_verify.py            # 纯只读，随时可跑
python3 admin_verify.py
python3 banner_verify.py
python3 member_password_verify.py
python3 region_verify.py
python3 fe_verify.py
python3 owner_verify.py
python3 refund_verify.py
python3 order_timeout_verify.py   # 约 4 分钟（含 3 个扫描周期等待）
python3 delete_reference_verify.py
python3 login_guard_verify.py     # 约 2 分钟
python3 seckill_bench.py          # 最后跑，它会打满库存
reset_to_baseline.bat             # 收尾
```

自清理情况：`banner` / `member_password` / `region` / `login_guard`
四个脚本跑完自己删干净；其余会留下测试数据，由重置脚本统一清。

## 四、RBAC 种子补丁

`docs/sql/jifeng-mall-init.sql` 已包含全部 RBAC 种子（资源 28 条、权限 6 条、
权限-资源关联 28 条）。如果你是从更早的库升上来的，需要补：

- 资源 `2009~2020` —— 缺了会被网关 403（`rbac_verify.py` / `admin_verify.py` 会失败）
- 资源 `1007` / `2021` + 权限 `306` —— 地区管理（FR-210），缺了
  `region_verify.py` 的写操作断言会全部失败

## 五、已知限制

诚实列出这套脚本的边界，避免把它的结论用过头：

1. **秒杀活动会过期。** `seckill` 表的活动有时间窗，过期后预热任务静默退出、
   Redis 里 `seckill:stock:*` 消失且**不留日志**。续命：
   ```sql
   UPDATE seckill SET start_time = NOW() - INTERVAL 1 HOUR,
                      end_time = NOW() + INTERVAL 24 HOUR WHERE id = 1;
   ```
2. **`owner_verify.py` 依赖种子订单 id=3**（
   `联调测试订单-购物车`，归属 `member`）作为越界访问的目标。
   重置脚本会保留它，但如果你手工删过订单，这个脚本会失败。
3. **`login_guard_verify.py` 会锁定账号 15 分钟。** 它刻意不用 `admin` / `member`
   演示账号做锁定测试，而是现场注册 `lg_test_*` 临时账号并跑完删掉。
   **不要改成用 admin 或 member 跑**，锁上就没法演示了。
4. **压测结论仅适用于开发环境。** 单机单实例、客户端与被测服务同机争抢 CPU，
   只压了抢购提交接口。详见 `docs/秒杀压测报告.md` 第 6 节。
5. **脚本断言的是当前实现的行为**，不是需求的绝对真值。若实现变更，
   需要同步改断言 —— 断言本身也可能有 bug（本项目联调中就出现过
   「被测代码正确、测试写错」的假失败，见 `docs/测试报告.md`）。
