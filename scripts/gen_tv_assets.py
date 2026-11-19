"""生成 Android TV 端图标资源（banner + 方形 launcher icon）。

设计依据：
- TV 端主色 金黄 #FFD700 / 橙 #FFA500（见 ui/theme/Color.kt 的 NasTVPrimaryGold）
- 背景 #0F0F0F，与手机端图标（深底 + 白色三角）形成区分
- 构图参考 huaguoshan/guoyuan 的 tv_banner：左侧大圆角块 + 右侧产品名

用法：python scripts/gen_tv_assets.py
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

SS = 4  # 超采样倍数，先大后缩保证边缘锐利

BG_TOP = (20, 20, 21)
BG_BOTTOM = (9, 9, 10)
GOLD_TOP = (255, 215, 0)
GOLD_BOTTOM = (255, 165, 0)
TEXT_COLOR = (245, 245, 245)
TRIANGLE_COLOR = (13, 13, 14)

FONT_BOLD = "C:/Windows/Fonts/segoeuib.ttf"

RES = Path(__file__).resolve().parent.parent / "client-android/app/src/main/res"


def vertical_gradient(size, top, bottom):
    w, h = size
    strip = Image.new("RGB", (1, h))
    for y in range(h):
        t = y / max(1, h - 1)
        strip.putpixel((0, y), tuple(round(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return strip.resize((w, h), Image.BILINEAR)


def rounded_mask(size, radius):
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius=radius, fill=255)
    return mask


def make_block(size):
    """金黄渐变的圆角方块 + 深色 play 三角。"""
    gradient = vertical_gradient((size, size), GOLD_TOP, GOLD_BOTTOM)
    block = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    block.paste(gradient, (0, 0), rounded_mask((size, size), round(size * 0.255)))

    draw = ImageDraw.Draw(block)
    cx, cy = size * 0.525, size * 0.5
    h = size * 0.34
    w = h * 0.88
    draw.polygon(
        [(cx - w / 2, cy - h / 2), (cx - w / 2, cy + h / 2), (cx + w / 2, cy)],
        fill=TRIANGLE_COLOR,
    )
    return block


def compose_banner(width, height, label="NasTV"):
    W, H = width * SS, height * SS
    canvas = vertical_gradient((W, H), BG_TOP, BG_BOTTOM).convert("RGBA")

    block_size = round(H * 0.52)
    bx = round(W * 0.085)
    by = (H - block_size) // 2
    radius = round(block_size * 0.255)

    # 方块投影，让块面从黑底上浮起来
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        [bx + round(4 * SS), by + round(8 * SS), bx + block_size + round(4 * SS), by + block_size + round(8 * SS)],
        radius=radius,
        fill=(0, 0, 0, 150),
    )
    canvas = Image.alpha_composite(canvas, shadow.filter(ImageFilter.GaussianBlur(round(9 * SS))))
    canvas.alpha_composite(make_block(block_size), (bx, by))

    # 产品名：块右侧居中，自动缩放到安全区内
    left = bx + block_size + round(W * 0.055)
    max_w = W - round(W * 0.06) - left
    size = round(H * 0.30)
    while True:
        font = ImageFont.truetype(FONT_BOLD, size)
        box = font.getbbox(label)
        text_w, text_h = box[2] - box[0], box[3] - box[1]
        if text_w <= max_w or size <= 12:
            break
        size -= 2
    ImageDraw.Draw(canvas).text(
        (left, (H - text_h) / 2 - box[1]),
        label,
        font=font,
        fill=TEXT_COLOR,
    )
    return canvas.resize((width, height), Image.LANCZOS)


def compose_icon(px):
    S = px * SS
    canvas = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    pad = round(S * 0.06)
    canvas.alpha_composite(make_block(S - 2 * pad), (pad, pad))
    return canvas.resize((px, px), Image.LANCZOS)


def main():
    banners = {"mdpi": (320, 180), "hdpi": (480, 270), "xhdpi": (640, 360)}
    for density, size in banners.items():
        out = RES / f"drawable-{density}/tv_banner.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        compose_banner(*size).save(out, optimize=True)
        print(f"banner {density:6s} {size[0]}x{size[1]} -> {out.relative_to(RES)}")

    icons = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for density, px in icons.items():
        out = RES / f"mipmap-television-{density}/ic_launcher.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        compose_icon(px).save(out, optimize=True)
        print(f"icon   {density:6s} {px}x{px} -> {out.relative_to(RES)}")


if __name__ == "__main__":
    main()
