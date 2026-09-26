#!/usr/bin/env python3
"""Generates Chase's Retreat icon assets from the official logo artwork.

Source: `scripts/assets/chases-retreat-logo-source.png` — a transparent-background
PNG where every opaque pixel is pure black line art (the husky badge), meant to be
composited onto any background color. Renders that art at every size the Android
and iOS icon sets need, plus the standalone in-app logo used on the auto-login
splash screen.

Run once from the repo root: `python3 scripts/generate_icons.py`. Not part of the
app's build — its only output is the checked-in PNG/WEBP files below.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
LOGO_SOURCE = ROOT / "scripts/assets/chases-retreat-logo-source.png"

WHITE = (255, 255, 255, 255)


def load_logo() -> Image.Image:
    return Image.open(LOGO_SOURCE).convert("RGBA")


def composite_on_white(logo: Image.Image, size: int, scale: float) -> Image.Image:
    """White square canvas with the logo centered at `scale` fraction of size."""
    canvas = Image.new("RGBA", (size, size), WHITE)
    art_size = round(size * scale)
    art = logo.resize((art_size, art_size), Image.LANCZOS)
    offset = ((size - art_size) // 2, (size - art_size) // 2)
    canvas.paste(art, offset, art)
    return canvas


def circle_crop(img: Image.Image) -> Image.Image:
    size = img.size[0]
    mask = Image.new("L", (size, size), 0)
    from PIL import ImageDraw
    ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def save_android_mipmaps(logo: Image.Image):
    densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    android_res = ROOT / "androidApp/src/main/res"
    for density, px in densities.items():
        square = composite_on_white(logo, px, scale=0.88)
        square.convert("RGB").save(android_res / f"mipmap-{density}/ic_launcher.webp", "WEBP", lossless=True)

        round_square = composite_on_white(logo, px, scale=0.92)
        round_icon = circle_crop(round_square)
        round_icon.save(android_res / f"mipmap-{density}/ic_launcher_round.webp", "WEBP", lossless=True)


def save_android_adaptive_foreground(logo: Image.Image):
    # Adaptive icon foreground: transparent canvas, art scaled to sit inside the
    # ~66% safe-zone circle so it survives circular/squircle/rounded-square launcher
    # masks without clipping the outer ring of the badge.
    size = 432  # 108dp @ xxxhdpi (4x), a common single-asset adaptive foreground size
    art_size = round(size * 0.66)
    art = logo.resize((art_size, art_size), Image.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    offset = ((size - art_size) // 2, (size - art_size) // 2)
    canvas.paste(art, offset, art)
    drawable_dir = ROOT / "androidApp/src/main/res/drawable"
    old_vector = drawable_dir / "ic_launcher_foreground.xml"
    if old_vector.exists():
        old_vector.unlink()
    canvas.save(drawable_dir / "ic_launcher_foreground.png")


def save_ios_iconset(logo: Image.Image):
    sizes = [16, 20, 24, 27, 29, 32, 33, 40, 48, 50, 54, 55, 57, 58, 60, 64, 66, 72, 76, 80,
             86, 87, 88, 92, 98, 100, 102, 108, 114, 117, 120, 128, 129, 144, 152, 167, 172,
             180, 196, 216, 234, 256, 258, 512, 1024]
    out = ROOT / "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"
    master = composite_on_white(logo, 1024, scale=0.88).convert("RGB")
    for px in sizes:
        master.resize((px, px), Image.LANCZOS).save(out / f"{px}.png")


def save_logo_mark(logo: Image.Image):
    # In-app splash logo: white disc backing so the black line art stays legible
    # against the app's surface color in both light and dark theme.
    size = 256
    disc = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    from PIL import ImageDraw
    ImageDraw.Draw(disc).ellipse([0, 0, size - 1, size - 1], fill=WHITE)
    art = logo.resize((round(size * 0.92), round(size * 0.92)), Image.LANCZOS)
    offset = ((size - art.size[0]) // 2, (size - art.size[1]) // 2)
    disc.paste(art, offset, art)
    out = ROOT / "composeApp/src/commonMain/composeResources/drawable/mass.webp"
    disc.save(out, "WEBP", lossless=True)


if __name__ == "__main__":
    logo = load_logo()
    save_android_mipmaps(logo)
    save_android_adaptive_foreground(logo)
    save_ios_iconset(logo)
    save_logo_mark(logo)
    print("Icons generated.")
