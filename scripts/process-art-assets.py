#!/usr/bin/env python3
"""Prepare the concept artwork for high-density Android rendering."""
from collections import deque
from pathlib import Path
import sys
from PIL import Image, ImageFilter, ImageOps

out_dir = Path(sys.argv[1] if len(sys.argv) > 1 else "app/src/main/res/drawable-nodpi")

def upscale_rgb(path: Path, target_width: int, quality: int) -> None:
    image = Image.open(path).convert("RGB")
    width, height = image.size
    target_height = max(1, round(height * target_width / width))
    image = image.resize((target_width, target_height), Image.Resampling.LANCZOS)
    image = image.filter(ImageFilter.UnsharpMask(radius=1.25, percent=85, threshold=3))
    image.save(path, "WEBP", quality=quality, method=6)
    print(f"Upscaled {path.name}: {width}x{height} -> {target_width}x{target_height} ({path.stat().st_size} bytes)")

def clean_and_upscale_icon(path: Path) -> None:
    source = Image.open(path).convert("RGB")
    width, height = source.size
    pixels = list(source.getdata())

    # Detect the near-neutral bright gray/white checkerboard only. Colored
    # magenta/cyan glass reflections and the character's saturated outline stay.
    candidates = bytearray(width * height)
    for i, (r, g, b) in enumerate(pixels):
        if max(r, g, b) - min(r, g, b) <= 14 and min(r, g, b) >= 205:
            candidates[i] = 1

    background = bytearray(width * height)
    queue = deque()

    def add(x: int, y: int) -> None:
        i = y * width + x
        if candidates[i] and not background[i]:
            background[i] = 1
            queue.append((x, y))

    for x in range(width):
        add(x, 0)
        add(x, height - 1)
    for y in range(height):
        add(0, y)
        add(width - 1, y)

    while queue:
        x, y = queue.popleft()
        for ny in range(max(0, y - 1), min(height, y + 2)):
            for nx in range(max(0, x - 1), min(width, x + 2)):
                add(nx, ny)

    alpha_values = [0 if background[i] else 255 for i in range(width * height)]
    alpha = Image.new("L", (width, height))
    alpha.putdata(alpha_values)
    # Grow the transparent background by one source pixel to suppress checkerboard halos.
    background_alpha = ImageOps.invert(alpha).filter(ImageFilter.MaxFilter(3))
    alpha = ImageOps.invert(background_alpha).filter(ImageFilter.GaussianBlur(0.35))
    rgba = source.convert("RGBA")
    rgba.putalpha(alpha)
    box = alpha.point(lambda a: 255 if a > 32 else 0).getbbox()
    if not box:
        raise RuntimeError("Mascot icon background removal produced an empty image")

    left, top, right, bottom = box
    pad = max(3, round(max(right - left, bottom - top) * 0.055))
    left = max(0, left - pad); top = max(0, top - pad)
    right = min(width, right + pad); bottom = min(height, bottom + pad)
    foreground = rgba.crop((left, top, right, bottom))

    # Square canvas and safe-zone margin prevent Android launcher mask clipping.
    side = max(foreground.width, foreground.height)
    square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    square.alpha_composite(foreground, ((side - foreground.width) // 2, (side - foreground.height) // 2))
    target = 768
    inner = square.resize((target, target), Image.Resampling.LANCZOS)
    inner = inner.filter(ImageFilter.UnsharpMask(radius=1.0, percent=65, threshold=2))
    inner.save(path, "WEBP", quality=88, method=6, alpha_quality=100)
    print(f"Cleaned/upscaled icon_art.webp: {width}x{height} -> {target}x{target} alpha WebP ({path.stat().st_size} bytes)")

hero_path = out_dir / "hero_art.webp"
splash_path = out_dir / "splash_art.webp"
icon_path = out_dir / "icon_art.webp"
for required in (hero_path, splash_path, icon_path):
    if not required.exists():
        raise FileNotFoundError(f"Missing assembled artwork: {required}")

upscale_rgb(hero_path, 1280, 84)
upscale_rgb(splash_path, 1080, 82)
clean_and_upscale_icon(icon_path)
