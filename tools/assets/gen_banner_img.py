#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成首页轮播图，落到 d:/shoplook2026/upload/banner/。

种子数据（docs/sql/jifeng-mall-init.sql 第 23 节）把三条轮播的 image_url 指向
  /upload/banner/banner-1.png  ...  banner-3.png
这三个文件不在仓库里（图片是二进制资源，不进仓库），需要生成本地文件。

设计说明：图上**不写文字**——纯标准库里没有中文字体，硬画只会很难看。
标题/副标题由前端 Home.vue 用 HTML 叠在图上，所以这里只画「背景美术」：
对角渐变 + 两团柔光 + 斜向细纹 + 暗角。观感是抽象背景板，不是"示意图"。

纯标准库实现（zlib + struct 手写 PNG），2 倍超采样抗锯齿。
"""
import struct
import zlib

W, H = 1200, 420     # 输出尺寸（宽幅，配合前端 360px 高的轮播容器）
SS = 2               # 超采样倍数
W2, H2 = W * SS, H * SS
OUT_DIR = "/mnt/d/shoplook2026/upload/banner"

# 三条轮播的配色：起点色、终点色、两团柔光的颜色
THEMES = [
    ((24, 36, 92), (86, 46, 140), (110, 150, 255), (190, 120, 255)),   # 1 新品首发：深蓝 → 紫
    ((146, 52, 24), (206, 92, 30), (255, 176, 96), (255, 110, 60)),    # 2 手机专享：暖橙 → 红
    ((108, 16, 40), (176, 28, 52), (255, 120, 120), (255, 190, 120)),  # 3 限时秒杀：暗红 → 朱红
]


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


def render(theme):
    """按主题渲一张 W2×H2 的图，返回降采样后的 RGB bytearray"""
    from math import hypot

    (r0, g0, b0), (r1, g1, b1), (gr, gg, gb), (hr, hg, hb) = theme

    # 两团柔光：位置、半径、强度（都是相对尺寸，换分辨率不用改）
    glows = [
        (W2 * 0.20, H2 * 0.28, W2 * 0.40, 0.60, gr, gg, gb),
        (W2 * 0.86, H2 * 0.70, W2 * 0.46, 0.55, hr, hg, hb),
    ]
    glows = [(cx, cy, r * r, k, cr, cg, cb) for cx, cy, r, k, cr, cg, cb in glows]

    # 同心圆环：整张图的视觉主体，放在右侧（左侧留给前端叠的标题文字）
    ring_cx, ring_cy = W2 * 0.80, H2 * 0.50
    rings = []
    for n in range(5):
        rr = W2 * (0.085 + n * 0.062)
        rings.append((rr, W2 * 0.0040))          # 半径、线宽
    rings.append((W2 * 0.470, W2 * 0.0026))      # 最外一圈细的
    ring_peak = 0.5                                  # 环的峰值不透明度

    # 右下角的斜向亮带：一条被两条平行线夹住的柔光，给画面一点方向感
    band_a, band_b = -0.62, W2 * 0.10                # 斜率、带宽
    band_base = H2 * 1.05

    cx0, cy0 = W2 / 2.0, H2 / 2.0
    inv_max = 1.0 / (cx0 * cx0 + cy0 * cy0)

    big = bytearray(W2 * H2 * 3)
    i = 0
    for y in range(H2):
        fy = y / (H2 - 1.0)
        for x in range(W2):
            fx = x / (W2 - 1.0)
            # 对角渐变：左上加右下
            t = (fx * 0.62 + fy * 0.38)
            r = r0 + (r1 - r0) * t
            g = g0 + (g1 - g0) * t
            b = b0 + (b1 - b0) * t

            for gx, gy, r2, k, cr, cg, cb in glows:
                dx = x - gx
                dy = y - gy
                d2 = dx * dx + dy * dy
                if d2 < r2:
                    f = 1.0 - d2 / r2
                    f = f * f * k            # 平方衰减，边缘更柔
                    r += (cr - r) * f
                    g += (cg - g) * f
                    b += (cb - b) * f

            # 斜向亮带：到直线的距离在带宽内就给一点提亮，越靠中心越亮
            dband = (y - band_base - band_a * x)
            aband = dband if dband >= 0 else -dband
            if aband < band_b:
                f = 1.0 - aband / band_b
                f = f * f * 0.16
                r += (255 - r) * f
                g += (255 - g) * f
                b += (255 - b) * f

            # 同心圆环：先算到圆心的距离，落在环带内就混白色，边缘做线性羽化
            rx = x - ring_cx
            ry = y - ring_cy
            d = hypot(rx, ry)
            for rr, hw in rings:
                dd = d - rr
                if dd < 0:
                    dd = -dd
                if dd < hw:
                    f = (1.0 - dd / hw) * ring_peak
                    f = f * f
                    r += (255 - r) * f
                    g += (255 - g) * f
                    b += (255 - b) * f

            # 暗角：四角压暗，让上层的 HTML 文字更跳
            vx = x - cx0
            vy = (y - cy0) * 1.55            # 宽幅图纵向压得狠一点
            v = 1.0 - 0.30 * (vx * vx + vy * vy) * inv_max
            r *= v
            g *= v
            b *= v

            big[i] = 0 if r < 0 else (255 if r > 255 else int(r))
            big[i + 1] = 0 if g < 0 else (255 if g > 255 else int(g))
            big[i + 2] = 0 if b < 0 else (255 if b > 255 else int(b))
            i += 3

    # 2x2 降采样
    out = bytearray(W * H * 3)
    o = 0
    for y in range(H):
        row0 = (y * 2) * W2 * 3
        row1 = row0 + W2 * 3
        for x in range(W):
            c = x * 2 * 3
            for ch in range(3):
                s = (big[row0 + c + ch] + big[row0 + c + 3 + ch]
                     + big[row1 + c + ch] + big[row1 + c + 3 + ch])
                out[o] = s >> 2
                o += 1
    return out


def main():
    import os
    os.makedirs(OUT_DIR, exist_ok=True)
    for n, theme in enumerate(THEMES, start=1):
        path = os.path.join(OUT_DIR, f"banner-{n}.png")
        buf = render(theme)
        write_png(path, W, H, buf)
        print(f"{path}  {W}x{H}  {os.path.getsize(path)} bytes")


if __name__ == "__main__":
    main()
