#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
地区管理（需求 FR-210 / 6.10「行政区划树维护（省/市/区）」）验证脚本。

region-api 原先只有查询接口，后台「地区管理」是只读页。本轮补上了
POST / PUT / DELETE，并把该模块挂进 RBAC（资源 1007 菜单 + 2021 接口、权限 306）。

覆盖：
  1) 读公开、写要权限：游客 GET 200 / 游客写 401 / 会员写 403 / 零授权后台 403
  2) 层级由上级推导：根 1、逐级 +1、第四级被拒；客户端传的 level 一律被忽略
  3) 编码（id）可显式指定、可留空自增；重复编码被拒
  4) 名称必填、上级必须存在
  5) 改父级：挂到自己或自己的下级被拒；父子层级整体跟随移动
  6) 删除级联：删省级连市/区一起删，返回实际删除行数
  7) **缓存失效**：区划列表/详情带 @Cacheable，写操作必须清缓存，
     否则前台省市区三级联动一直读到改动前的数据
  8) 排序号生效

测试数据统一用 9990xx 编码（种子里没有 9 开头），跑完自己删干净。
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


# 测试用编码。区划编码是主键，种子里只有 110000/110100/440000/440100/440106，
# 9 开头这个段位不会撞上真实数据。
P = 999001     # 省
C = 999002     # 市
D = 999003     # 区
AUTO_ID = 999009   # 用于测"留空自增"的占位（实际会拿到自增 id，跑完一并清理）

TEST_IDS = [999001, 999002, 999003, 999004, 999005, 999006, 999009]

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


def rows(token=None):
    """全量列表（pageSize=0 = 不分页）"""
    st, j = call("GET", "/api/regions?pageNo=1&pageSize=0", token)
    return (j.get("data") or {}).get("list") or []


def find(token, rid):
    for r in rows(token):
        if r.get("id") == rid:
            return r
    return None


def children(pid):
    """干净取法：直接打接口，用于观察缓存效果"""
    st, j = call("GET", f"/api/regions/parent-id/{pid}")
    return (j.get("data") or []) if ok(j) else None


