#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
mall-web 后端契约验证脚本。

走 vite dev server 的代理（和浏览器完全同一条路径）打到网关，逐条验证前端页面依赖的接口，
断言返回结构是否与前端代码的假设一致。会写入测试数据（订单备注统一带 FE_TEST 便于清理）。

用法:
    python3 fe_verify.py            # 跑全部
    python3 fe_verify.py --no-write # 只跑只读部分
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

MARK = "FE_TEST"

results = []


def call(method, path, token=None, body=None, timeout=20):
    url = BASE + path
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            raw = r.read().decode("utf-8")
            status = r.status
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8")
        status = e.code
    except Exception as e:
        return 0, {"_err": str(e)}
    try:
        return status, json.loads(raw)
    except Exception:
        return status, {"_raw": raw[:300]}


def check(name, cond, detail=""):
    results.append((name, bool(cond), detail))
    print(f"  [{'PASS' if cond else 'FAIL'}] {name}" + (f"   {detail}" if detail else ""))
    return bool(cond)


def ok(j):
    """统一响应是否业务成功"""
    return isinstance(j, dict) and j.get("success") is True


def main():
    write = "--no-write" not in sys.argv

    print("=" * 78)
    print(" 第 1 部分：游客（无令牌）")
    print("=" * 78)

    st, j = call("GET", "/api/goods?pageNo=1&pageSize=5")
    check("GET /api/goods 公开可访问", st == 200 and ok(j), f"HTTP {st}")
    check("  返回 PageInfo 结构(list/total/pageNum)",
          ok(j) and {"list", "total", "pageNum"} <= set(j["data"] or {}),
          f"keys={sorted((j.get('data') or {}).keys())[:6]}")

    st, j = call("GET", "/api/categories/tree")
    check("GET /api/categories/tree 分类树", st == 200 and ok(j),
          f"HTTP {st} msg={j.get('msg')}")
    if ok(j):
        check("  一级分类 parentId=0 被正确当作根", len(j["data"]) > 0,
              f"roots={[c['name'] for c in j['data']]}")

    st, j = call("GET", "/api/brands?pageNo=1&pageSize=10")
    check("GET /api/brands", st == 200 and ok(j), f"HTTP {st}")

    st, j = call("GET", "/api/seckills/active")
    check("GET /api/seckills/active 有进行中活动",
          st == 200 and ok(j) and len(j["data"] or []) > 0, f"HTTP {st}")

    st, j = call("GET", "/api/seckill-goods/seckill/1")
    check("GET /api/seckill-goods/seckill/{id} 游客可读(白名单修复)",
          st == 200 and ok(j), f"HTTP {st}")

    st, j = call("GET", "/api/seckill-goods?goodId=1&pageNo=1&pageSize=20")
    check("GET /api/seckill-goods?goodId= 详情页反查", st == 200 and ok(j), f"HTTP {st}")

    st, j = call("GET", "/api/regions/parent-id/0")
    check("GET /api/regions/parent-id/0 级联省份", st == 200 and ok(j) and len(j["data"] or []) > 0,
          f"HTTP {st}")

    st, j = call("GET", "/api/goods/id/1?full=true")
    check("GET /api/goods/id/{id}?full=true 含品牌+分类",
          st == 200 and ok(j) and j["data"].get("brand") and j["data"].get("category"),
          f"HTTP {st}")

    st, j = call("GET", "/api/carts/member/1")
    check("GET /api/carts/member/{id} 无令牌应 401", st == 401, f"HTTP {st}")

    print()
    print("=" * 78)
    print(" 第 2 部分：会员登录")
    print("=" * 78)

    st, j = call("POST", "/api/members/login", body={"account": "member", "password": "123456"})
    if not check("POST /api/members/login", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}"):
        return
    token = j["data"]["token"]
    uid = j["data"]["userId"]
    check("  返回 LoginUserInfo(token/userId/account/name/audience)",
          {"token", "userId", "account", "name", "audience"} <= set(j["data"]),
          f"audience={j['data'].get('audience')}")

    st, j = call("GET", f"/api/members/id/{uid}", token)
    check("GET /api/members/id/{id} 个人信息页", st == 200 and ok(j), f"HTTP {st}")

    st, j = call("GET", "/api/member-addresses/account/member", token)
    check("GET /api/member-addresses/account/{account}", st == 200 and ok(j),
          f"HTTP {st} n={len(j.get('data') or [])}")
    if ok(j) and j["data"]:
        a0 = j["data"][0]
        addr_obj = a0.get("address") or {}
        grand = (addr_obj.get("parent") or {}).get("parent")
        check("  地址带省市区嵌套(address.parent.parent)",
              bool(grand),
              f"fullName={addr_obj.get('fullName')}")

    if not write:
        return summary()

    print()
    print("=" * 78)
    print(" 第 3 部分：会员写操作（会落测试数据，备注统一 " + MARK + "）")
    print("=" * 78)

    # --- 地址 CRUD ---
    st, j = call("POST", "/api/member-addresses", token, {
        "memberAccount": "member", "receiver": MARK, "phone": "13800002222",
        "addrId": 440106, "addrDetail": MARK + "-street", "isDefault": False})
    if not check("POST /api/member-addresses 新增地址", st == 200 and ok(j),
                 f"HTTP {st} {j.get('msg')}"):
        return summary()
    addr_id = j["data"]["id"]

    st, j = call("PUT", "/api/member-addresses", token, {
        "id": addr_id, "receiver": MARK, "phone": "13800003333",
        "addrId": 440106, "addrDetail": MARK + "-street-2", "isDefault": False})
    check("PUT /api/member-addresses 编辑地址", st == 200 and ok(j), f"HTTP {st}")

    st, j = call("PUT", f"/api/member-addresses/default/{addr_id}?memberAccount=member", token)
    check("PUT /api/member-addresses/default/{id} 设为默认",
          st == 200 and ok(j) and j["data"].get("isDefault") is True, f"HTTP {st}")

    st, j = call("GET", "/api/member-addresses/account/member", token)
    defaults = [a for a in (j.get("data") or []) if a.get("isDefault")]
    check("  默认地址唯一（只应有 1 条）", len(defaults) == 1, f"默认条数={len(defaults)}")

    # --- 购物车 ---
    st, j = call("POST", "/api/carts/save-merge", token,
                 {"memberId": uid, "goodId": 2, "qty": 2})
    check("POST /api/carts/save-merge 加入购物车", st == 200 and ok(j), f"HTTP {st}")
    cart_id = j["data"]["id"] if ok(j) else None

    st, j = call("POST", "/api/carts/save-merge", token,
                 {"memberId": uid, "goodId": 2, "qty": 1})
    check("  同商品再次加入应累加(2→3)", ok(j) and j["data"]["qty"] == 3,
          f"qty={j.get('data', {}).get('qty')}")

    st, j = call("PUT", "/api/carts", token, {"id": cart_id, "qty": 1})
    check("PUT /api/carts 修改数量", st == 200 and ok(j), f"HTTP {st}")

    st, j = call("GET", f"/api/carts/member/{uid}", token)
    cart_rows = j.get("data") or []
    check("GET /api/carts/member/{id} 含商品对象(good)",
          st == 200 and ok(j) and cart_rows and cart_rows[0].get("good"),
          f"HTTP {st} n={len(cart_rows)}")

    # --- 下单（立即购买路径，不用购物车） ---
    st, j = call("POST", "/api/orders/create", token, {
        "goodId": 2, "qty": 1, "addrId": addr_id,
        "comment": MARK + "-buynow", "memberAccount": "member"})
    if not check("POST /api/orders/create 立即购买下单", st == 200 and ok(j),
                 f"HTTP {st} {j.get('msg')}"):
        return summary()
    order = j["data"]
    oid = order["id"]
    check("  返回 OrderEntity 待付款", order.get("status") == "待付款",
          f"orderNo={order.get('orderNo')} totalPay={order.get('totalPay')}")

    st, j = call("GET", f"/api/orders/id/{oid}", token)
    check("GET /api/orders/id/{id}", st == 200 and ok(j), f"HTTP {st}")

    st, j = call("GET", f"/api/order-items/order/{oid}", token)
    items = j.get("data") or []
    check("GET /api/order-items/order/{orderId} 订单明细",
          st == 200 and ok(j) and len(items) > 0,
          f"n={len(items)} goodName={items[0].get('goodName') if items else None}")

    # --- 支付 ---
    st, j = call("POST", f"/api/orders/{oid}/pay", token)
    check("POST /api/orders/{id}/pay 发起支付", st == 200 and ok(j), f"HTTP {st}")

    st, j = call("POST", f"/api/orders/{oid}/pay/confirm", token)
    check("POST /api/orders/{id}/pay/confirm 模拟支付确认", st == 200 and ok(j), f"HTTP {st}")

    st, j = call("GET", f"/api/orders/id/{oid}", token)
    o = j.get("data") or {}
    check("  支付后状态=已支付 且有支付信息",
          o.get("status") == "已支付" and o.get("payType") and o.get("alipayTradeNo"),
          f"status={o.get('status')} payType={o.get('payType')} tradeNo={o.get('alipayTradeNo')}")

    # --- 发货（后台操作）→ 确认收货（会员操作）---
    #
    # 发货必须用**管理员令牌**：它是后台管理动作，会员不该能给自己发货。
    # 网关的 RBAC 修好之后，用会员令牌调 /ship 会正确返回 403。
    st, ja = call("POST", "/api/users/login", body={"account": "admin", "password": "123456"})
    if not ok(ja):
        print("  !! 管理员登录失败，跳过后台发货步骤")
        return summary()
    admin_token = ja["data"]["token"]

    st, j = call("PUT", f"/api/orders/{oid}/ship", admin_token)
    check("PUT /api/orders/{id}/ship 发货（管理员）", st == 200 and ok(j), f"HTTP {st}")

    st, j = call("PUT", f"/api/orders/{oid}/ship", token)
    check("  会员令牌发货应被拒(403)", st == 403, f"HTTP {st}")

    st, j = call("PUT", f"/api/orders/{oid}/confirm", token)
    check("PUT /api/orders/{id}/confirm 确认收货（会员）", st == 200 and ok(j), f"HTTP {st}")

    st, j = call("GET", f"/api/orders/id/{oid}", token)
    check("  确认后状态=已确认", (j.get("data") or {}).get("status") == "已确认",
          f"status={(j.get('data') or {}).get('status')}")

    # --- 订单状态机：非法流转应被拒 ---
    st, j = call("PUT", f"/api/orders/{oid}/cancel", token)
    check("  已确认订单再取消应被拒(7005)", not ok(j) and j.get("code") == 7005,
          f"code={j.get('code')} msg={j.get('msg')}")

    st, j = call("GET", "/api/orders/member-account/member", token)
    check("GET /api/orders/member-account/{account}", st == 200 and ok(j), f"HTTP {st}")

    # --- 秒杀 ---
    st, j = call("GET", "/api/seckill-goods/seckill/1")
    before = (j["data"] or [{}])[0] if ok(j) else {}

    st, j = call("POST", "/api/seckills/grab", token, {"seckillGoodId": 1})
    check("POST /api/seckills/grab 抢购", st == 200 and ok(j) and (j.get("data") or {}).get("success"),
          f"HTTP {st} msg={j.get('msg')}")
    sno = (j.get("data") or {}).get("orderNo") if ok(j) else None

    if sno:
        seckill_order = None
        for i in range(8):
            time.sleep(1.5)
            st2, j2 = call("GET", f"/api/orders/seckill-no/{sno}", token)
            if ok(j2) and j2.get("data"):
                seckill_order = j2["data"]
                break
        check("  异步下单已生成订单(轮询 seckill-no)", seckill_order is not None,
              f"orderId={seckill_order.get('id') if seckill_order else None} "
              f"status={seckill_order.get('status') if seckill_order else None}")

        st, j = call("GET", "/api/seckill-goods/seckill/1")
        after = (j["data"] or [{}])[0] if ok(j) else {}
        check("  秒杀库存已扣减", (after.get("sold", 0) or 0) > (before.get("sold", 0) or 0),
              f"sold {before.get('sold')}→{after.get('sold')} stock {before.get('stock')}→{after.get('stock')}")

        # 重复抢购应被限购拦截
        st, j = call("POST", "/api/seckills/grab", token, {"seckillGoodId": 1})
        check("  同一活动重复抢购应被拒(7003)", not ok(j) and j.get("code") == 7003,
              f"code={j.get('code')} msg={j.get('msg')}")

        # 取消秒杀订单 → 回补库存（同时完成清理）
        if seckill_order:
            st, j = call("PUT", f"/api/orders/{seckill_order['id']}/cancel", token)
            check("PUT /api/orders/{id}/cancel 取消秒杀订单(触发回补)", st == 200 and ok(j),
                  f"HTTP {st}")

    # 清理：删掉本次测试地址 + 购物车
    st, j = call("DELETE", "/api/member-addresses", token, [addr_id])
    check("DELETE /api/member-addresses 清理测试地址", st == 200 and ok(j), f"HTTP {st}")

    summary()
    print()
    print(f" 注：本次新建订单 id={oid}（已走完 支付→发货→确认收货，无法再用接口删除）")
    print(f"     清理见 tools/verify/sql/reset_to_baseline.bat")


def summary():
    print()
    print("=" * 78)
    passed = sum(1 for _, c, _ in results if c)
    failed = [n for n, c, _ in results if not c]
    print(f" 合计 {len(results)} 项：通过 {passed}，失败 {len(results) - passed}")
    if failed:
        print(" 失败项：")
        for n in failed:
            print("   - " + n)
    print("=" * 78)


if __name__ == "__main__":
    main()
