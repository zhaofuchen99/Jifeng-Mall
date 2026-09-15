#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
首页轮播（需求 5.3 / FR-102）验证脚本。

轮播并入商品中心，接口在 /api/goods/banners —— 因此它天然落在网关的
「GET /api/goods/** 公开」白名单里，游客能读、写要后台权限。

覆盖：
  1) 游客可读列表/详情，返回按 sort_no 排序
  2) 写操作权限：游客 401 / 会员 403 / 零授权后台 403 / 管理员放行
  3) 新增（含 sort_no、enabled 不传时的 NOT NULL 兜底）
  4) 排序号生效（改 sort_no 后顺序变化）
  5) 停用后前台查询(enabled=true)看不到，后台不过滤仍能看到
  6) 标题必填校验
  7) 删除
  8) 轮播图片可访问（/upload/banner/*.png）

会写测试数据（标题统一带 BANNER_TEST 前缀），跑完自己删干净。
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

MARK = "BANNER_TEST"

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


def head_status(path):
    """只取状态码，用于探测静态图片"""
    req = urllib.request.Request(BASE + path, method="GET")
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, r.headers.get("Content-Type"), len(r.read())
    except urllib.error.HTTPError as e:
        return e.code, None, 0
    except Exception:
        return 0, None, 0


def check(name, cond, detail=""):
    results.append((name, bool(cond)))
    print(f"  [{'PASS' if cond else 'FAIL'}] {name}" + (f"   {detail}" if detail else ""))
    return bool(cond)


def ok(j):
    return isinstance(j, dict) and j.get("success") is True


def front_list():
    """前台首页的取法：只看启用的"""
    st, j = call("GET", "/api/goods/banners?enabled=true&pageNo=1&pageSize=0")
    return (j.get("data") or {}).get("list") or []


def admin_list():
    """后台列表：不过滤，全部"""
    st, j = call("GET", "/api/goods/banners?pageNo=1&pageSize=0")
    return (j.get("data") or {}).get("list") or []


def main():
    print("=" * 78)
    print(" 第 1 部分：游客可读（首页是公开页）")
    print("=" * 78)

    rows = front_list()
    check("游客 GET /api/goods/banners?enabled=true", len(rows) > 0, f"n={len(rows)}")
    check("  种子 3 条都在", len(rows) == 3, f"titles={[r.get('title') for r in rows]}")
    check("  已按 sort_no 升序",
          [r.get("sortNo") for r in rows] == sorted(r.get("sortNo") for r in rows),
          f"sortNos={[r.get('sortNo') for r in rows]}")
    if rows:
        check("  带 imageUrl / linkUrl", bool(rows[0].get("imageUrl")) and bool(rows[0].get("linkUrl")),
              f"imageUrl={rows[0].get('imageUrl')} linkUrl={rows[0].get('linkUrl')}")

    st, j = call("GET", "/api/goods/banners/id/1")
    check("游客 GET /api/goods/banners/id/{id}", st == 200 and ok(j), f"HTTP {st}")

    # 轮播图片本身要能访问（前台 <img> 直接引这个地址）
    for n in (1, 2, 3):
        st, ctype, size = head_status(f"/upload/banner/banner-{n}.png")
        check(f"  轮播图 banner-{n}.png 可访问", st == 200 and size > 1000,
              f"HTTP {st} {ctype} {size}B")

    print()
    print("=" * 78)
    print(" 第 2 部分：写操作权限")
    print("=" * 78)

    st, j = call("POST", "/api/goods/banners", body={"title": MARK})
    check("游客新增应 401", st == 401, f"HTTP {st}")

    st, j = call("POST", "/api/members/login", body={"account": "member", "password": "123456"})
    mtoken = j["data"]["token"] if ok(j) else None
    st, j = call("POST", "/api/goods/banners", mtoken, {"title": MARK})
    check("会员新增应被拒 403", st == 403, f"HTTP {st}")

    st, j = call("POST", "/api/users/login", body={"account": "operator", "password": "123456"})
    otoken = j["data"]["token"] if ok(j) else None
    st, j = call("POST", "/api/goods/banners", otoken, {"title": MARK})
    check("零授权后台账号新增应被拒 403", st == 403, f"HTTP {st}")

    st, j = call("POST", "/api/users/login", body={"account": "admin", "password": "123456"})
    atoken = j["data"]["token"] if ok(j) else None
    if not check("管理员登录", atoken is not None, f"HTTP {st}"):
        return summary()

    print()
    print("=" * 78)
    print(" 第 3 部分：新增 / 排序 / 停用 / 删除")
    print("=" * 78)

    before = len(admin_list())

    # 故意不传 sortNo / enabled，验证后端补默认值（NOT NULL 列的兜底）
    st, j = call("POST", "/api/goods/banners", atoken, {
        "title": MARK + "-新增", "linkUrl": "/help", "imageUrl": ""})
    if not check("管理员新增轮播（不传 sortNo/enabled）", st == 200 and ok(j),
                 f"HTTP {st} code={j.get('code')} {j.get('msg')}"):
        return summary()
    new_id = j["data"]["id"]
    check("  后端补了默认值 sortNo=0 / enabled=true",
          j["data"].get("sortNo") == 0 and j["data"].get("enabled") is True,
          f"sortNo={j['data'].get('sortNo')} enabled={j['data'].get('enabled')}")

    check("  总数 +1", len(admin_list()) == before + 1, f"{before} → {len(admin_list())}")

    # 标题必填
    st, j = call("POST", "/api/goods/banners", atoken, {"title": "   "})
    check("标题为空应被拒(400)", not ok(j) and j.get("code") == 400,
          f"code={j.get('code')} msg={j.get('msg')}")

    # 改排序号到最前（-99），前台第一条应该变成它
    st, j = call("PUT", "/api/goods/banners", atoken, {"id": new_id, "sortNo": -99, "title": MARK + "-改名"})
    check("管理员修改轮播", st == 200 and ok(j), f"HTTP {st}")
    rows = front_list()
    check("  改成 sortNo=-99 后排到最前", rows and rows[0].get("id") == new_id,
          f"first={rows[0].get('title') if rows else None} sortNo={rows[0].get('sortNo') if rows else None}")
    check("  改名已生效", rows and rows[0].get("title") == MARK + "-改名",
          f"title={rows[0].get('title') if rows else None}")

    # 停用
    st, j = call("PUT", "/api/goods/banners", atoken, {"id": new_id, "enabled": False})
    check("管理员停用轮播", st == 200 and ok(j), f"HTTP {st}")
    check("  前台(enabled=true)看不到了", all(r.get("id") != new_id for r in front_list()),
          f"n={len(front_list())}")
    check("  后台不过滤仍能看到", any(r.get("id") == new_id for r in admin_list()), "")

    # 删除
    st, j = call("DELETE", "/api/goods/banners", atoken, [new_id])
    check("管理员删除轮播", st == 200 and ok(j), f"HTTP {st} code={j.get('code')}")
    check("  总数回到原样", len(admin_list()) == before, f"{len(admin_list())} vs {before}")
    check("  已删干净", all(r.get("id") != new_id for r in admin_list()), "")

    # 收尾：确保没有残留（新增后中途失败的情况）
    for r in admin_list():
        if (r.get("title") or "").startswith(MARK):
            call("DELETE", "/api/goods/banners", atoken, [r["id"]])
    left = [r for r in admin_list() if (r.get("title") or "").startswith(MARK)]
    check("清理：无 BANNER_TEST 残留", len(left) == 0, f"left={[r.get('title') for r in left]}")

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
