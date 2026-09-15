#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RBAC 越权拒绝验证。

只发 GET（只读），不产生任何写操作 —— 在权限尚未验证通过时发写请求，
万一过滤器仍然失效就会真的改到数据。

验证三件事：
  1) operator（零授权账号）访问「后台专属」接口应当被拒绝
  2) admin 访问全部后台接口应当正常
  3) 「公开接口」对谁都放行（这是设计如此：前台商城游客也要能浏览）

区分公开与专属的依据是网关的 PUBLIC_PREFIXES + AuthGlobalFilter 白名单。
"""
import os
import json
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


results = []


def call(method, path, token=None, body=None):
    req = urllib.request.Request(
        BASE + path,
        data=json.dumps(body).encode() if body is not None else None,
        method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
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


# 后台专属接口：非 admin 或未授权 admin 都该被拒
ADMIN_ONLY = [
    "/api/roles?pageNo=1&pageSize=2",
    "/api/users?pageNo=1&pageSize=2",
    "/api/perms?pageNo=1&pageSize=2",
    "/api/resources?pageNo=1&pageSize=2",
    "/api/menus?pageNo=1&pageSize=2",
    "/api/groups?pageNo=1&pageSize=2",
    "/api/user-groups?pageNo=1&pageSize=2",
    "/api/group-roles?pageNo=1&pageSize=2",
    "/api/role-perms?pageNo=1&pageSize=2",
    "/api/perm-resources?pageNo=1&pageSize=2",
    "/api/order-items?pageNo=1&pageSize=2",
    "/api/members?pageNo=1&pageSize=2",
    "/api/orders?pageNo=1&pageSize=2",
]

# 公开接口：前台商城游客要能浏览，故意放行（网关 PUBLIC_GET_ONLY / 白名单）。
# 接口文档第 2 节明确写「/api/seckills/**、/api/seckill-goods/** 查询(GET)公开」，
# 而且前台商品详情页要靠 /api/seckill-goods?goodId= 反查秒杀信息（游客可见的页面），
# 返回的是秒杀价/库存这类公开商品数据 —— 所以 GET 公开是设计如此，写操作才需要授权。
PUBLIC = [
    "/api/goods?pageNo=1&pageSize=2",
    "/api/brands?pageNo=1&pageSize=2",
    "/api/categories/tree",
    "/api/regions/parent-id/0",
    "/api/seckills/active",
    "/api/seckill-goods?pageNo=1&pageSize=2",
]


def main():
    print("=" * 78)
    print(" 第 0 部分：登录拿令牌")
    print("=" * 78)

    st, ja = call("POST", "/api/users/login", body={"account": "admin", "password": "123456"})
    st2, jo = call("POST", "/api/users/login", body={"account": "operator", "password": "123456"})
    st3, jm = call("POST", "/api/members/login", body={"account": "member", "password": "123456"})
    if not check("admin / operator / member 三个账号都能登录",
                 ok(ja) and ok(jo) and ok(jm)):
        return summary()
    admin_tok = ja["data"]["token"]
    op_tok = jo["data"]["token"]
    member_tok = jm["data"]["token"]
    print(f"   admin audience={ja['data']['audience']}  "
          f"operator audience={jo['data']['audience']}  "
          f"member audience={jm['data']['audience']}")

    print()
    print("=" * 78)
    print(" 第 1 部分：operator（零授权）访问后台专属接口 —— 必须被拒")
    print("=" * 78)
    leaked = []
    for p in ADMIN_ONLY:
        st, j = call("GET", p, op_tok)
        if j.get("code") == 403:
            mark = "🔒 403"
        elif j.get("code") == 503:
            mark = "⚠️  503（权限服务不可用）"
            leaked.append(p)
        elif j.get("success"):
            mark = "❌ 放行了"
            leaked.append(p)
        else:
            mark = f"拒绝({j.get('code')})"
        print(f"  {mark:<24} {p.split('?')[0]}")
    check("operator 对全部后台专属接口都被拒绝", not leaked,
          "" if not leaked else f"泄漏 {len(leaked)} 个：{leaked}")

    print()
    print("=" * 78)
    print(" 第 2 部分：admin 访问同样的接口 —— 必须正常（种子授权要补全）")
    print("=" * 78)
    blocked = []
    for p in ADMIN_ONLY:
        st, j = call("GET", p, admin_tok)
        if j.get("success"):
            mark = "✅ 放行"
        else:
            mark = f"❌ 被拒 code={j.get('code')} {j.get('msg')}"
            blocked.append(p)
        print(f"  {mark:<28} {p.split('?')[0]}")
    check("admin 对全部后台接口都正常访问", not blocked,
          "" if not blocked else f"误拦 {len(blocked)} 个：{blocked}")

    print()
    print("=" * 78)
    print(" 第 3 部分：公开接口 —— 游客/会员/管理员都应放行（设计如此）")
    print("=" * 78)
    for p in PUBLIC:
        st, j = call("GET", p)          # 不带令牌
        st_m, j_m = call("GET", p, member_tok)
        check(f"游客与会员都能访问 {p.split('?')[0]}", ok(j) and ok(j_m),
              f"游客 HTTP {st} / 会员 HTTP {st_m}")

    print()
    print("=" * 78)
    print(" 第 4 部分：鉴权边界")
    print("=" * 78)
    st, j = call("GET", "/api/roles?pageNo=1&pageSize=2")
    check("无令牌访问后台接口 → 401", st == 401, f"HTTP {st}")

    print()
    print("=" * 78)
    print(" 第 5 部分：会员令牌 —— 后台接口必须全拒，自己的业务接口必须能用")
    print("=" * 78)
    print("  -- 后台接口（应全部 403）--")
    mem_leak = []
    for p in ADMIN_ONLY:
        st, j = call("GET", p, member_tok)
        if j.get("code") == 403:
            mark = "🔒 403"
        elif j.get("success"):
            mark = "❌ 放行了"
            mem_leak.append(p)
        else:
            mark = f"拒绝({j.get('code')})"
        print(f"  {mark:<22} {p.split('?')[0]}")
    check("会员令牌无法访问任何后台接口", not mem_leak,
          "" if not mem_leak else f"泄漏 {len(mem_leak)} 个：{mem_leak}")

    print("  -- 会员自己的业务接口（必须能用，否则前台会挂）--")
    member_ok = [
        "/api/carts/member/1",
        "/api/member-addresses/account/member",
        "/api/orders/member-account/member",
        "/api/members/id/1",
        "/api/seckill-goods/seckill/1",
    ]
    broken = []
    for p in member_ok:
        st, j = call("GET", p, member_tok)
        # 只要不是 403/401 就算通路正常（数据可能为空）
        good = st != 403 and st != 401
        print(f"  {'✅ 可访问' if good else '❌ 被拦'}   HTTP {st}  {p}")
        if not good:
            broken.append(p)
    check("会员令牌可正常访问自己的业务接口", not broken,
          "" if not broken else f"误拦 {len(broken)} 个：{broken}")

    st, j = call("GET", "/api/menus/mine", member_tok)
    check("会员令牌访问 /api/menus/mine → 被拒（不能看到管理员菜单）",
          not ok(j), f"HTTP {st} code={j.get('code')}")

    st, j = call("GET", "/api/roles?pageNo=1&pageSize=2", member_tok)
    check("会员令牌访问后台接口 → 被拒（非 200 且非成功）", not ok(j),
          f"HTTP {st} code={j.get('code')} msg={j.get('msg')}")

    st, j = call("GET", "/api/menus/mine", op_tok)
    check("operator 的 /api/menus/mine 仍可访问（返回空菜单，非 403）",
          ok(j) and (j.get("data") or []) == [],
          f"HTTP {st} data={j.get('data')}")

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
