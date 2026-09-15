#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
admin-web 后端契约验证。

覆盖后台各模块用到的接口：登录、动态菜单、以及各资源的增删改查。
重点验证两个坑：
  1) POST /api/roles、POST /api/groups 必须显式传 enabled（NOT NULL 列 + insert 显式带该字段）
  2) 后台建的用户密码必须被 BCrypt 加密，否则永远登录不上

会写入测试数据（名称统一带 ADM_TEST 前缀 / adm_test_ 账号前缀），跑完由
tools/verify/sql/reset_to_baseline.bat 清理（脚本原先承诺的
admin_cleanup.sql 其实从来不存在，已并入统一的重置脚本）。
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

MARK = "ADM_TEST"

results = []
created = {}


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


def crud_cycle(token, label, base, sample, id_key="id"):
    """通用：新增 → 查回 → 修改 → 删除"""
    st, j = call("POST", base, token, sample)
    if not check(f"{label} 新增", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}"):
        return None
    new_id = j["data"][id_key]
    created.setdefault(label, []).append(new_id)

    st, j = call("GET", f"{base}/id/{new_id}", token)
    check(f"{label} 按 id 查回", st == 200 and ok(j), f"HTTP {st}")

    st, j = call("PUT", base, token, {**sample, id_key: new_id})
    check(f"{label} 修改", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}")

    st, j = call("DELETE", base, token, [new_id])
    check(f"{label} 删除", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}")
    if ok(j):
        created[label].remove(new_id)
    return new_id


