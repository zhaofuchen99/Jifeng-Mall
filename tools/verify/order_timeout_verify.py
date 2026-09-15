#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
订单超时关单**兜底扫描**验证（设计文档 6.2「RabbitMQ 延时消息 + 定时扫描兜底」）。

背景：超时关单的主路径是 RabbitMQ 延时消息。这条路径是单点的 —— 消息一丢，
订单就永远停在「待付款」、库存永远不回补。OrderTimeoutCloseTask 是它的保险。

怎么测的（关键）：
  兜底的截止时刻是「订单超时时长 + 宽限期」= 30 + 5 = 35 分钟。
  等 35 分钟不现实，所以直接**把订单的 created_time 回拨 40 分钟** ——
  对一个"当时创建、现在还没被关"的订单来说，这与真实超时状态完全等价，
  且**不需要改任何生产配置**（测的就是真实的 30 分钟超时）。
  下单时投出的 MQ 延时消息还有 30 分钟才到点，测试期间不会来干扰。

覆盖：
  1) 没超时的待付款订单**不能被误关**（截止时刻判断正确）
  2) 超时的待付款订单**被兜底关闭**，且库存回补
  3) 已支付订单**不能被兜底误关**（只处理待付款）
  4) **幂等**：关闭后再扫一轮，不重复关、不重复回补库存
  5) 清理，回到基线

读走 HTTP 接口、只把「回拨时间 / 清理」这两个写操作交给 mysql.exe：
MySQL 的 root 只允许 localhost 登录，WSL 直连会被拒（Access denied for root@<wsl-ip>），
而 mysql.exe 跑在 Windows 侧，天然是 localhost。

前置：
  - order-api 已启动（且是含 OrderTimeoutCloseTask 的版本）
  - 建议先跑 tools/verify/sql/reset_to_baseline.bat
