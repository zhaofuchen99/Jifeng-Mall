#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
订单退款（模拟）验证脚本。

走 vite dev server 的代理（与浏览器同一条路径）打到网关。

覆盖：
  1) 权限：游客 401 / 会员 403 / 零授权管理员 403 / 管理员放行
  2) 状态机：待付款不能退、已支付可发起、重复发起被拒、退款中才能确认、确认后不可重复
  3) 退款状态：无退款 → 退款中 → 已退款
  4) 订单状态：退款确认后转「已取消」
  5) 库存回补：普通订单回补 good.qty；秒杀订单回补 seckill_good.stock/sold

会写测试数据，跑完由 tools/verify/sql/reset_to_baseline.bat 清理。
"""
import os
import json
import sys
import time
import urllib.error
import urllib.request

# ---- 被测地址：可用环境变量覆盖 --------------------------------------
# 默认打 vite dev server(5173) 的代理，与浏览器同路径。
# 部署形态验证时改成 Nginx 的地址即可，脚本无需改动：
#   JIFENG_BASE=http://172.22.96.1:8090 python3 fe_verify.py
# 注意 WSL 里要用 Windows 宿主 IP（172.22.96.1），localhost 走不通。
def _env(name, default):
    return os.environ.get(name, default)

BASE = _env("JIFENG_BASE", "http://172.22.96.1:5173")   # vite 代理 / Nginx，同路径

MARK = "REFUND_TEST"

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


def check(name, cond, detail=""):
    results.append((name, bool(cond)))
    print(f"  [{'PASS' if cond else 'FAIL'}] {name}" + (f"   {detail}" if detail else ""))
    return bool(cond)


def ok(j):
    return isinstance(j, dict) and j.get("success") is True


def good_qty(good_id):
    st, j = call("GET", f"/api/goods/id/{good_id}")
    return (j.get("data") or {}).get("qty") if ok(j) else None


def seckill_stock(sg_id=1):
    st, j = call("GET", f"/api/seckill-goods/seckill/1")
    rows = j.get("data") or [] if ok(j) else []
    for r in rows:
        if r.get("id") == sg_id:
            return r.get("stock"), r.get("sold")
    return None, None


def order_of(oid, token):
    st, j = call("GET", f"/api/orders/id/{oid}", token)
    return j.get("data") or {} if ok(j) else {}


def main():
    print("=" * 78)
    print(" 第 1 部分：权限")
    print("=" * 78)

    st, j = call("POST", "/api/members/login", body={"account": "member", "password": "123456"})
    if not check("会员登录", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}"):
        return summary()
    mtoken = j["data"]["token"]

    st, j = call("POST", "/api/users/login", body={"account": "admin", "password": "123456"})
    if not check("管理员登录", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}"):
        return summary()
    atoken = j["data"]["token"]

    st, j = call("POST", "/api/users/login", body={"account": "operator", "password": "123456"})
    otoken = j["data"]["token"] if ok(j) else None
    check("零授权账号 operator 登录", otoken is not None, f"HTTP {st}")

    st, j = call("PUT", "/api/orders/1/refund")
    check("游客发起退款应 401", st == 401, f"HTTP {st}")

    st, j = call("PUT", "/api/orders/1/refund", mtoken)
    check("会员发起退款应被拒 403", st == 403, f"HTTP {st}")

    st, j = call("PUT", "/api/orders/1/refund/confirm", mtoken)
    check("会员确认退款应被拒 403", st == 403, f"HTTP {st}")

    if otoken:
        st, j = call("PUT", "/api/orders/1/refund", otoken)
        check("零授权管理员发起退款应被拒 403", st == 403, f"HTTP {st}")

    # 取一个属于 member 的收货地址
    st, j = call("GET", "/api/member-addresses/account/member", mtoken)
    addrs = j.get("data") or []
    if not check("取到 member 的收货地址", bool(addrs), f"n={len(addrs)}"):
        return summary()
    addr_id = addrs[0]["id"]

    print()
    print("=" * 78)
    print(" 第 2 部分：普通订单 已支付 → 退款")
    print("=" * 78)

    GOOD_ID = 2
    qty_before = good_qty(GOOD_ID)

    st, j = call("POST", "/api/orders/create", mtoken, {
        "goodId": GOOD_ID, "qty": 1, "addrId": addr_id,
        "comment": MARK + "-refund-paid", "memberAccount": "member"})
    if not check("下单（立即购买）", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}"):
        return summary()
    oid = j["data"]["id"]

    st, j = call("POST", f"/api/orders/{oid}/pay/confirm", mtoken)
    check("支付确认 → 已支付", st == 200 and ok(j), f"HTTP {st}")
    o = order_of(oid, mtoken)
    check("  支付后 refundStatus=无退款", o.get("refundStatus") == "无退款",
          f"refundStatus={o.get('refundStatus')} status={o.get('status')}")

    qty_after_order = good_qty(GOOD_ID)
    check("  下单已扣减库存", qty_after_order == (qty_before or 0) - 1,
          f"qty {qty_before}→{qty_after_order}")

    st, j = call("PUT", f"/api/orders/{oid}/refund", atoken)
    check("管理员发起退款", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}")
    o = order_of(oid, atoken)
    check("  refundStatus=退款中，订单状态未变",
          o.get("refundStatus") == "退款中" and o.get("status") == "已支付",
          f"refundStatus={o.get('refundStatus')} status={o.get('status')}")
    check("  退款人已记入 updatedBy（需求 7.5-3 审计）", o.get("updatedBy") == "admin",
          f"updatedBy={o.get('updatedBy')}")

    st, j = call("PUT", f"/api/orders/{oid}/refund", atoken)
    check("  重复发起退款应被拒(7005)", not ok(j) and j.get("code") == 7005,
          f"code={j.get('code')} msg={j.get('msg')}")

    qty_mid = good_qty(GOOD_ID)
    check("  退款中尚未回补库存", qty_mid == qty_after_order, f"qty={qty_mid}")

    # 退款在途时订单状态仍是「已支付」，不加判断就会把正在退款的订单发出去
    st, j = call("PUT", f"/api/orders/{oid}/ship", atoken)
    check("  退款中的订单不能再发货(7005)", not ok(j) and j.get("code") == 7005,
          f"code={j.get('code')} msg={j.get('msg')}")

    st, j = call("PUT", f"/api/orders/{oid}/refund/confirm", atoken)
    check("管理员确认退款", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}")
    o = order_of(oid, atoken)
    check("  refundStatus=已退款且订单转已取消",
          o.get("refundStatus") == "已退款" and o.get("status") == "已取消",
          f"refundStatus={o.get('refundStatus')} status={o.get('status')}")

    qty_final = good_qty(GOOD_ID)
    check("  退款后已回补库存 good.qty", qty_final == qty_before,
          f"qty {qty_after_order}→{qty_final}（期望回到 {qty_before}）")

    st, j = call("PUT", f"/api/orders/{oid}/refund/confirm", atoken)
    check("  重复确认退款应被拒(7005)", not ok(j) and j.get("code") == 7005,
          f"code={j.get('code')} msg={j.get('msg')}")

    st, j = call("PUT", f"/api/orders/{oid}/refund", atoken)
    check("  已退款订单不能再发起退款(7005)", not ok(j) and j.get("code") == 7005,
          f"code={j.get('code')} msg={j.get('msg')}")

    st, j = call("PUT", f"/api/orders/{oid}/ship", atoken)
    check("  已退款订单不能再发货(7005)", not ok(j) and j.get("code") == 7005,
          f"code={j.get('code')} msg={j.get('msg')}")

    print()
    print("=" * 78)
    print(" 第 3 部分：普通订单 待收货 → 退款")
    print("=" * 78)

    qty_before2 = good_qty(GOOD_ID)
    st, j = call("POST", "/api/orders/create", mtoken, {
        "goodId": GOOD_ID, "qty": 2, "addrId": addr_id,
        "comment": MARK + "-refund-shipped", "memberAccount": "member"})
    oid2 = j["data"]["id"] if ok(j) else None
    if check("下单（数量 2）", oid2 is not None, f"HTTP {st} {j.get('msg')}"):
        call("POST", f"/api/orders/{oid2}/pay/confirm", mtoken)
        st, j = call("PUT", f"/api/orders/{oid2}/ship", atoken)
        o = order_of(oid2, atoken)
        check("发货 → 待收货", st == 200 and o.get("status") == "待收货",
              f"HTTP {st} status={o.get('status')}")

        st, j = call("PUT", f"/api/orders/{oid2}/refund", atoken)
        check("待收货订单可发起退款", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}")

        # 退款在途的订单也不能再确认收货，否则「已确认」会被退款翻成「已取消」
        st, j = call("PUT", f"/api/orders/{oid2}/confirm", mtoken)
        check("  退款中的订单不能再确认收货(7005)", not ok(j) and j.get("code") == 7005,
              f"code={j.get('code')} msg={j.get('msg')}")

        st, j = call("PUT", f"/api/orders/{oid2}/refund/confirm", atoken)
        check("确认退款", st == 200 and ok(j), f"HTTP {st}")
        o = order_of(oid2, atoken)
        check("  已退款/已取消", o.get("refundStatus") == "已退款" and o.get("status") == "已取消",
              f"refundStatus={o.get('refundStatus')} status={o.get('status')}")

        qty_final2 = good_qty(GOOD_ID)
        check("  回补 2 件库存", qty_final2 == qty_before2,
              f"qty {qty_before2}→{qty_final2}")

    print()
    print("=" * 78)
    print(" 第 4 部分：待付款订单不能退款")
    print("=" * 78)

    st, j = call("POST", "/api/orders/create", mtoken, {
        "goodId": GOOD_ID, "qty": 1, "addrId": addr_id,
        "comment": MARK + "-pending", "memberAccount": "member"})
    oid3 = j["data"]["id"] if ok(j) else None
    if check("下单（待付款）", oid3 is not None, f"HTTP {st}"):
        st, j = call("PUT", f"/api/orders/{oid3}/refund", atoken)
        check("待付款订单发起退款应被拒(7005)", not ok(j) and j.get("code") == 7005,
              f"code={j.get('code')} msg={j.get('msg')}")

        st, j = call("PUT", f"/api/orders/{oid3}/refund/confirm", atoken)
        check("未发起退款就确认应被拒(7005)", not ok(j) and j.get("code") == 7005,
              f"code={j.get('code')} msg={j.get('msg')}")

        # 取消掉，顺带清理库存
        st, j = call("PUT", f"/api/orders/{oid3}/cancel", mtoken)
        check("  清理：取消该待付款订单", st == 200 and ok(j), f"HTTP {st}")

    print()
    print("=" * 78)
    print(" 第 5 部分：秒杀订单退款（回补秒杀库存）")
    print("=" * 78)

    sg_stock0, sg_sold0 = seckill_stock(1)
    st, j = call("POST", "/api/seckills/grab", mtoken, {"seckillGoodId": 1})
    sno = (j.get("data") or {}).get("orderNo") if ok(j) else None
    if check("抢购提交", st == 200 and ok(j) and sno, f"HTTP {st} msg={j.get('msg')}"):
        sorder = None
        for _ in range(8):
            time.sleep(1.5)
            st2, j2 = call("GET", f"/api/orders/seckill-no/{sno}", mtoken)
            if ok(j2) and j2.get("data"):
                sorder = j2["data"]
                break
        if check("异步下单已生成订单", sorder is not None,
                 f"orderId={sorder.get('id') if sorder else None}"):
            sid = sorder["id"]
            call("POST", f"/api/orders/{sid}/pay/confirm", mtoken)
            o = order_of(sid, mtoken)
            check("秒杀单支付 → 已支付", o.get("status") == "已支付", f"status={o.get('status')}")

            st, j = call("PUT", f"/api/orders/{sid}/refund", atoken)
            check("秒杀单发起退款", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}")

            st, j = call("PUT", f"/api/orders/{sid}/refund/confirm", atoken)
            check("秒杀单确认退款", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}")
            o = order_of(sid, atoken)
            check("  已退款/已取消", o.get("refundStatus") == "已退款" and o.get("status") == "已取消",
                  f"refundStatus={o.get('refundStatus')} status={o.get('status')}")

            time.sleep(1)
            sg_stock1, sg_sold1 = seckill_stock(1)
            check("  秒杀库存已回补",
                  sg_stock1 == sg_stock0 and sg_sold1 == sg_sold0,
                  f"stock {sg_stock0}→{sg_stock1} sold {sg_sold0}→{sg_sold1}")

            # 回补后应能再次抢购（抢购名额被释放）
            st, j = call("POST", "/api/seckills/grab", mtoken, {"seckillGoodId": 1})
            again = (j.get("data") or {}).get("orderNo") if ok(j) else None
            check("  退款后抢购名额已释放（可再次抢购）", again is not None,
                  f"code={j.get('code')} msg={j.get('msg')}")
            if again:
                for _ in range(8):
                    time.sleep(1.5)
                    st2, j2 = call("GET", f"/api/orders/seckill-no/{again}", mtoken)
                    if ok(j2) and j2.get("data"):
                        call("PUT", f"/api/orders/{j2['data']['id']}/cancel", mtoken)
                        break

    summary()


def summary():
    print()
    print("=" * 78)
    passed = sum(1 for _, c in results if c)
    failed = [n for n, c in results if not c]
    print(f" 合计 {len(results)} 项：通过 {passed}，失败 {len(results) - passed}")
    if failed:
        print(" 失败项：")
        for n in failed:
            print("   - " + n)
    print("=" * 78)


if __name__ == "__main__":
    main()
