#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
品牌 / 分类「删除禁引校验」验证（设计文档 5.3）。

背景：原先删品牌/分类是**裸删** —— 删掉之后引用它的商品还在，
但商品详情组装品牌/分类时得到 null，列表里那一栏凭空消失、且没有任何报错。
典型的静默数据损坏。现在删除前会调 good-api 统计引用，有引用就拒绝。

覆盖：
  1) 品牌下有商品 → 拒删；清掉商品后 → 可删
  2) 分类下有商品 → 拒删
  3) **父分类的子分类下有商品 → 拒删**（级联删除会波及整棵子树，这条最关键）
  4) 空分类树 → 级联删除仍然正常（没被新校验误伤）
  5) 种子品牌/分类（被种子商品引用）→ 拒删
  6) 报错信息里带得上引用数量（运维看得懂）
"""
import json
import os
import sys
import urllib.error
import urllib.request

BASE = os.environ.get("JIFENG_BASE", "http://172.22.96.1:5173")
MARK = "DEREF_TEST"

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


def refused(j, code=400):
    """业务层拒绝：HTTP 200 + body 里 code"""
    return isinstance(j, dict) and j.get("code") == code


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
    token = j["data"]["token"] if ok(j) else None
    if not check("管理员登录", token is not None, f"HTTP {st}"):
        return summary()

    created = {"品牌": [], "分类": [], "商品": []}

    # ------------------------------------------------------------------
    print()
    print("=" * 78)
    print(" 第 1 部分：品牌 —— 有商品时拒删")
    print("=" * 78)

    st, j = call("POST", "/api/brands", token,
                 {"name": MARK + "_品牌", "company": MARK, "description": MARK})
    if not check("新增测试品牌", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}"):
        return summary()
    brand_id = j["data"]["id"]
    created["品牌"].append(brand_id)

    st, j = call("POST", "/api/goods", token,
                 {"spuNo": MARK + "_SPU1", "name": MARK + "_商品1", "categoryId": 1,
                  "brandId": brand_id, "markPrice": 100.00, "price": 50.00, "qty": 5})
    if not check("新增挂在测试品牌下的商品", st == 200 and ok(j), f"HTTP {st} {j.get('msg')}"):
        return summary()
    good_id = j["data"]["id"]
    created["商品"].append(good_id)

    st, j = call("DELETE", "/api/brands", token, [brand_id])
    check("品牌下有商品 → 拒删", refused(j), f"code={j.get('code')} msg={j.get('msg')}")
    check("  错误信息里带上了引用数量", "1" in str(j.get("msg")), f"msg={j.get('msg')}")

    st, j = call("DELETE", "/api/goods", token, [good_id])
    if ok(j):
        created["商品"].remove(good_id)
    check("清掉引用商品", ok(j), f"HTTP {st}")

    st, j = call("DELETE", "/api/brands", token, [brand_id])
    check("商品清掉后 → 可以删除", ok(j), f"HTTP {st} {j.get('msg')}")
    if ok(j):
        created["品牌"].remove(brand_id)

    # ------------------------------------------------------------------
    print()
    print("=" * 78)
    print(" 第 2 部分：分类 —— 子分类有商品时父分类也不能删（级联会波及）")
    print("=" * 78)

    st, j = call("POST", "/api/categories", token,
                 {"parentId": 0, "name": MARK + "_父类", "sort": 950})
    parent_id = j["data"]["id"] if ok(j) else None
    if not check("新增父分类", parent_id is not None, f"HTTP {st}"):
        return summary()
    created["分类"].append(parent_id)

    st, j = call("POST", "/api/categories", token,
                 {"parentId": parent_id, "name": MARK + "_子类", "sort": 951})
    child_id = j["data"]["id"] if ok(j) else None
    check("新增子分类", child_id is not None, f"HTTP {st}")

    st, j = call("POST", "/api/goods", token,
                 {"spuNo": MARK + "_SPU2", "name": MARK + "_商品2", "categoryId": child_id,
                  "brandId": 1, "markPrice": 100.00, "price": 50.00, "qty": 5})
    good2 = j["data"]["id"] if ok(j) else None
    if not check("新增挂在**子分类**下的商品", good2 is not None, f"HTTP {st} {j.get('msg')}"):
        return summary()
    created["商品"].append(good2)

    st, j = call("DELETE", "/api/categories", token, [parent_id])
    check("★ 父分类的子分类有商品 → 拒删父分类",
          refused(j), f"code={j.get('code')} msg={j.get('msg')}")
    check("  错误信息里指出了是哪个分类", "子类" in str(j.get("msg")), f"msg={j.get('msg')}")

    st, j = call("DELETE", "/api/categories", token, [child_id])
    check("  子分类自己有商品 → 同样拒删", refused(j),
          f"code={j.get('code')} msg={j.get('msg')}")

    st, j = call("DELETE", "/api/goods", token, [good2])
    if ok(j):
        created["商品"].remove(good2)

    st, j = call("DELETE", "/api/categories", token, [parent_id])
    check("商品清掉后 → 父分类可删（级联仍然工作）", ok(j), f"HTTP {st} {j.get('msg')}")
    if ok(j):
        created["分类"].remove(parent_id)
        st, j2 = call("GET", f"/api/categories/id/{child_id}", token)
        check("  子分类被级联删掉", not (ok(j2) and j2.get("data")))

    # ------------------------------------------------------------------
    print()
    print("=" * 78)
    print(" 第 3 部分：种子品牌/分类被种子商品引用，不能删")
    print("=" * 78)

    st, j = call("DELETE", "/api/brands", token, [1])
    check("种子品牌 id=1（iPhone 挂着）→ 拒删", refused(j),
          f"code={j.get('code')} msg={j.get('msg')}")

    st, j = call("DELETE", "/api/categories", token, [1])
    check("种子分类 id=1（两个种子商品都挂着）→ 拒删", refused(j),
          f"code={j.get('code')} msg={j.get('msg')}")

    # 确认拒绝之后数据还在（没有「报错了但已经删了一半」）
    st, j = call("GET", "/api/brands/id/1")
    check("  被拒后品牌仍然存在", ok(j) and j.get("data"), f"data={bool(j.get('data'))}")
    st, j = call("GET", "/api/goods/id/1")
    check("  被拒后商品仍然存在", ok(j) and j.get("data"))

    # ------------------------------------------------------------------
    print()
    print("=" * 78)
    print(" 第 4 部分：清理")
    print("=" * 78)

    for g in list(created["商品"]):
        call("DELETE", "/api/goods", token, [g])
    if created["分类"]:
        call("DELETE", "/api/categories", token, created["分类"])
    for b in list(created["品牌"]):
        call("DELETE", "/api/brands", token, [b])

    st, j = call("GET", "/api/brands")
    left_b = [r for r in (j.get("data") or {}).get("list", []) if MARK in str(r.get("name", ""))]
    st, j = call("GET", "/api/categories/tree")
    def has_mark(nodes):
        for n in nodes or []:
            if MARK in str(n.get("name", "")) or has_mark(n.get("children")):
                return True
        return False
    st, j = call("GET", "/api/categories", None)
    check("测试品牌已清理", len(left_b) == 0, f"残留 {len(left_b)}")
    st, j = call("GET", "/api/goods", None)
    left_g = [r for r in (j.get("data") or {}).get("list", []) if MARK in str(r.get("name", ""))]
    check("测试商品已清理", len(left_g) == 0, f"残留 {len(left_g)}")

    return summary()


if __name__ == "__main__":
    sys.exit(0 if main() else 1)
