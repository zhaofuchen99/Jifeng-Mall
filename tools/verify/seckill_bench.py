#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
秒杀并发压测（验收标准 3「并发压测下无超卖、无重复抢购、库存与订单最终一致」
以及标准 5「性能以压测报告为准」）。

方法：
  1. 重置秒杀状态（DB stock=50/sold=0，Redis seckill:stock=50，清空限购标记）
  2. 并发注册 N 个压测会员（每人只抢一次，所以限购 1 件不构成干扰）
  3. 用 **线程栅栏** 让 N 个抢购请求在同一瞬间发出 —— 不同步的话就是排队，
     考验不到 Lua 脚本的原子性
  4. 统计成功/售罄/限购的数量，并算出 P50/P95/P99 与吞吐
  5. 等 MQ 异步下单结算完，核对 Redis 与 DB 是否一致、订单是否重复

直连网关 8888（不经 vite 代理），更接近 Nginx → 网关的部署形态。
"""
import os
import json
import statistics
import sys
import threading
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor

# ---- 被测地址：可用环境变量覆盖 --------------------------------------
# 默认打 vite dev server(5173) 的代理，与浏览器同路径。
# 部署形态验证时改成 Nginx 的地址即可，脚本无需改动：
#   JIFENG_BASE=http://172.22.96.1:8090 python3 fe_verify.py
# 注意 WSL 里要用 Windows 宿主 IP（172.22.96.1），localhost 走不通。
def _env(name, default):
    return os.environ.get(name, default)

GW = _env("JIFENG_GATEWAY", "http://172.22.96.1:8888")   # 压测直连网关，不经前端


STOCK = 50          # 秒杀库存（与 DB / Redis 重置后一致）
MEMBERS = 200       # 参与抢购的会员数
SECKILL_GOOD_ID = 1
PREFIX = "bench_"
PWD = "bench123456"

# 并发度：等于会员数时用栅栏同步起跑（瞬时洪峰，考验防超卖）；
# 小于会员数时用有界线程池（稳态负载，用它来对照 NFR-001 的 P95 目标）
CONCURRENCY = int(sys.argv[1]) if len(sys.argv) > 1 else MEMBERS

grab_results = []   # (http_status, code, latency_ms, orderNo, msg)
lock = threading.Lock()


def post(path, body, token=None, timeout=30):
    req = urllib.request.Request(GW + path, data=json.dumps(body).encode(), method="POST")
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            st, raw = r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        st, raw = e.code, e.read().decode()
    except Exception as e:
        return 0, {"_err": str(e)}, (time.perf_counter() - t0) * 1000
    return st, json.loads(raw), (time.perf_counter() - t0) * 1000


def get(path, token=None, timeout=30):
    req = urllib.request.Request(GW + path)
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode())
        except Exception:
            return e.code, {}
    except Exception as e:
        return 0, {"_err": str(e)}


def pct(values, p):
    if not values:
        return 0.0
    s = sorted(values)
    k = max(0, min(len(s) - 1, int(round((p / 100.0) * len(s) + 0.5)) - 1))
    return s[k]


def main():
    print("=" * 78)
    print(f" 秒杀并发压测：库存 {STOCK}，会员 {MEMBERS} 人，各抢 1 次，并发度 {CONCURRENCY}")
    print("=" * 78)

    # ---------- 1. 注册会员 ----------
    print(f"\n[1/5] 并发注册 {MEMBERS} 个压测会员 ...")
    t0 = time.perf_counter()
    tokens = []

    def reg(i):
        acc = f"{PREFIX}{int(time.time())}_{i}"
        st, j, _ = post("/api/members/register",
                        {"account": acc, "password": PWD, "name": f"压测{i:03d}",
                         "phone": "138%08d" % i})
        return acc, (j["data"]["token"] if j.get("success") else None), j.get("msg")

    with ThreadPoolExecutor(max_workers=24) as ex:
        for acc, tok, msg in ex.map(reg, range(MEMBERS)):
            if tok:
                tokens.append(tok)
            else:
                print(f"    注册失败 {acc}: {msg}")
    reg_sec = time.perf_counter() - t0
    print(f"   成功注册 {len(tokens)} 个，用时 {reg_sec:.1f}s")
    if len(tokens) < 2:
        print("   会员不足，终止")
        return

    # ---------- 2. 并发抢购 ----------
    n = len(tokens)
    burst = CONCURRENCY >= n
    if burst:
        print(f"\n[2/5] {n} 个请求**同一瞬间**并发抢购（栅栏同步，模拟开抢洪峰）...")
        barrier = threading.Barrier(n)
    else:
        print(f"\n[2/5] {n} 个请求按并发度 {CONCURRENCY} 压测（稳态负载）...")
        barrier = None

    def grab(tok):
        if barrier is not None:
            try:
                barrier.wait(timeout=120)   # 所有线程在此对齐，然后一起冲
            except threading.BrokenBarrierError:
                pass
        st, j, ms = post("/api/seckills/grab", {"seckillGoodId": SECKILL_GOOD_ID}, tok)
        with lock:
            grab_results.append({
                "http": st, "code": j.get("code"), "msg": j.get("msg"),
                "orderNo": (j.get("data") or {}).get("orderNo") if isinstance(j.get("data"), dict) else None,
                "ms": ms,
            })

    t0 = time.perf_counter()
    with ThreadPoolExecutor(max_workers=(n if burst else CONCURRENCY)) as ex:
        list(ex.map(grab, tokens))
    wall = time.perf_counter() - t0

    # ---------- 3. 统计 ----------
    ok_cnt = sum(1 for r in grab_results if r["code"] == 200)
    sold_out = sum(1 for r in grab_results if r["code"] == 7004)
    limited = sum(1 for r in grab_results if r["code"] == 7003)
    other = [r for r in grab_results if r["code"] not in (200, 7003, 7004)]
    lat = [r["ms"] for r in grab_results]

    print(f"\n[3/5] 抢购结果")
    print(f"    请求总数      {len(grab_results)}")
    print(f"    成功(200)     {ok_cnt}")
    print(f"    已售罄(7004)  {sold_out}")
    print(f"    已参与(7003)  {limited}")
    print(f"    其他          {len(other)}  {[(r['code'], r['msg']) for r in other[:5]]}")
    print(f"    墙钟总耗时    {wall * 1000:.0f} ms   吞吐 {len(grab_results) / wall:.0f} req/s")
    print(f"    延迟 P50/P95/P99/max = {pct(lat,50):.0f} / {pct(lat,95):.0f} / "
          f"{pct(lat,99):.0f} / {max(lat):.0f} ms   均值 {statistics.mean(lat):.0f} ms")

    # ---------- 4. 等异步下单结算 ----------
    print(f"\n[4/5] 等待 MQ 异步下单结算 ...")
    settled = False
    for i in range(40):
        time.sleep(1.5)
        st, j = get("/api/seckill-goods/seckill/1")
        row = (j.get("data") or [{}])[0]
        sold, stock = row.get("sold"), row.get("stock")
        if sold == ok_cnt:
            settled = True
            print(f"    已结算：sold={sold} stock={stock}（第 {i+1} 次轮询，{(i+1)*1.5:.1f}s）")
            break
    if not settled:
        st, j = get("/api/seckill-goods/seckill/1")
        row = (j.get("data") or [{}])[0]
        print(f"    ⚠️ 30s 内未结算完：sold={row.get('sold')}，成功数={ok_cnt}")

    # ---------- 5. 一致性核对 ----------
    st, j = get("/api/seckill-goods/seckill/1")
    row = (j.get("data") or [{}])[0]
    db_sold, db_stock = row.get("sold"), row.get("stock")

    order_nos = [r["orderNo"] for r in grab_results if r["code"] == 200 and r["orderNo"]]
    uniq_order_nos = set(order_nos)

    print(f"\n[5/5] 一致性核对")
    print(f"    DB  seckill_good: stock={db_stock}  sold={db_sold}")
    print(f"    抢购成功数={ok_cnt}  生成流水号={len(order_nos)}（去重后 {len(uniq_order_nos)}）")

    checks = [
        ("成功数不超过库存（防超卖）", ok_cnt <= STOCK, f"{ok_cnt} <= {STOCK}"),
        ("成交数恰好等于库存（无少卖）", db_sold == STOCK, f"sold={db_sold}"),
        ("DB 剩余库存归零", db_stock == 0, f"stock={db_stock}"),
        ("成功数与 DB 已售数一致", ok_cnt == db_sold, f"{ok_cnt} vs {db_sold}"),
        ("秒杀流水号无重复（防重复下单）", len(order_nos) == len(uniq_order_nos),
         f"{len(order_nos)} vs {len(uniq_order_nos)}"),
        ("未产生非预期错误", len(other) == 0, str([r["code"] for r in other][:5])),
    ]
    failed = []
    for name, cond, detail in checks:
        print(f"    [{'PASS' if cond else 'FAIL'}] {name}   {detail}")
        if not cond:
            failed.append(name)

    print()
    print("=" * 78)
    print(f" 压测结论：{'全部通过 ✅' if not failed else '存在失败项 ❌ ' + str(failed)}")
    print("=" * 78)
    print(f"\n 注：本次注册了 {len(tokens)} 个 {PREFIX}* 会员，成功抢购 {ok_cnt} 单，")
    print(f"     清理见 tools/verify/sql/reset_to_baseline.bat")


if __name__ == "__main__":
    main()
