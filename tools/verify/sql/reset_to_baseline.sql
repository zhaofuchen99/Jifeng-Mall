-- =====================================================================
-- 测试数据重置：把库恢复到「种子基线」状态
--
-- 用途：跑验证脚本套件之前 / 之后各执行一次，保证每轮测试都从同一个
--       已知状态出发 —— 这是「测试结果可复现」的前提。
--
-- 配套：同目录的 reset_to_baseline.bat
--       ⚠️ **只跑这个 .sql 是不够的**：Redis 里的 seckill:* 键不在数据库里，
--       必须由那个 .bat 一并清掉。直接 mysql < 本文件 会留下
--       seckill:stock / seckill:user / seckill:order 三组脏键，
--       导致下一轮压测出现「明明重置了库存却报售罄」的假故障。
--
-- ⚠️ 关于 `order.id > 4`：种子订单只有 1~4（见 jifeng-mall-init.sql），
--    其余全部是测试数据。这个判断在这个库里是准确的 ——
--    但它是**全量重置**语义，不是「清理某个脚本的数据」，
--    所以只能在跑套件前后用，不要在一轮套件中途执行（那会销毁别的脚本
--    尚未核对的数据）。用完会打印核对结果。
--
-- 幂等：可以重复执行。
-- =====================================================================

SET NAMES utf8mb4;

SELECT '================ 重置前 ================' AS step;

SELECT '订单' AS t, COUNT(*) AS c FROM `order`
UNION ALL SELECT '订单明细', COUNT(*) FROM order_item
UNION ALL SELECT '会员', COUNT(*) FROM member
UNION ALL SELECT '后台用户', COUNT(*) FROM `user`
UNION ALL SELECT '购物车', COUNT(*) FROM cart
UNION ALL SELECT '收货地址', COUNT(*) FROM member_address;

SELECT id, name, qty FROM good ORDER BY id;
SELECT id, stock, sold FROM seckill_good;

-- ---------------------------------------------------------------------
-- 1. 测试订单与明细
--    种子订单是 1~4：id=3 已确认（购物车单）、id=4 已取消（立即购买单）、
--    1/2 是更早的秒杀单，都是演示要保留的历史。
-- ---------------------------------------------------------------------
DELETE oi FROM order_item oi
JOIN `order` o ON o.id = oi.order_id
WHERE o.id > 4;

DELETE FROM `order` WHERE id > 4;

-- ---------------------------------------------------------------------
-- 2. 测试账号
--    各脚本自建的临时账号，前缀是有约定的：
--      fe_own_*    owner_verify.py  的越权测试会员
--      bench_*     seckill_bench.py 的压测会员
--      lg_test_*   login_guard_verify.py 的防爆破测试账号（会员 + 后台用户）
--      mrp_test_*  member_password_verify.py 的改密测试会员
--      adm_test_* / ADM_TEST  admin_verify.py 的后台用户与业务数据
--    ⚠️ 用 LIKE '前缀\_%' 并转义下划线：_ 在 LIKE 里是单字符通配符，
--       不转义会把 feXown 之类的账号也匹配上。
-- ---------------------------------------------------------------------
DELETE FROM member WHERE account LIKE 'fe\_own\_%'
                      OR account LIKE 'bench\_%'
                      OR account LIKE 'lg\_test\_%'
                      OR account LIKE 'mrp\_test\_%'
                      OR account LIKE 'fe\_sec\_%'
                      OR account LIKE 'adm\_test\_%';

DELETE FROM `user` WHERE username LIKE 'lg\_test\_%'
                     OR username LIKE 'adm\_test\_%';

-- 3. 临时账号遗留的收货地址（按会员账号关联删，比按 receiver 文案匹配可靠）
DELETE FROM member_address WHERE member_account NOT IN (SELECT account FROM member);

-- 4. 购物车清空（残留会让「同商品累加 2→3」这类断言失败）
DELETE FROM cart;

-- ---------------------------------------------------------------------
-- 5. 商品库存回到种子值
--    ⚠️ 基线是**照种子文件**的 100 / 200，不是观测值。
--    曾经有清理脚本把 196 当基线写回（那是更早的测试已消耗 4 件的残留值），
--    导致后续「下单后库存 -N」的断言全部对不上。
-- ---------------------------------------------------------------------
UPDATE good SET qty = 100 WHERE id = 1;
UPDATE good SET qty = 200 WHERE id = 2;

-- 6. 秒杀商品归位（种子 50/0）
UPDATE seckill_good SET stock = 50, sold = 0;

-- ---------------------------------------------------------------------
-- 7. 演示地址（张三/天河区）
--    不是种子数据（init 脚本里 member_address 无 INSERT），
--    是更早会话留下的，但演示收货流程要用，所以保留并归位默认标记。
-- ---------------------------------------------------------------------
UPDATE member_address SET is_default = 1 WHERE id = 1;

-- 8. 测试造的轮播 / 区划（脚本自清理失效时的兜底）
DELETE FROM banner WHERE title LIKE 'BANNER\_TEST%';
DELETE FROM t_china_region WHERE name LIKE 'REG\_TEST%' OR id BETWEEN 999001 AND 999999;

-- ---------------------------------------------------------------------
-- 核对：以下每项都应等于期望值
-- ---------------------------------------------------------------------
SELECT '================ 重置后（核对） ================' AS step;

SELECT '订单(期望4)' AS t, COUNT(*) AS c FROM `order`
UNION ALL SELECT '订单明细(期望4)', COUNT(*) FROM order_item
UNION ALL SELECT '会员(期望1)', COUNT(*) FROM member
UNION ALL SELECT '后台用户(期望2)', COUNT(*) FROM `user`
UNION ALL SELECT '购物车(期望0)', COUNT(*) FROM cart
UNION ALL SELECT '收货地址(期望1)', COUNT(*) FROM member_address
UNION ALL SELECT '轮播(期望3)', COUNT(*) FROM banner
UNION ALL SELECT '行政区划(期望5)', COUNT(*) FROM t_china_region
UNION ALL SELECT 'RBAC资源(期望28)', COUNT(*) FROM t_rbac_resource
UNION ALL SELECT '权限资源关联(期望28)', COUNT(*) FROM t_rbac_perm_resource;

SELECT '--- 商品（期望 good1=100 / good2=200）---' AS step;
SELECT id, name, qty FROM good ORDER BY id;

SELECT '--- 秒杀商品（期望 50/0）---' AS step;
SELECT id, stock, sold FROM seckill_good;

SELECT '--- 保留的种子订单 ---' AS step;
SELECT id, order_no, member_account, status, LEFT(order_comment, 20) AS cmt
FROM `order` ORDER BY id;

SELECT '--- 演示地址（期望 id=1 张三 is_default=1）---' AS step;
SELECT id, member_account, receiver, is_default FROM member_address ORDER BY id;
