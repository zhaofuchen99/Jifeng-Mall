#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
会员改密码 / 后台重置密码 验证脚本（需求 FR-207「重置密码」+ FR-101 个人中心）。

同时覆盖 bug 14：会员自助改资料/改密码原先被网关 403（裸 PUT /api/members 不在
MEMBER_PATHS 里），修复后改走 /api/members/id/{id} 系列。

覆盖：
  1) 会员自助改资料（自己的能改、别人的 403、请求体里的 password 被丢弃）
  2) 会员自助改密码（服务端校验旧密码、长度校验、越界 403）
  3) 后台重置密码（旧密码立即失效、裸 PUT /api/members 仍为后台专用）
  4) 与防爆破联动：被锁定的会员，重置密码后立刻能登录（后台路径 + 自助路径都验）

临时会员统一 mrp_test_ 前缀，跑完删掉。
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

P1, P2, P3 = "mrp_old_111", "mrp_new_222", "mrp_admin_333"
MARK = "mrp_test_"
VICTIM_ID = 1          # 种子会员 member

results = []


def call(method, path, token=None, body=None, timeout=20):
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


def mlogin(account, password):
    return call("POST", "/api/members/login", body={"account": account, "password": password})


def login_ok(account, password):
    st, j = mlogin(account, password)
    return st == 200 and ok(j)


def register(account, password):
    st, j = call("POST", "/api/members/register",
                 body={"account": account, "password": password, "name": "改密测试"})
    if ok(j):
        return j["data"]["userId"], j["data"]["token"]
    return None, None


