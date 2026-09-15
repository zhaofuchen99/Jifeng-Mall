#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
登录防爆破验证脚本（需求 6.2「错误次数限制与锁定」、NFR-002「登录失败限流」）。

走 vite dev server 的代理，与浏览器同一条路径。

⚠️ 刻意**不用** admin / member 这两个演示账号做锁定测试——锁是 15 分钟 TTL，
   锁上就没法演示了。全部用现场注册的临时账号，跑完删掉。

覆盖：
  1) 正常登录不受影响（回归）
  2) 阈值：错 4 次不锁、第 5 次锁，错误码 400 → 429
  3) 锁定期内即使密码正确也拒绝（先判锁、连密码都不校验）
  4) 锁定剩余时间随 TTL 递减（等 65 秒复测）
  5) 成功登录清零累计失败次数
  6) 账号不存在同样计数（否则可用「有没有被锁」枚举出真实账号）
  7) 按账号隔离，不误伤其它账号；会员与后台两套登录行为一致
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

PWD = "lg_test_123456"
MARK = "lg_test_"

results = []
created = {"users": [], "members": []}


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


def admin_login(account, password):
    return call("POST", "/api/users/login", body={"account": account, "password": password})


def member_login(account, password):
    return call("POST", "/api/members/login", body={"account": account, "password": password})


def fail_n(login_fn, account, n, times=None):
    """连续错误登录 n 次，返回最后一次的响应"""
    st, j = 0, {}
    for i in range(n):
        st, j = login_fn(account, "definitely-wrong")
        if times is not None:
            times.append((j.get("code"), j.get("msg")))
    return st, j


