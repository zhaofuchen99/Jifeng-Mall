#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成种子数据里缺的两张商品图。

背景：docs/sql/jifeng-mall-init.sql 的演示商品把 pic 写成了
  /upload/good/iphone.png、/upload/good/mate.png，
但 d:/shoplook2026/upload/good/ 目录里根本没有这两个文件（项目里也没有），
所以商城的商品图一直是破的。这里直接把图生成到上传目录，不动数据库。

纯标准库实现（zlib + struct 手写 PNG），因为环境里没有 PIL / ImageMagick。
2 倍超采样后降采样做抗锯齿。
"""
import math
import os
import struct
import zlib

W = H = 800          # 输出尺寸
SS = 2               # 超采样倍数
W2, H2 = W * SS, H * SS
OUT_DIR = "/mnt/d/shoplook2026/upload/good"


def write_png(path, w, h, buf):
    """buf: bytearray，长度 w*h*3，RGB"""
    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    raw = bytearray()
    stride = w * 3
    for y in range(h):
        raw.append(0)                      # filter type 0
        raw += buf[y * stride:(y + 1) * stride]

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


def lerp(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def rr_inside(px, py, x, y, w, h, r):
    """点是否在圆角矩形内"""
    if px < x or px > x + w or py < y or py > y + h:
        return False
    cx = min(max(px, x + r), x + w - r)
    cy = min(max(py, y + r), y + h - r)
    dx, dy = px - cx, py - cy
    return dx * dx + dy * dy <= r * r


def circle_inside(px, py, cx, cy, r):
    dx, dy = px - cx, py - cy
    return dx * dx + dy * dy <= r * r


def render(bg_top, bg_bot, body_top, body_bot, screen_top, screen_bot,
           accent, style):
    """画一台手机。style: 'front' 正面 / 'back' 背面"""
    fw, fh = W2, H2
    buf = bytearray(fw * fh * 3)

    # --- 几何参数（按超采样后的画布算） ---
    pw, ph = int(fw * 0.42), int(fh * 0.80)          # 机身尺寸
    px0 = (fw - pw) // 2
    py0 = (fh - ph) // 2
    radius = int(pw * 0.13)
    bezel = int(pw * 0.045)

    # 机身下的椭圆投影
    sh_cx, sh_cy = fw / 2, py0 + ph + fh * 0.035
    sh_rx, sh_ry = pw * 0.62, fh * 0.028

    for y in range(fh):
        ty = y / fh
        bg = lerp(bg_top, bg_bot, ty)
        for x in range(fw):
            c = bg

            # 投影：靠中心越近越深
            dx = (x - sh_cx) / sh_rx
            dy = (y - sh_cy) / sh_ry
            d = dx * dx + dy * dy
            if d < 1.0:
                k = (1.0 - d) ** 1.5 * 0.22
                c = lerp(c, (30, 34, 42), k)

            # 机身
            if rr_inside(x, y, px0, py0, pw, ph, radius):
                # 机身本身有竖直渐变
                ty2 = (y - py0) / ph
                body = lerp(body_top, body_bot, ty2)
                # 左右边缘做点明暗，像金属边框受光
                edge = (x - px0) / pw
                if edge < 0.06:
                    body = lerp(body, (255, 255, 255), (0.06 - edge) * 3.2)
                elif edge > 0.94:
                    body = lerp(body, (0, 0, 0), (edge - 0.94) * 3.2)
                c = body

                # 屏幕（正面）
                if style == "front":
                    sx, sy = px0 + bezel, py0 + bezel
                    sw, sh = pw - bezel * 2, ph - bezel * 2
                    if rr_inside(x, y, sx, sy, sw, sh, radius - bezel):
                        c = lerp(screen_top, screen_bot, (y - sy) / sh)
                        # 斜向高光：在带内线性淡入淡出，否则会看到两条硬边
                        g = ((x - sx) / sw) + ((y - sy) / sh)
                        if 0.60 < g < 1.40:
                            c = lerp(c, (255, 255, 255),
                                     (1.0 - abs(g - 1.0) / 0.40) * 0.20)
                        # 刘海
                        nw, nh = sw * 0.42, sh * 0.030
                        if rr_inside(x, y, sx + (sw - nw) / 2, sy + bezel * 0.5,
                                     nw, nh, nh / 2):
                            c = lerp(c, (10, 12, 16), 0.95)

                # 背面：圆形摄像头模组 + 三摄
                if style == "back":
                    mcx, mcy = px0 + pw / 2, py0 + ph * 0.215
                    mr = pw * 0.27
                    if circle_inside(x, y, mcx, mcy, mr):
                        # 模组：外圈金属环 + 内圈深色玻璃
                        c = lerp(accent, (16, 20, 24), 0.25)
                        if circle_inside(x, y, mcx, mcy, mr * 0.87):
                            c = lerp(accent, (16, 20, 24), 0.68)

                        # 三颗镜头：半径 0.22*mr、离圆心 0.42*mr，相邻间距 0.73*mr
                        # 远大于直径 0.44*mr，所以不会互相重叠
                        lr = mr * 0.22
                        for ang in (210, 330, 90):
                            lx = mcx + math.cos(math.radians(ang)) * mr * 0.42
                            ly = mcy + math.sin(math.radians(ang)) * mr * 0.42
                            if circle_inside(x, y, lx, ly, lr):
                                # 镜头金属圈
                                c = lerp(accent, (255, 255, 255), 0.42)
                                if circle_inside(x, y, lx, ly, lr * 0.74):
                                    # 镜片：中心亮、边缘暗，像玻璃反光
                                    dist = math.hypot(x - lx, y - ly) / (lr * 0.74)
                                    c = lerp((74, 90, 110), (8, 10, 14), dist ** 0.7)

            buf[(y * fw + x) * 3:(y * fw + x) * 3 + 3] = bytes(
                int(max(0, min(255, v))) for v in c)

    # --- 降采样 ---
    out = bytearray(W * H * 3)
    for y in range(H):
        for x in range(W):
            r = g = b = 0
            for dy in range(SS):
                base = ((y * SS + dy) * fw + x * SS) * 3
                for dx in range(SS):
                    o = base + dx * 3
                    r += buf[o]
                    g += buf[o + 1]
                    b += buf[o + 2]
            n = SS * SS
            o = (y * W + x) * 3
            out[o] = r // n
            out[o + 1] = g // n
            out[o + 2] = b // n
    return out


def main():
    os.makedirs(OUT_DIR, exist_ok=True)

    print("生成 iphone.png ...")
    buf = render(
        bg_top=(250, 251, 253), bg_bot=(226, 230, 237),
        body_top=(78, 84, 94), body_bot=(38, 42, 50),
        screen_top=(26, 38, 56), screen_bot=(58, 96, 140),
        accent=(120, 128, 140), style="front")
    write_png(os.path.join(OUT_DIR, "iphone.png"), W, H, buf)

    print("生成 mate.png ...")
    buf = render(
        bg_top=(249, 251, 251), bg_bot=(222, 231, 232),
        body_top=(52, 84, 88), body_bot=(22, 40, 44),
        screen_top=(20, 44, 46), screen_bot=(46, 108, 104),
        accent=(196, 168, 110), style="back")
    write_png(os.path.join(OUT_DIR, "mate.png"), W, H, buf)

    for f in ("iphone.png", "mate.png"):
        p = os.path.join(OUT_DIR, f)
        print(f"  {p}  {os.path.getsize(p)} bytes")


if __name__ == "__main__":
    main()