def main():
    st, j = call("POST", "/api/users/login", body={"account": "admin", "password": "123456"})
    if not check("管理员登录", ok(j), f"HTTP {st} {j.get('msg')}"):
        return summary()
    atoken = j["data"]["token"]

    a = MARK + "a"
    aid, atok = register(a, P1)
    if not check(f"注册临时会员 {a}", aid is not None, f"id={aid}"):
        return summary()

    print()
    print("=" * 78)
    print(" 第 1 部分：会员自助改资料（bug 14）")
    print("=" * 78)

    st, j = call("PUT", f"/api/members/id/{aid}", atok, {"name": "改过的名字", "phone": "13900000001"})
    check("会员改自己的资料", st == 200 and ok(j), f"HTTP {st} code={j.get('code')} {j.get('msg')}")

    st, j = call("GET", f"/api/members/id/{aid}", atok)
    check("  改后回读生效", ok(j) and j["data"].get("name") == "改过的名字",
          f"name={j.get('data', {}).get('name')}")

    # 注意两种 403 不是一个东西：
    #   网关层的拒绝 → 真实 HTTP 403（如裸 PUT /api/members 不在会员白名单里）
    #   业务层的拒绝 → HTTP 200 + body 里 code=403（统一响应结构，见 utils/request.js 的说明）
    # 归属校验在 member-api 里，所以走的是后者。
    st, j = call("PUT", f"/api/members/id/{VICTIM_ID}", atok, {"name": "越界改名"})
    check("会员改**别人**的资料应被拒", st == 200 and j.get("code") == 403,
          f"HTTP {st} code={j.get('code')} msg={j.get('msg')}")

    st, j = call("GET", f"/api/members/id/{VICTIM_ID}", atoken)
    check("  受害者资料未被改动", ok(j) and j["data"].get("name") != "越界改名",
          f"name={j.get('data', {}).get('name')}")

    st, j = call("PUT", f"/api/members/id/{aid}", atok, {"name": "再改一次", "password": P2})
    check("会员改资料时夹带 password 请求被接受（会被服务端丢弃）", st == 200 and ok(j), f"HTTP {st}")
    check("  旧密码仍然有效（password 确实被丢弃）", login_ok(a, P1), "")
    check("  夹带的新密码并未生效", not login_ok(a, P2), "")

    print()
    print("=" * 78)
    print(" 第 2 部分：会员自助改密码（服务端校验旧密码）")
    print("=" * 78)

    st, j = call("PUT", f"/api/members/id/{aid}/password", atok,
                 {"oldPassword": "definitely-wrong", "newPassword": P2})
    check("旧密码错误应被拒(400)", j.get("code") == 400 and "当前密码" in (j.get("msg") or ""),
          f"code={j.get('code')} msg={j.get('msg')}")

    st, j = call("PUT", f"/api/members/id/{aid}/password", atok, {"oldPassword": P1, "newPassword": "123"})
    check("新密码过短应被拒(400)", j.get("code") == 400, f"code={j.get('code')} msg={j.get('msg')}")

    st, j = call("PUT", f"/api/members/id/{aid}/password", atok, {"oldPassword": P1, "newPassword": P2})
    check("会员自助改密码", st == 200 and ok(j), f"HTTP {st} code={j.get('code')} {j.get('msg')}")
    check("  旧密码立即失效", not login_ok(a, P1), "")
    check("  新密码可登录", login_ok(a, P2), "")

    st, j = call("PUT", f"/api/members/id/{VICTIM_ID}/password", atok,
                 {"oldPassword": "123456", "newPassword": "hacked123"})
    check("会员改**别人**的密码应被拒（业务层 403）", st == 200 and j.get("code") == 403,
          f"HTTP {st} code={j.get('code')} msg={j.get('msg')}")
    check("  受害者原密码未被改掉", login_ok("member", "123456"), "")

    st, j = call("PUT", f"/api/members/id/{aid}/password", body={"oldPassword": P2, "newPassword": P3})
    check("无令牌改密码应 401", st == 401, f"HTTP {st}")

    print()
    print("=" * 78)
    print(" 第 3 部分：后台重置密码（FR-207）")
    print("=" * 78)

    st, j = call("PUT", "/api/members", atok, {"name": "会员越权改名"})
    check("会员调裸 PUT /api/members 仍被拒 403（保持后台专用）", st == 403, f"HTTP {st}")

    st, j = call("PUT", "/api/members", atoken, {"id": aid, "password": P3})
    check("管理员重置密码（PUT /api/members 带 password）", st == 200 and ok(j),
          f"HTTP {st} code={j.get('code')} {j.get('msg')}")
    check("  原密码立即失效", not login_ok(a, P2), "")
    check("  新密码可登录（登录走 BCrypt 比对，能过就说明库里的确是密文）", login_ok(a, P3), "")

    st, j = call("PUT", "/api/members", atoken, {"id": aid, "password": "ab"})
    check("后台重置成过短密码也应被拒(400)", j.get("code") == 400,
          f"code={j.get('code')} msg={j.get('msg')}")
    check("  被拒后原密码未被改掉", login_ok(a, P3), "")

    print()
    print("=" * 78)
    print(" 第 4 部分：与登录防爆破联动（重置密码顺带解锁）")
    print("=" * 78)

    b = MARK + "b"
    bid, btok = register(b, P1)
    if check(f"注册临时会员 {b}", bid is not None, f"id={bid}"):
        for _ in range(5):
            mlogin(b, "wrong-password")
        st, j = mlogin(b, P1)
        check("连错 5 次后已被锁定(429)", j.get("code") == 429, f"code={j.get('code')} msg={j.get('msg')}")

        st, j = call("PUT", "/api/members", atoken, {"id": bid, "password": P2})
        check("  管理员重置密码", st == 200 and ok(j), f"HTTP {st}")
        check("  重置后**立刻**能用新密码登录（锁定已解除）", login_ok(b, P2),
              "若没解除，这里会继续 429")

    c = MARK + "c"
    cid, ctok = register(c, P1)
    if check(f"注册临时会员 {c}", cid is not None, f"id={cid}"):
        for _ in range(5):
            mlogin(c, "wrong-password")
        st, j = mlogin(c, P1)
        check("连错 5 次后已被锁定(429)", j.get("code") == 429, f"code={j.get('code')}")

        st, j = call("PUT", f"/api/members/id/{cid}/password", ctok,
                     {"oldPassword": P1, "newPassword": P2})
        check("  会员自助改密码（持旧令牌，仍可调）", st == 200 and ok(j),
              f"HTTP {st} code={j.get('code')} {j.get('msg')}")
        check("  自助改密码后也能立刻登录（锁定同样被解除）", login_ok(c, P2), "")

    # 清理
    print()
    print("=" * 78)
    print(" 清理临时会员")
    print("=" * 78)
    st, j = call("GET", "/api/members?pageNo=1&pageSize=0", atoken)
    rows = ((j.get("data") or {}).get("list") or []) if ok(j) else []
    ids = [r["id"] for r in rows if (r.get("account") or "").startswith(MARK)]
    if ids:
        st, j = call("DELETE", "/api/members", atoken, ids)
        check(f"删除临时会员 {ids}", ok(j), f"HTTP {st} code={j.get('code')} {j.get('msg')}")
    st, j = call("GET", "/api/members?pageNo=1&pageSize=0", atoken)
    left = [r.get("account") for r in (((j.get("data") or {}).get("list") or [])) if (r.get("account") or "").startswith(MARK)]
    check("无残留", len(left) == 0, f"left={left}")

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