def main():
    print("=" * 78)
    print(" 第 0 部分：正常登录不受影响（回归）")
    print("=" * 78)

    st, j = admin_login("admin", "123456")
    check("admin 正常登录", st == 200 and ok(j), f"HTTP {st} code={j.get('code')} {j.get('msg')}")
    atoken = j["data"]["token"] if ok(j) else None

    st, j = member_login("member", "123456")
    check("member 正常登录", st == 200 and ok(j), f"HTTP {st} code={j.get('code')} {j.get('msg')}")
    if not atoken:
        return summary()

    # 建两个临时后台账号
    for name in ("admin_a", "admin_b"):
        u = MARK + name
        st, j = call("POST", "/api/users", atoken, {"username": u, "password": PWD})
        if ok(j):
            created["users"].append(u)
    if not check("建临时后台账号", len(created["users"]) == 2, f"{created['users']}"):
        return summary()

    ua, ub = created["users"]

    print()
    print("=" * 78)
    print(" 第 1 部分：后台登录 阈值与锁定")
    print("=" * 78)

    st, j = admin_login(ua, PWD)
    check(f"{ua} 正确密码可登录", st == 200 and ok(j), f"code={j.get('code')} {j.get('msg')}")

    logs = []
    st, j = fail_n(admin_login, ua, 4, logs)
    check(f"连续错 4 次仍是普通错误(400)", all(c == 400 for c, _ in logs),
          f"codes={[c for c, _ in logs]} msg={logs[-1][1]}")

    st, j = fail_n(admin_login, ua, 1, logs)
    check("第 5 次触发锁定(429)", j.get("code") == 429, f"code={j.get('code')} msg={j.get('msg')}")
    lock_msg = j.get("msg") or ""
    check("  锁定提示含「锁定」", "锁定" in lock_msg, lock_msg)

    st, j = admin_login(ua, PWD)
    check("锁定期内正确密码也被拒(429)", j.get("code") == 429,
          f"code={j.get('code')} msg={j.get('msg')}")

    # 从提示里解析剩余分钟，落在 1~15 之间
    minutes_first = parse_minutes(j.get("msg") or "")
    check("  剩余锁定时间在 1~15 分钟之间", 1 <= minutes_first <= 15,
          f"minutes={minutes_first} msg={j.get('msg')}")

    st, j = admin_login(ub, PWD)
    check("另一个账号不受影响（按账号隔离）", st == 200 and ok(j),
          f"code={j.get('code')} {j.get('msg')}")

    print("  ... 等 65 秒复测剩余锁定时间（验证 TTL 在走）")
    time.sleep(65)
    st, j = admin_login(ua, PWD)
    minutes_later = parse_minutes(j.get("msg") or "")
    check("剩余锁定时间随 TTL 递减", j.get("code") == 429 and minutes_later < minutes_first,
          f"{minutes_first} → {minutes_later} 分钟，msg={j.get('msg')}")

    print()
    print("=" * 78)
    print(" 第 2 部分：成功登录清零累计失败")
    print("=" * 78)

    logs = []
    fail_n(admin_login, ub, 3, logs)
    check(f"{ub} 错 3 次后仍可登录", admin_login(ub, PWD)[1].get("success") is True, "")

    logs = []
    st, j = fail_n(admin_login, ub, 4, logs)
    check("清零后再错 4 次仍未锁定（若没清零，3+4=7 早该锁）",
          all(c == 400 for c, _ in logs), f"codes={[c for c, _ in logs]}")

    print()
    print("=" * 78)
    print(" 第 3 部分：不存在的账号也计数（防账号枚举）")
    print("=" * 78)

    ghost = MARK + "no_such_account"
    logs = []
    st, j = fail_n(admin_login, ghost, 4, logs)
    check("不存在账号错 4 次仍是普通错误",
          all(c == 400 and "用户名或密码错误" in (m or "") for c, m in logs),
          f"codes={[c for c, _ in logs]}")
    st, j = fail_n(admin_login, ghost, 1, logs)
    check("不存在账号第 5 次同样锁定(429) —— 否则可据此枚举真实账号",
          j.get("code") == 429, f"code={j.get('code')} msg={j.get('msg')}")

    print()
    print("=" * 78)
    print(" 第 4 部分：会员登录防爆破（与后台一致）")
    print("=" * 78)

    mu = MARK + "member_a"
    st, j = call("POST", "/api/members/register", body={"account": mu, "password": PWD, "name": "防爆破测试"})
    if check("注册临时会员", ok(j), f"code={j.get('code')} {j.get('msg')}"):
        created["members"].append(mu)
        st, j = member_login(mu, PWD)
        check("  正确密码可登录", st == 200 and ok(j), f"code={j.get('code')} {j.get('msg')}")

        logs = []
        st, j = fail_n(member_login, mu, 4, logs)
        check("  错 4 次仍是普通错误(400)", all(c == 400 for c, _ in logs),
              f"codes={[c for c, _ in logs]}")

        st, j = fail_n(member_login, mu, 1, logs)
        check("  第 5 次锁定(429)", j.get("code") == 429, f"code={j.get('code')} msg={j.get('msg')}")

        st, j = member_login(mu, PWD)
        check("  锁定期内正确密码也被拒(429)", j.get("code") == 429,
              f"code={j.get('code')} msg={j.get('msg')}")

        st, j = member_login("member", "123456")
        check("  演示账号 member 不受影响", st == 200 and ok(j),
              f"code={j.get('code')} {j.get('msg')}")

    cleanup(atoken)
    summary()


def parse_minutes(msg):
    """从「...请 14 分钟后重试」里取出分钟数，取不到返回 -1"""
    import re
    m = re.search(r"请\s*(\d+)\s*分钟", msg or "")
    return int(m.group(1)) if m else -1


def cleanup(atoken):
    print()
    print("=" * 78)
    print(" 清理：删掉临时账号")
    print("=" * 78)

    for u in created["users"]:
        st, j = call("GET", f"/api/users?username={u}", atoken)
        rows = ((j.get("data") or {}).get("list") or []) if ok(j) else []
        ids = [r["id"] for r in rows if r.get("username") == u]
        if ids:
            st, j = call("DELETE", "/api/users", atoken, ids)
            check(f"删除后台账号 {u}", ok(j), f"HTTP {st} code={j.get('code')} {j.get('msg')}")

    for m in created["members"]:
        st, j = call("GET", f"/api/members?account={m}", atoken)
        rows = ((j.get("data") or {}).get("list") or []) if ok(j) else []
        ids = [r["id"] for r in rows if r.get("account") == m]
        if ids:
            st, j = call("DELETE", "/api/members", atoken, ids)
            check(f"删除会员 {m}", ok(j), f"HTTP {st} code={j.get('code')} {j.get('msg')}")

    print()
    print(" 注：临时账号已删，Redis 里 login:fail:admin:lg_test_* 这些键最多 15 分钟后自行过期，无副作用。")


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