def cleanup(token):
    """删掉所有测试编码。级联删除会带走下级，多删几个 id 是安全的。"""
    call("DELETE", "/api/regions", token, TEST_IDS)
    # 自增产生的 id 不在上面的列表里，按名称兜底扫一遍
    left = [r["id"] for r in rows(token) if str(r.get("name", "")).startswith("REG_TEST")]
    if left:
        call("DELETE", "/api/regions", token, left)


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
    st, j = call("POST", "/api/users/login", body={"account": "admin", "password": "123456"})
    atoken = j["data"]["token"] if ok(j) else None
    if not check("管理员登录", atoken is not None, f"HTTP {st}"):
        return summary()
    cleanup(atoken)   # 上一轮若有残留先清掉

    seed_count = len(rows(atoken))

    # ------------------------------------------------------------------
    print()
    print("=" * 78)
    print(" 第 1 部分：读公开、写要权限")
    print("=" * 78)

    st, j = call("GET", "/api/regions?pageNo=1&pageSize=0")
    check("游客可读区划列表", st == 200 and ok(j), f"HTTP {st}")

    st, j = call("GET", "/api/regions/parent-id/0")
    check("游客可读省级列表", st == 200 and ok(j), f"HTTP {st}")

    st, j = call("POST", "/api/regions", body={"name": "REG_TEST-游客"})
    check("游客新增应 401", st == 401, f"HTTP {st}")

    st, j = call("POST", "/api/members/login", body={"account": "member", "password": "123456"})
    mtoken = j["data"]["token"] if ok(j) else None
    st, j = call("POST", "/api/regions", mtoken, {"name": "REG_TEST-会员"})
    check("会员新增应被拒 403", st == 403, f"HTTP {st}")

    st, j = call("POST", "/api/users/login", body={"account": "operator", "password": "123456"})
    otoken = j["data"]["token"] if ok(j) else None
    st, j = call("POST", "/api/regions", otoken, {"name": "REG_TEST-零授权"})
    check("零授权后台账号新增应被拒 403", st == 403, f"HTTP {st}")

    st, j = call("DELETE", "/api/regions", mtoken, [1])
    check("会员删除应被拒 403", st == 403, f"HTTP {st}")

    st, j = call("PUT", "/api/regions", otoken, {"id": 110000, "name": "REG_TEST-改"})
    check("零授权后台账号改区划应被拒 403", st == 403, f"HTTP {st}")

    # ------------------------------------------------------------------
    print()
    print("=" * 78)
    print(" 第 2 部分：层级由上级推导，客户端传的 level 不作数")
    print("=" * 78)

    # 故意传 level=9：应被忽略，实际由 parentId=0 推导成 1
    st, j = call("POST", "/api/regions", atoken,
                 {"id": P, "name": "REG_TEST-省", "parentId": 0, "level": 9, "sortOrder": 1})
    if not check("新增省级（编码显式指定，夹带 level=9）",
                 st == 200 and ok(j), f"HTTP {st} code={j.get('code')} {j.get('msg')}"):
        cleanup(atoken)
        return summary()
    check("  客户端的 level=9 被忽略，实际 level=1",
          j["data"].get("level") == 1, f"level={j['data'].get('level')}")

    st, j = call("POST", "/api/regions", atoken,
                 {"id": C, "name": "REG_TEST-市", "parentId": P, "sortOrder": 1})
    ok_c = check("新增市级", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}")
    if ok_c:
        check("  层级自动推导为 2", j["data"].get("level") == 2, f"level={j['data'].get('level')}")

    st, j = call("POST", "/api/regions", atoken,
                 {"id": D, "name": "REG_TEST-区", "parentId": C, "sortOrder": 1})
    ok_d = check("新增区级", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}")
    if ok_d:
        check("  层级自动推导为 3", j["data"].get("level") == 3, f"level={j['data'].get('level')}")

    st, j = call("POST", "/api/regions", atoken,
                 {"id": 999004, "name": "REG_TEST-四级", "parentId": D})
    check("第四级应被拒（行政区划最多三级）",
          st == 200 and j.get("code") == 400, f"code={j.get('code')} {j.get('msg')}")

    # 留空 id → 自增
    st, j = call("POST", "/api/regions", atoken,
                 {"name": "REG_TEST-自增编码", "parentId": P})
    if check("编码留空时自增", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}"):
        auto_id = j["data"].get("id")
        check("  后端返回了自增出来的编码", auto_id not in (None, 0), f"id={auto_id}")
        if auto_id:
            TEST_IDS.append(auto_id)

    # ------------------------------------------------------------------
    print()
    print("=" * 78)
    print(" 第 3 部分：字段校验")
    print("=" * 78)

    st, j = call("POST", "/api/regions", atoken, {"id": P, "name": "REG_TEST-重复编码"})
    check("重复编码应被拒", st == 200 and j.get("code") == 400,
          f"code={j.get('code')} {j.get('msg')}")

    st, j = call("POST", "/api/regions", atoken, {"id": 999005, "name": "   ", "parentId": 0})
    check("名称为空应被拒", st == 200 and j.get("code") == 400,
          f"code={j.get('code')} {j.get('msg')}")

    st, j = call("POST", "/api/regions", atoken, {"id": 999006, "name": "REG_TEST-孤儿", "parentId": 888888})
    check("上级不存在应被拒", st == 200 and j.get("code") == 400,
          f"code={j.get('code')} {j.get('msg')}")

    # ------------------------------------------------------------------
    print()
    print("=" * 78)
    print(" 第 4 部分：缓存失效（写操作必须清掉 @Cacheable）")
    print("=" * 78)

    # 先把缓存读热
    before = children(P)
    check("预热缓存：省级下级列表可读", before is not None, f"n={len(before or [])}")

    st, j = call("POST", "/api/regions", atoken, {"name": "REG_TEST-缓存", "parentId": P})
    new_id = j["data"]["id"] if ok(j) else None
    if new_id:
        TEST_IDS.append(new_id)
    after = children(P)
    check("新增后 findByParentId 缓存已失效（新节点立刻可见）",
          new_id is not None and any(r["id"] == new_id for r in after),
          f"新增前 {len(before or [])} 条 → 新增后 {len(after or [])} 条")

    # findById 缓存：改名前先读一次
    st, j = call("GET", f"/api/regions/id/{C}")
    name_before = (j.get("data") or {}).get("name")
    st, j = call("PUT", "/api/regions", atoken, {"id": C, "name": "REG_TEST-市-改名"})
    st, j = call("GET", f"/api/regions/id/{C}")
    check("改名后 findById 缓存已失效",
          (j.get("data") or {}).get("name") == "REG_TEST-市-改名",
          f"{name_before} → {(j.get('data') or {}).get('name')}")

    # ------------------------------------------------------------------
    print()
    print("=" * 78)
    print(" 第 5 部分：改父级 —— 成环检测与层级整体迁移")
    print("=" * 78)

    st, j = call("PUT", "/api/regions", atoken, {"id": C, "parentId": C})
    check("把区划挂到自己下面应被拒", st == 200 and j.get("code") == 400,
          f"code={j.get('code')} {j.get('msg')}")

    st, j = call("PUT", "/api/regions", atoken, {"id": C, "parentId": D})
    check("把市挂到自己的下级（区）下面应被拒（成环）",
          st == 200 and j.get("code") == 400, f"code={j.get('code')} {j.get('msg')}")

    st, j = call("GET", f"/api/regions/id/{C}")
    check("  被拒后市级的位置没被改动",
          (j.get("data") or {}).get("parentId") == P,
          f"parentId={(j.get('data') or {}).get('parentId')} 期望 {P}")

    # 市（level2，带子节点区 level3）提到根 → 市变 1、区跟着变 2
    st, j = call("PUT", "/api/regions", atoken, {"id": C, "parentId": 0})
    check("把市级提到顶级", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}")
    st, j = call("GET", f"/api/regions/id/{C}")
    check("  市级自身层级 2 → 1", (j.get("data") or {}).get("level") == 1,
          f"level={(j.get('data') or {}).get('level')}")
    st, j = call("GET", f"/api/regions/id/{D}")
    check("  其后代（区）层级跟着 3 → 2", (j.get("data") or {}).get("level") == 2,
          f"level={(j.get('data') or {}).get('level')}")

    # 复原
    st, j = call("PUT", "/api/regions", atoken, {"id": C, "parentId": P})
    st, j = call("GET", f"/api/regions/id/{D}")
    check("  改回原父级后，后代层级回到 3", (j.get("data") or {}).get("level") == 3,
          f"level={(j.get('data') or {}).get('level')}")

    # 只改名称、不传 parentId：不能把节点悄悄提到根
    st, j = call("PUT", "/api/regions", atoken, {"id": D, "name": "REG_TEST-区-只改名"})
    st, j = call("GET", f"/api/regions/id/{D}")
    check("局部更新（不传 parentId）不会把节点移到根",
          (j.get("data") or {}).get("parentId") == C and (j.get("data") or {}).get("level") == 3,
          f"parentId={(j.get('data') or {}).get('parentId')} level={(j.get('data') or {}).get('level')}")

    # ------------------------------------------------------------------
    print()
    print("=" * 78)
    print(" 第 6 部分：排序号生效")
    print("=" * 78)

    st, j = call("POST", "/api/regions", atoken,
                 {"name": "REG_TEST-排序B", "parentId": P, "sortOrder": 20})
    id_b = j["data"]["id"] if ok(j) else None
    st, j = call("POST", "/api/regions", atoken,
                 {"name": "REG_TEST-排序A", "parentId": P, "sortOrder": 10})
    id_a = j["data"]["id"] if ok(j) else None
    if id_a:
        TEST_IDS.append(id_a)
    if id_b:
        TEST_IDS.append(id_b)

    kids = children(P) or []
    order = [r["id"] for r in kids if r["id"] in (id_a, id_b)]
    check("下级按 sortOrder 排序（20 的排在 10 的后面）",
          order == [id_a, id_b], f"顺序={order}")

    # ------------------------------------------------------------------
    print()
    print("=" * 78)
    print(" 第 7 部分：缓存失效（删除）与级联删除")
    print("=" * 78)

    nodes = len(children(P) or [])
    check("删除前省级下挂着若干下级", nodes >= 2, f"n={nodes}")

    st, j = call("DELETE", "/api/regions", atoken, [P])
    check("删除省级成功", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}")
    deleted = j.get("data")
    check("  返回的行数大于 1（说明级联删掉了下级）",
          isinstance(deleted, int) and deleted > 1, f"删除行数={deleted}")

    left = children(P)
    check("  删完后 parent-id 查询缓存已失效且结果为空",
          left == [] or left is None, f"n={len(left or [])}")

    remaining = rows(atoken)
    gone = all(r["id"] not in (P, C, D) for r in remaining)
    check("  省/市/区三行都已从列表消失", gone)

    # ------------------------------------------------------------------
    print()
    print("=" * 78)
    print(" 第 8 部分：后台菜单挂载（原先硬编码，现走 RBAC 动态菜单）")
    print("=" * 78)

    def menus(token):
        st, j = call("GET", "/api/menus/mine", token)
        return (j.get("data") or []) if ok(j) else None

    admin_menus = menus(atoken) or []
    region_menu = [m for m in admin_menus if m.get("url") == "/region"]
    check("管理员的动态菜单里有「地区管理」", len(region_menu) == 1,
          f"菜单={[m.get('url') for m in admin_menus]}")
    if region_menu:
        check("  菜单挂了资源 1007、排序 7",
              region_menu[0].get("resourceId") == 1007 and region_menu[0].get("sort") == 7,
              f"resourceId={region_menu[0].get('resourceId')} sort={region_menu[0].get('sort')}")

    op_menus = menus(otoken)
    check("零授权账号看不到「地区管理」（FR-201 无权限的菜单不显示）",
          op_menus is not None and all(m.get("url") != "/region" for m in op_menus),
          f"operator 菜单={op_menus}")

    # ------------------------------------------------------------------
    print()
    print("=" * 78)
    print(" 第 9 部分：清理与种子数据核对")
    print("=" * 78)

    cleanup(atoken)
    final = rows(atoken)
    check("测试数据已全部清理", len(final) == seed_count,
          f"清理前 {seed_count} 条 → 清理后 {len(final)} 条")

    seed = {110000: "北京市", 110100: "北京市市辖区", 440000: "广东省",
            440100: "广州市", 440106: "天河区"}
    intact = all(any(r["id"] == k and r["name"] == v for r in final) for k, v in seed.items())
    check("种子里 5 条区划完好", intact and len(final) == 5, f"共 {len(final)} 条")

    return summary()


if __name__ == "__main__":
    import sys
    sys.exit(0 if main() else 1)