"""
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request

# ---- 被测地址 --------------------------------------------------------
BASE = os.environ.get("JIFENG_BASE", "http://172.22.96.1:5173")

# ---- 数据库（只用于回拨 created_time 与清理）-------------------------
# 通过 tools/verify/sql/run_sql.bat 执行：脚本把 SQL 写进临时文件再交给它。
# 不要把语句作为参数直接传给 cmd.exe —— Python 的 argv 重新加引号 + cmd 剥外层引号
# 会让 "C:\Program Files\..." 变成裸的 C:\Program 而报「不是内部或外部命令」。
RUN_SQL_BAT = os.environ.get(
    "JIFENG_RUN_SQL_BAT",
    r"D:\IDEA_projects\shoplook2026-parent\tools\verify\sql\run_sql.bat")
# 临时 SQL 文件必须放在 Windows 也能看到的路径上
RUN_SQL_TMP_WSL = os.environ.get("JIFENG_SQL_TMP_WSL", "/mnt/d/shoplook2026/_verify_tmp.sql")
RUN_SQL_TMP_WIN = os.environ.get("JIFENG_SQL_TMP_WIN", r"D:\shoplook2026\_verify_tmp.sql")

T_ORDER = "`order`"     # order 是 MySQL 保留字，必须加反引号

MARK = "OT_TEST"
GOOD_ID = 2             # 用普通商品（不碰秒杀链路），基线库存 200
ADDR_ID = 1             # 演示地址（归属 member）
BACKDATE_MIN = 40       # 回拨到多少分钟前（> 30+5 的兜底截止时刻）
SCAN_CYCLE = 70         # 兜底扫描默认间隔 60s，留 10s 余量

results = []


def call(method, path, token=None, body=None, timeout=25):
    req = urllib.request.Request(
        BASE + path,
        data=json.dumps(body).encode("utf-8") if body is not None else None,
        method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode("utf-8"))
        except Exception:
            return e.code, {}
    except Exception as e:
        return 0, {"_err": str(e)}


def sql(statement):
    """
    执行一条 SQL（不解析结果，只用于「回拨时间」「清理」这类写操作）。

    为什么绕这么一圈：MySQL 的 root 只允许 localhost 登录，WSL 直连会
    Access denied；所以必须走 Windows 侧的 mysql.exe。而把语句作为参数
    直接交给 cmd.exe 会被 Python 的 argv 重新加引号、又被 cmd 剥掉外层引号
    （详见 run_sql.bat 头部注释），所以改成「写临时文件 → 调包装 bat」。
    """
    try:
        with open(RUN_SQL_TMP_WSL, "w", encoding="utf-8", newline="\n") as f:
            f.write("SET NAMES utf8mb4;\n" + statement.rstrip().rstrip(";") + ";\n")
    except Exception as e:
        print(f"    !! 写临时 SQL 失败: {e}")
        return False

    try:
        r = subprocess.run(["cmd.exe", "/c", RUN_SQL_BAT, RUN_SQL_TMP_WIN],
                           capture_output=True, timeout=60)
    except Exception as e:
        print(f"    !! 执行 SQL 异常: {e}")
        return False

    if r.returncode != 0:
        # mysql 的输出是 Windows 本地编码（GBK），别按 UTF-8 解会崩
        err = (r.stderr or r.stdout).decode("gbk", errors="replace")
        err = " ".join(err.split())[:220]
        print(f"    !! SQL 失败(rc={r.returncode}): {err}")
        return False
    return True


def check(name, cond, detail=""):
    results.append((name, bool(cond)))
    print(f"  [{'PASS' if cond else 'FAIL'}] {name}" + (f"   {detail}" if detail else ""))
    return bool(cond)


def ok(j):
    return isinstance(j, dict) and j.get("success") is True


def good_qty():
    """商品库存走 HTTP 读（公开接口）"""
    st, j = call("GET", f"/api/goods/id/{GOOD_ID}")
    return (j.get("data") or {}).get("qty") if ok(j) else None


def order_status(oid, token):
    """
    读订单状态；订单不存在时返回 "__GONE__"。

    ⚠️ 注意实现细节：`GET /api/orders/id/{id}` 对**不存在的订单**返回的是
    HTTP 200 + `{"code":200,"success":true,"data":null}`，而不是 404 ——
    该接口的 controller 直接 `JsonResp.success(orderService.findById(id))`，
    没有像 cancel/pay 那样走 requireOrder。所以判断「已删除」要看 data 是否为 null，
    不能只看状态码。（这是既有的接口不一致，与本次改动无关。）
    """
    st, j = call("GET", f"/api/orders/id/{oid}", token)
    if st == 200 and ok(j):
        data = j.get("data")
        if not data:
            return "__GONE__"
        return data.get("status")
    if st == 404 or (isinstance(j, dict) and j.get("code") == 404):
        return "__GONE__"
    return None


def backdate(oid, minutes=BACKDATE_MIN):
    """把订单创建时间回拨 —— 模拟「这个订单 40 分钟前就该被关了」"""
    return sql(f"UPDATE {T_ORDER} SET created_time = NOW() - INTERVAL {minutes} "
               f"MINUTE WHERE id = {oid}")


def wait_for(fn, want, timeout=SCAN_CYCLE * 2, interval=5):
    deadline = time.time() + timeout
    while time.time() < deadline:
        if fn() == want:
            return True
        time.sleep(interval)
    return False


def summary():
    total = len(results)
    passed = sum(1 for _, c in results if c)
    print()
    print("=" * 78)
    print(f" 合计 {total} 项：通过 {passed}，失败 {total - passed}")
    print("=" * 78)
    if passed != total:
        print(" 失败项：")
        for n, c in results:
            if not c:
                print(f"   - {n}")
    return passed == total


def main():
    # ---------------------------------------------------------------
    print("=" * 78)
    print(" 第 1 部分：准备")
    print("=" * 78)

    st, j = call("POST", "/api/members/login", body={"account": "member", "password": "123456"})
    mtoken = j["data"]["token"] if ok(j) else None
    if not check("会员登录", mtoken is not None, f"HTTP {st}"):
        return summary()

    qty0 = good_qty()
    check("读到商品基线库存（建议先跑 reset_to_baseline.bat）", qty0 is not None,
          f"good#{GOOD_ID} qty={qty0}")
    if qty0 is None:
        return summary()

    # ---------------------------------------------------------------
    print()
    print("=" * 78)
    print(" 第 2 部分：未超时的待付款订单不能被误关")
    print("=" * 78)

    def create_order(comment):
        st, j = call("POST", "/api/orders/create", mtoken, {
            "goodId": GOOD_ID, "qty": 1, "addrId": ADDR_ID,
            "comment": comment, "memberAccount": "member"})
        return (j.get("data") or {}) if ok(j) else None

    oa = create_order(MARK + "-pending")
    if not check("创建待付款订单 A", oa is not None, f"id={(oa or {}).get('id')}"):
        return summary()
    oid_a = oa["id"]
    check("  A 初始状态为待付款", oa.get("status") == "待付款", f"status={oa.get('status')}")

    qty_after_a = good_qty()
    check("  下单已扣库存", qty_after_a == qty0 - 1, f"{qty0} → {qty_after_a}")

    # 已支付订单 B：用于验证「兜底只碰待付款」
    ob = create_order(MARK + "-paid")
    oid_b = ob["id"] if ob else None
    call("POST", f"/api/orders/{oid_b}/pay", mtoken)
    call("POST", f"/api/orders/{oid_b}/pay/confirm", mtoken)
    check("创建并支付订单 B", order_status(oid_b, mtoken) == "已支付",
          f"status={order_status(oid_b, mtoken)}")

    print(f"  等一个扫描周期（{SCAN_CYCLE}s）确认新鲜订单没被误关 ...")
    time.sleep(SCAN_CYCLE)
    check("  A 未超时，扫描后仍是待付款", order_status(oid_a, mtoken) == "待付款",
          f"status={order_status(oid_a, mtoken)}")
    check("  B 未超时，扫描后仍是已支付", order_status(oid_b, mtoken) == "已支付",
          f"status={order_status(oid_b, mtoken)}")

    # ---------------------------------------------------------------
    print()
    print("=" * 78)
    print(f" 第 3 部分：把 A、B 的创建时间回拨 {BACKDATE_MIN} 分钟，模拟超时且 MQ 消息丢失")
    print("=" * 78)

    check("回拨 A 的 created_time", backdate(oid_a))
    check("回拨 B 的 created_time", backdate(oid_b))

    qty_before = good_qty()
    print(f"  等待兜底扫描（最多 {SCAN_CYCLE * 2}s）...")
    closed = wait_for(lambda: order_status(oid_a, mtoken), "已取消")

    check("A（超时待付款）被兜底关闭为已取消", closed, f"status={order_status(oid_a, mtoken)}")
    check("B（超时但已支付）没有被误关", order_status(oid_b, mtoken) == "已支付",
          f"status={order_status(oid_b, mtoken)}")

    qty_after = good_qty()
    check("A 的库存已回补（+1）", qty_after == qty_before + 1,
          f"{qty_before} → {qty_after}")

    # ---------------------------------------------------------------
    print()
    print("=" * 78)
    print(" 第 4 部分：幂等 —— 再扫一轮不重复关单、不重复回补")
    print("=" * 78)

    status_1 = order_status(oid_a, mtoken)
    qty_1 = good_qty()
    print(f"  再等一个扫描周期（{SCAN_CYCLE}s）...")
    time.sleep(SCAN_CYCLE)

    check("A 仍是已取消（没有被二次处理）", order_status(oid_a, mtoken) == status_1,
          f"{status_1} → {order_status(oid_a, mtoken)}")
    check("库存没有再次回补", good_qty() == qty_1, f"{qty_1} → {good_qty()}")

    # ---------------------------------------------------------------
    print()
    print("=" * 78)
    print(" 第 5 部分：清理")
    print("=" * 78)

    sql(f"DELETE FROM order_item WHERE order_id IN ({oid_a},{oid_b})")
    sql(f"DELETE FROM {T_ORDER} WHERE id IN ({oid_a},{oid_b})")
    sql(f"UPDATE good SET qty={qty0} WHERE id={GOOD_ID}")

    check("测试订单已删除", order_status(oid_a, mtoken) == "__GONE__")
    check("商品库存已还原", good_qty() == qty0, f"qty={good_qty()} 期望 {qty0}")

    print()
    print(" 注：下单时投出的 MQ 延时消息仍在 order.delay.queue 里（TTL 30 分钟），")
    print("     约 30 分钟后到点触发，此时订单已被删 → 消费端会打一行")
    print("     「超时关单失败：订单不存在」的 ERROR 日志。属预期，无副作用。")
    print("     （想立刻清掉：rabbitmqctl purge_queue order.delay.queue）")

    return summary()


if __name__ == "__main__":
    sys.exit(0 if main() else 1)
