#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
订单/会员归属校验验证（需求 7.2-3「会员只能操作本人订单」）。

思路：用两个会员账号互相越界访问。
  - member（id=1，种子数据，有历史订单 1~4）
  - fe_own_<ts>（本轮注册的临时会员，自己没有任何订单）

临时会员去读 member 的订单 / 明细 / 资料，应当全部 403；
读自己的东西应当正常；admin 不受限；服务间调用（无 X-Audience 头）不受限。

除了"冒名下单"那一条，其余全是只读。
"""
import os
import json
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

ORDER_API = _env("JIFENG_ORDER_API", "http://172.22.96.1:10017")   # 直连 order-api，模拟服务间调用
ACC = "fe_own_%d" % int(time.time())
PWD = "test123456"
# member 的历史订单（种子/往期联调留下的，归属 member）
OTHER_ORDER_ID = 3

results = []


def call(method, path, token=None, body=None, headers=None):
    req = urllib.request.Request(
        BASE + path,
        data=json.dumps(body).encode() if body is not None else None,
        method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
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


def denied(j):
    return isinstance(j, dict) and j.get("code") == 403


def main():
    print("=" * 78)
    print(" 第 0 部分：准备账号")
    print("=" * 78)

    st, jm = call("POST", "/api/members/login", body={"account": "member", "password": "123456"})
    st, ju = call("POST", "/api/users/login", body={"account": "admin", "password": "123456"})
    st, jr = call("POST", "/api/members/register",
                  body={"account": ACC, "password": PWD, "name": "越权测试", "phone": "13600001111"})
    if not check("member / admin / 临时会员 都就绪", ok(jm) and ok(ju) and ok(jr)):
        return summary()

    owner_tok = jm["data"]["token"]        # 数据的主人
    admin_tok = ju["data"]["token"]
    other_tok = jr["data"]["token"]        # 越界者
    other_acc = jr["data"]["account"]
    other_id = jr["data"]["userId"]
    print(f"   主人 member(id=1) / 越界者 {other_acc}(id={other_id})")

    print()
    print("=" * 78)
    print(" 第 1 部分：越界者读『别人』的订单 —— 必须 403")
    print("=" * 78)
    cross = [
        ("GET", f"/api/orders/id/{OTHER_ORDER_ID}", "按 id 读他人订单"),
        ("GET", f"/api/order-items/order/{OTHER_ORDER_ID}", "读他人订单明细"),
        ("GET", "/api/orders/member-account/member", "按账号查他人订单列表"),
        ("GET", "/api/members/id/1", "读他人会员资料(按id)"),
        ("GET", "/api/members/account/member", "读他人会员资料(按账号)"),
        ("PUT", f"/api/orders/{OTHER_ORDER_ID}/cancel", "取消他人订单"),
        ("PUT", f"/api/orders/{OTHER_ORDER_ID}/confirm", "确认他人订单收货"),
        ("POST", f"/api/orders/{OTHER_ORDER_ID}/pay", "支付他人订单"),
        ("POST", f"/api/orders/{OTHER_ORDER_ID}/pay/confirm", "确认支付他人订单"),
    ]
    leaked = []
    for m, p, name in cross:
        st, j = call(m, p, other_tok)
        if denied(j):
            flag = "🔒 403"
        elif ok(j):
            flag = "❌ 放行了"
            leaked.append(name)
        else:
            flag = f"拒绝(code={j.get('code')})"
        print(f"  {flag:<22} {name}")
    check("越界访问他人订单/资料全部被拒", not leaked,
          "" if not leaked else f"泄漏: {leaked}")

    print()
    print("=" * 78)
    print(" 第 2 部分：读『自己』的东西 —— 必须正常")
    print("=" * 78)
    mine = [
        ("GET", f"/api/members/id/{other_id}", "自己的资料(按id)"),
        ("GET", f"/api/members/account/{other_acc}", "自己的资料(按账号)"),
        ("GET", f"/api/orders/member-account/{other_acc}", "自己的订单列表"),
        ("GET", "/api/carts/member/%d" % other_id, "自己的购物车"),
    ]
    broken = []
    for m, p, name in mine:
        st, j = call(m, p, other_tok)
        good = not denied(j) and st != 401
        print(f"  {'✅ 可访问' if good else '❌ 被拦'}   HTTP {st}  {name}")
        if not good:
            broken.append(name)
    check("会员访问自己的数据正常", not broken, "" if not broken else f"误拦: {broken}")

    st, j = call("GET", f"/api/orders/id/{OTHER_ORDER_ID}", owner_tok)
    check("主人读自己的订单正常", ok(j), f"HTTP {st}")

    print()
    print("=" * 78)
    print(" 第 3 部分：admin 不受限（后台要看任意订单/会员）")
    print("=" * 78)
    for m, p, name in [
        ("GET", f"/api/orders/id/{OTHER_ORDER_ID}", "admin 读任意订单"),
        ("GET", f"/api/order-items/order/{OTHER_ORDER_ID}", "admin 读任意订单明细"),
        ("GET", "/api/members/id/1", "admin 读任意会员资料"),
        ("GET", "/api/orders/member-account/member", "admin 按账号查订单"),
    ]:
        st, j = call(m, p, admin_tok)
        check(name, ok(j), f"HTTP {st}")

    print()
    print("=" * 78)
    print(" 第 4 部分：服务间调用（无 X-Audience 头）不受限")
    print("=" * 78)
    print("  （seckill-api 用 Feign 调 /api/orders/seckill-no/{sno} 做幂等判断，不能误伤）")
    print("  注意必须**绕过网关**直连 order-api 端口：经网关的话没有效 JWT 会先被 401 拦掉")

    def call_direct(path, headers=None):
        req = urllib.request.Request(ORDER_API + path)
        for k, v in (headers or {}).items():
            req.add_header(k, v)
        try:
            with urllib.request.urlopen(req, timeout=20) as r:
                return r.status, json.loads(r.read().decode("utf-8"))
        except urllib.error.HTTPError as e:
            try:
                return e.code, json.loads(e.read().decode("utf-8"))
            except Exception:
                return e.code, {}
        except Exception as e:
            return 0, {"_err": str(e)}

    # 直连、且刻意带上"别人的" X-User-Name，模拟内部调用的最坏情况
    st, j = call_direct("/api/orders/id/%d" % OTHER_ORDER_ID,
                        {"X-User-Id": "999", "X-User-Name": "someone-else"})
    check("直连 order-api 且无 X-Audience（模拟服务间调用）→ 不拦",
          st == 200 and ok(j), f"HTTP {st} code={j.get('code')}")

    st, j = call_direct("/api/orders/id/%d" % OTHER_ORDER_ID, {"X-Audience": "member", "X-User-Name": "someone-else"})
    check("直连但带了 X-Audience=member 且非本人 → 仍要拦",
          st == 200 and j.get("code") == 403, f"HTTP {st} code={j.get('code')}")

    print()
    print("=" * 78)
    print(" 第 5 部分：防冒名下单 —— 请求体里的 memberAccount 应被令牌覆盖")
    print("=" * 78)

    # 先给越界者建一个自己的地址（下面要用它才能正常下单）
    st, ja = call("POST", "/api/member-addresses", other_tok,
                  {"memberAccount": other_acc, "receiver": "越界测试", "phone": "13600001111",
                   "addrId": 440106, "addrDetail": "FE_OWN-street", "isDefault": True})
    if not check("给越界者建自己的收货地址", ok(ja), f"HTTP {st} {ja.get('msg')}"):
        return summary()
    other_addr = ja["data"]["id"]

    # 5.1 用**别人的地址**下单 → 必须被拒（地址归属校验）
    st, j = call("POST", "/api/orders/create", other_tok,
                 {"goodId": 2, "qty": 1, "addrId": 1, "comment": "FE_OWN_TEST-badaddr"})
    check("用他人地址下单 → 被拒(403)", j.get("code") == 403,
          f"HTTP {st} code={j.get('code')} msg={j.get('msg')}")

    # 5.2 用自己的地址、但故意传 member 的账号 → 订单应挂在自己名下
    st, j = call("POST", "/api/orders/create", other_tok,
                 {"goodId": 2, "qty": 1, "addrId": other_addr,
                  "comment": "FE_OWN_TEST-impersonate", "memberAccount": "member"})
    if check("用他人账号下单：请求被接受", ok(j), f"HTTP {st} {j.get('msg')}"):
        created = j["data"]
        check("  订单归属被强制改为调用者本人（未被冒名）",
              created.get("memberAccount") == other_acc,
              f"memberAccount={created.get('memberAccount')}，期望 {other_acc}")
        # 收尾：取消订单，回补库存
        st2, j2 = call("PUT", f"/api/orders/{created['id']}/cancel", other_tok)
        check("  取消该测试订单（回补库存）", ok(j2), f"HTTP {st2}")
        print(f"   测试订单 id={created['id']}，由 reset_to_baseline.bat 清理")

    print()
    print("=" * 78)
    print(" 第 6 部分：购物车归属 —— 会员只能操作自己的购物车")
    print("=" * 78)

    # 先用主人的令牌往他自己购物车里放一件，拿到条目 id
    st, j = call("POST", "/api/carts/save-merge", owner_tok, {"memberId": 1, "goodId": 2, "qty": 1})
    if not check("主人往自己购物车加购成功", ok(j), f"HTTP {st} {j.get('msg')}"):
        return summary()
    owner_cart_id = j["data"]["id"]
    print(f"   主人购物车条目 id={owner_cart_id}")

    cart_cross = [
        ("GET", f"/api/carts/id/{owner_cart_id}", None, "读他人购物车条目"),
        ("GET", "/api/carts/member/1", None, "读他人购物车列表"),
        ("PUT", "/api/carts", {"id": owner_cart_id, "qty": 99}, "改他人购物车数量"),
        ("DELETE", "/api/carts", [owner_cart_id], "删他人购物车条目"),
        ("DELETE", "/api/carts/clear", [owner_cart_id], "清他人购物车条目"),
        ("DELETE", "/api/carts/remove-by-goods?memberId=1&goodIds=2", None, "按商品清他人购物车"),
    ]
    cart_leak = []
    for m, p, body, name in cart_cross:
        st, j = call(m, p, other_tok, body)
        if j.get("code") == 403:
            flag = "🔒 403"
        elif ok(j):
            flag = "❌ 放行了"
            cart_leak.append(name)
        else:
            flag = f"拒绝(code={j.get('code')})"
        print(f"  {flag:<22} {name}")
    check("越界操作他人购物车全部被拒", not cart_leak,
          "" if not cart_leak else f"泄漏: {cart_leak}")

    # 「加购」这一条不一样：它不该被拒，而该把归属强制成调用者本人。
    # 请求体里填别人的 memberId 也没用——商品只会进到自己的购物车。
    st, j = call("POST", "/api/carts/save-merge", other_tok,
                 {"memberId": 1, "goodId": 1, "qty": 1})
    if check("用他人 memberId 加购：请求被接受（强制归属本人）", ok(j), f"HTTP {st} {j.get('msg')}"):
        check("  条目归属被强制为调用者，没塞进他人购物车",
              j["data"].get("memberId") == other_id,
              f"memberId={j['data'].get('memberId')}，期望 {other_id}")
        # 收尾：删掉这条
        call("DELETE", "/api/carts", other_tok, [j["data"]["id"]])

    # 主人自己仍能正常操作
    st, j = call("GET", f"/api/carts/id/{owner_cart_id}", owner_tok)
    check("主人读自己的购物车条目正常", ok(j), f"HTTP {st}")
    st, j = call("GET", "/api/carts/member/1", owner_tok)
    check("主人读自己的购物车列表正常", ok(j), f"HTTP {st}")

    # 越界者用他人 cartId 下单 → 必须被拒
    st, j = call("POST", "/api/orders/create", other_tok,
                 {"cartIds": [owner_cart_id], "addrId": other_addr, "comment": "FE_OWN_TEST-badcart"})
    check("用他人 cartId 下单 → 被拒(403)", j.get("code") == 403,
          f"HTTP {st} code={j.get('code')} msg={j.get('msg')}")

    # 收尾：清掉主人的购物车条目（用他自己的令牌）
    st, j = call("DELETE", "/api/carts", owner_tok, [owner_cart_id])
    check("收尾：主人清掉自己的购物车条目", ok(j), f"HTTP {st}")

    summary()


def summary():
    print()
    print("=" * 78)
    passed = sum(1 for _, c in results if c)
    failed = [n for n, c in results if not c]
    print(f" 合计 {len(results)} 项：通过 {passed}，失败 {len(results) - passed}")
    for n in failed:
        print("   - " + n)
    print("=" * 78)


if __name__ == "__main__":
    main()