def main():
    print("=" * 78)
    print(" 第 1 部分：登录与动态菜单")
    print("=" * 78)

    st, j = call("POST", "/api/users/login", body={"account": "admin", "password": "123456"})
    if not check("POST /api/users/login", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}"):
        return summary()
    token = j["data"]["token"]
    check("  返回 audience=admin", j["data"].get("audience") == "admin",
          f"audience={j['data'].get('audience')}")

    st, j = call("GET", "/api/menus/mine", token)
    menus = j.get("data") or []
    check("GET /api/menus/mine 动态菜单", st == 200 and ok(j) and len(menus) > 0,
          f"n={len(menus)} name={[m['name'] for m in menus]}")

    st, j = call("GET", "/api/goods?pageNo=1&pageSize=1", token)
    check("admin 访问业务接口不被 RBAC 拦截", ok(j), f"HTTP {st} code={j.get('code')}")

    print()
    print("=" * 78)
    print(" 第 2 部分：商品中心")
    print("=" * 78)

    crud_cycle(token, "品牌", "/api/brands",
               {"name": MARK + "_品牌", "company": MARK + " 公司", "site": "https://example.com",
                "description": MARK})

    # 分类树 & 级联删除
    st, j = call("POST", "/api/categories", token,
                 {"parentId": 0, "name": MARK + "_父类", "sort": 900, "description": MARK})
    if check("分类 新增父类", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}"):
        parent_id = j["data"]["id"]
        created.setdefault("分类", []).append(parent_id)

        st, j2 = call("POST", "/api/categories", token,
                      {"parentId": parent_id, "name": MARK + "_子类", "sort": 901})
        check("分类 新增子类", st == 200 and ok(j2), f"HTTP {st}")
        child_id = j2["data"]["id"] if ok(j2) else None

        st, j3 = call("GET", "/api/categories/tree", token)
        found = False
        if ok(j3):
            for n in j3["data"] or []:
                if n["id"] == parent_id and n.get("children"):
                    found = True
        check("  新增的父子关系出现在分类树里", found)

        st, j4 = call("DELETE", "/api/categories", token, [parent_id])
        check("分类 级联删除（父删子也删）", st == 200 and ok(j4), f"HTTP {st}")
        if ok(j4):
            created["分类"].remove(parent_id)
            st, j5 = call("GET", f"/api/categories/id/{child_id}", token)
            check("  子分类确实被级联删掉了", not (ok(j5) and j5.get("data")),
                  f"子类查询返回={j5.get('data')}")

    # 商品新增。**故意不传** isDel / isHot / isTakeDown / isSeckill ——
    # 这几列是 NOT NULL 且 insert 显式带它们，服务端必须自己给默认值，
    # 否则就是 "Column 'is_del' cannot be null"（后台的新增商品表单里本来也没有这些字段）。
    st, j = call("POST", "/api/goods", token,
                 {"spuNo": MARK + "_SPU", "name": MARK + "_商品", "categoryId": 1, "brandId": 1,
                  "markPrice": 199.00, "price": 99.00, "qty": 10, "description": MARK})
    if check("商品 新增（不传标记列，验服务端默认值）", st == 200 and ok(j),
             f"HTTP {st} {j.get('msg')}"):
        gid = j["data"]["id"]
        created.setdefault("商品", []).append(gid)
        d = j["data"]
        check("  isDel/isHot/isTakeDown/isSeckill 被服务端补成 false",
              d.get("isDel") is False and d.get("isHot") is False
              and d.get("isTakeDown") is False and d.get("isSeckill") is False,
              f"isDel={d.get('isDel')} isHot={d.get('isHot')} "
              f"isTakeDown={d.get('isTakeDown')} isSeckill={d.get('isSeckill')}")
        st, j2 = call("DELETE", "/api/goods", token, [gid])
        check("商品 删除", st == 200 and ok(j2), f"HTTP {st}")
        if ok(j2):
            created["商品"].remove(gid)

    print()
    print("=" * 78)
    print(" 第 3 部分：秒杀")
    print("=" * 78)

    st, j = call("POST", "/api/seckills", token, {
        "name": MARK + "_活动", "enabled": True,
        "startTime": time.strftime("%Y-%m-%d %H:%M:%S"),
        "endTime": time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(time.time() + 86400)),
        "description": MARK})
    if check("秒杀活动 新增", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}"):
        act_id = j["data"]["id"]
        created.setdefault("秒杀活动", []).append(act_id)

        # 同样**故意不传 sold**：它是系统累加的已售数，服务端必须补 0
        st, j2 = call("POST", "/api/seckill-goods", token,
                      {"seckillId": act_id, "goodId": 2, "seckillPrice": 1.00,
                       "stock": 3, "limitPerUser": 1, "description": MARK})
        check("秒杀商品 新增（不传 sold，验服务端补 0）", st == 200 and ok(j2),
              f"HTTP {st} {j2.get('msg')}")
        if ok(j2):
            check("  sold 被服务端补成 0", j2["data"].get("sold") == 0,
                  f"sold={j2['data'].get('sold')}")
        if ok(j2):
            sg_id = j2["data"]["id"]
            created.setdefault("秒杀商品", []).append(sg_id)
            st, j3 = call("PUT", "/api/seckill-goods", token, {"id": sg_id, "stock": 5})
            check("秒杀商品 修改库存", st == 200 and ok(j3), f"HTTP {st}")
            st, j4 = call("DELETE", "/api/seckill-goods", token, [sg_id])
            check("秒杀商品 删除", st == 200 and ok(j4), f"HTTP {st}")
            if ok(j4):
                created["秒杀商品"].remove(sg_id)

        st, j5 = call("DELETE", "/api/seckills", token, [act_id])
        check("秒杀活动 删除", st == 200 and ok(j5), f"HTTP {st}")
        if ok(j5):
            created["秒杀活动"].remove(act_id)

    print()
    print("=" * 78)
    print(" 第 4 部分：系统管理 RBAC（重点验 enabled NOT NULL 的坑）")
    print("=" * 78)

    crud_cycle(token, "角色", "/api/roles",
               {"name": MARK + "_角色", "enabled": True, "description": MARK})
    crud_cycle(token, "用户组", "/api/groups",
               {"name": MARK + "_用户组", "enabled": True, "description": MARK})
    crud_cycle(token, "权限", "/api/perms",
               {"name": MARK + "_权限", "enabled": True, "description": MARK})
    crud_cycle(token, "资源", "/api/resources",
               {"name": MARK + "_资源", "type": "接口", "value": "/api/adm-test/**",
                "description": MARK})
    crud_cycle(token, "菜单", "/api/menus",
               {"parentId": 0, "resourceId": 1001, "name": MARK + "_菜单",
                "icon": "el-icon-house", "url": "/adm-test", "sort": 900, "description": MARK})

    # 后台建用户 + 能否登录（验证密码加密）
    uname = "adm_test_" + str(int(time.time()))
    # 只传用户名和密码，enabled/status/locked/login_times 都交给服务端补默认值
    st, j = call("POST", "/api/users", token,
                 {"username": uname, "password": "test123456", "description": MARK})
    if check("后台用户 新增（只传用户名密码，验服务端补默认值）", st == 200 and ok(j),
             f"HTTP {st} {j.get('msg')}"):
        uid = j["data"]["id"]
        created.setdefault("用户", []).append(uid)

        pwd = j["data"].get("password") or ""
        check("  响应里的密码已被 BCrypt 加密（不是明文）",
              pwd.startswith("$2") and len(pwd) > 50, f"password={pwd[:20]}...")

        st, j2 = call("POST", "/api/users/login", body={"account": uname, "password": "test123456"})
        check("  新建的后台用户能正常登录", ok(j2), f"HTTP {st} {j2.get('msg')}")

        st, j3 = call("DELETE", "/api/users", token, [uid])
        check("后台用户 删除", st == 200 and ok(j3), f"HTTP {st}")
        if ok(j3):
            created["用户"].remove(uid)

    print()
    print("=" * 78)
    print(" 第 5 部分：关联表")
    print("=" * 78)

    # 用现成的 组1(管理员组) 与 角色2(运营人员) 建一条关联再删掉
    st, j = call("GET", "/api/group-roles/group/1", token)
    check("GET /api/group-roles/group/{id} 反查", st == 200 and ok(j),
          f"HTTP {st} n={len(j.get('data') or [])}")

    st, j = call("POST", "/api/group-roles", token, {"groupId": 1, "roleId": 2})
    if check("组-角色 新增关联", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}"):
        gr_id = j["data"]["id"]
        st, j2 = call("GET", "/api/group-roles/group/1", token)
        check("  反查能看到新增的关联", len(j2.get("data") or []) == 2,
              f"n={len(j2.get('data') or [])}")
        st, j3 = call("DELETE", "/api/group-roles", token, [gr_id])
        check("组-角色 删除关联（还原种子数据）", st == 200 and ok(j3), f"HTTP {st}")

    st, j = call("GET", "/api/user-groups/user/1", token)
    check("GET /api/user-groups/user/{id} 反查", st == 200 and ok(j),
          f"HTTP {st} n={len(j.get('data') or [])}")
    st, j = call("GET", "/api/role-perms/role/1", token)
    check("GET /api/role-perms/role/{id} 反查", st == 200 and ok(j),
          f"HTTP {st} n={len(j.get('data') or [])}")
    st, j = call("GET", "/api/perm-resources/perm/301", token)
    check("GET /api/perm-resources/perm/{id} 反查", st == 200 and ok(j),
          f"HTTP {st} n={len(j.get('data') or [])}")

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
    if created:
        print("\n 若中途失败可能残留的测试数据：")
        for k, v in created.items():
            if v:
                print(f"   {k}: {v}")
        print(" 清理脚本：tools/verify/sql/reset_to_baseline.bat")


if __name__ == "__main__":
    main()
