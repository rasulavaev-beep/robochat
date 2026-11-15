import glob, os
import numpy as np
from PIL import Image, ImageDraw

OUT = "app/src/main/res/drawable-nodpi"
os.makedirs(OUT, exist_ok=True)
imgs = []
for p in sorted(glob.glob("file_*")):
    try:
        imgs.append((p, Image.open(p).convert("RGB")))
    except Exception:
        pass
print("картинки:", [(p[:20], im.size) for p, im in imgs])
if len(imgs) < 2:
    raise SystemExit("нужно 2 картинки file_* в корне репозитория")
sq = min(imgs, key=lambda t: abs(t[1].width / t[1].height - 1))
rest = [t for t in imgs if t[0] != sq[0]]
wide = max(rest, key=lambda t: t[1].width / t[1].height)

im = sq[1]
a = np.array(im).astype(int)
ys, xs = np.where((a[:, :, 0] > 150) & (a[:, :, 1] < 90) & (a[:, :, 2] < 100))
if len(xs) > 100:
    cx, cy = (xs.min() + xs.max()) / 2, (ys.min() + ys.max()) / 2
    r = max(xs.max() - xs.min(), ys.max() - ys.min()) / 2
else:
    cx, cy, r = im.width / 2, im.height / 2, min(im.size) / 2
c = im.crop((int(cx - r) - 2, int(cy - r) - 2, int(cx + r) + 3, int(cy + r) + 3))
S = 4
mask = Image.new("L", (c.width * S, c.height * S), 0)
ImageDraw.Draw(mask).ellipse((2 * S, 2 * S, (c.width - 2) * S, (c.height - 2) * S), fill=255)
c = c.convert("RGBA")
c.putalpha(mask.resize(c.size, Image.LANCZOS))
c.resize((160, 160), Image.LANCZOS).save(OUT + "/bubble.png", optimize=True)

def to_alpha(img):
    b = np.array(img.convert("RGB")).astype(float)
    al = np.clip((b.max(axis=2) - 6) * 255 / 249, 0, 255)
    col = np.where(al[..., None] > 0, np.clip(b * 255 / np.maximum(al[..., None], 1), 0, 255), 0)
    return Image.fromarray(np.dstack([col, al]).astype(np.uint8), "RGBA")

w, h = wide[1].size
if abs(w / h - 1672 / 941) < 0.05:
    art = to_alpha(wide[1].crop((int(w * 880 / 1672), int(h * 150 / 941), int(w * 1470 / 1672), int(h * 500 / 941))))
else:
    art = to_alpha(wide[1])
    bb = art.getchannel("A").point(lambda v: 255 if v > 20 else 0).getbbox()
    if bb:
        art = art.crop(bb)
art.thumbnail((590, 350), Image.LANCZOS)
canvas = Image.new("RGBA", (590, 350), (0, 0, 0, 0))
canvas.alpha_composite(art, ((590 - art.width) // 2, (350 - art.height) // 2))
canvas.save(OUT + "/logo.png", optimize=True)
print("готово: bubble.png, logo.png")
