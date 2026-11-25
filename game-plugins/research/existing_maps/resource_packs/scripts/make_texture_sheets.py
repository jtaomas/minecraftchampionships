#!/usr/bin/env python3
"""Build labelled contact sheets from the extracted resource packs."""
import os
from PIL import Image, ImageDraw, ImageFont

HERE = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs"
CL = os.path.join(HERE, "packs", "clouds2", "extracted", "assets", "minecraft", "textures")
YE = os.path.join(HERE, "packs", "yeti_set_go", "extracted", "assets", "minecraft", "textures")
SP = os.path.join(HERE, "packs", "space_race", "extracted", "Space Race Resource Pack", "assets", "minecraft", "textures")
OUT_T = os.path.join(HERE, "textures")
OUT_F = os.path.join(HERE, "frames")
os.makedirs(OUT_T, exist_ok=True)
os.makedirs(OUT_F, exist_ok=True)

FONT = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 14)
FONT_S = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 11)
FONT_B = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 20)
BG = (24, 24, 28)
FG = (235, 235, 235)
MUT = (150, 150, 160)
ACC = (255, 190, 60)


def checker(size, cell=8):
    im = Image.new("RGB", size, (70, 70, 76))
    d = ImageDraw.Draw(im)
    for y in range(0, size[1], cell):
        for x in range(0, size[0], cell):
            if (x // cell + y // cell) % 2:
                d.rectangle([x, y, x + cell - 1, y + cell - 1], fill=(110, 110, 118))
    return im


def tile(img, scale, label, sub=""):
    w, h = img.size
    im = img.convert("RGBA").resize((w * scale, h * scale), Image.NEAREST)
    bg = checker((w * scale, h * scale), max(4, scale))
    bg.paste(im, (0, 0), im)
    return bg, label, sub


def sheet(tiles, cols, path, title, cell_w=None, cell_h=None, scale=8):
    pad = 12
    label_h = 34
    if cell_w is None:
        cell_w = max(t[0].width for t in tiles)
    if cell_h is None:
        cell_h = max(t[0].height for t in tiles)
    meas = ImageDraw.Draw(Image.new("RGB", (10, 10)))
    for _, label, _ in tiles:
        cell_w = max(cell_w, int(meas.textlength(label, font=FONT)) + 6)
    rows = (len(tiles) + cols - 1) // cols
    W = cols * (cell_w + pad) + pad
    H = 70 + rows * (cell_h + label_h + pad) + pad
    out = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(out)
    d.text((pad, 16), title, font=FONT_B, fill=ACC)
    for i, (img, label, sub) in enumerate(tiles):
        cx = pad + (i % cols) * (cell_w + pad)
        cy = 70 + (i // cols) * (cell_h + label_h + pad)
        out.paste(img, (cx + (cell_w - img.width) // 2, cy))
        d.text((cx, cy + cell_h + 3), label, font=FONT, fill=FG)
        if sub:
            d.text((cx, cy + cell_h + 19), sub, font=FONT_S, fill=MUT)
    out.save(path)
    print(path, out.size)


def block_sheet(folder, names, path, title, cols=6, scale=8):
    tiles = []
    for n in names:
        p = os.path.join(folder, "block", n + ".png")
        im = Image.open(p)
        w, h = im.size
        frames = h // 16 if h > 16 else 1
        # crop first frame for the overview
        first = im.crop((0, 0, 16, 16)) if h > 16 else im
        sub = f"{w}x{h}" + (f"  {frames}f" if frames > 1 else "")
        tiles.append(tile(first, scale, n.replace("_", " "), sub))
    sheet(tiles, cols, path, title, scale=scale)


def main():
    clouds_blocks = [
        "magenta_glazed_terracotta", "pink_glazed_terracotta", "purple_glazed_terracotta",
        "red_glazed_terracotta", "yellow_glazed_terracotta",
        "glowstone", "sea_lantern", "diorite", "oak_planks", "red_nether_bricks",
        "red_stained_glass", "soul_sand",
    ]
    block_sheet(CL, clouds_blocks, os.path.join(OUT_T, "sheet_clouds2_blocks.png"),
                "Clouds 2 - overridden block textures (frame 0 of animated, 8x)", cols=6, scale=8)
    block_sheet(CL, ["black_terracotta", "blue_terracotta", "brown_terracotta", "cyan_terracotta",
                     "gray_terracotta", "green_terracotta", "light_blue_terracotta", "light_gray_terracotta",
                     "lime_terracotta", "magenta_terracotta", "orange_terracotta", "pink_terracotta",
                     "purple_terracotta", "red_terracotta", "white_terracotta", "yellow_terracotta"],
                os.path.join(OUT_T, "sheet_clouds2_terracotta.png"),
                "Clouds 2 - 16 flat terracotta team colours (8x)", cols=8, scale=8)
    block_sheet(SP, ["acacia_planks", "birch_planks", "birch_trapdoor", "crimson_planks", "dark_oak_planks",
                     "jungle_planks", "blackstone", "blackstone_top", "stone_bricks", "smooth_stone",
                     "diamond_block", "end_stone", "glass", "iron_bars", "sea_lantern",
                     "black_wool", "gray_wool", "light_gray_wool", "orange_wool", "yellow_wool",
                     "red_concrete", "light_blue_stained_glass", "red_stained_glass", "white_stained_glass",
                     "magenta_glazed_terracotta"],
                os.path.join(OUT_T, "sheet_space_race_blocks.png"),
                "Space Race - overridden block textures (8x)", cols=7, scale=8)
    block_sheet(YE, ["magenta_glazed_terracotta", "pink_glazed_terracotta", "purple_glazed_terracotta",
                     "red_glazed_terracotta", "yellow_glazed_terracotta", "glowstone"],
                os.path.join(OUT_T, "sheet_yeti_blocks.png"),
                "Yeti Set Go - overridden block textures (frame 0, 8x)", cols=6, scale=8)

    # animated strips: frame-by-frame
    anim = [
        ("magenta_glazed_terracotta", 7), ("pink_glazed_terracotta", 7),
        ("purple_glazed_terracotta", 7), ("red_glazed_terracotta", 7),
        ("yellow_glazed_terracotta", 16), ("sea_lantern", 5),
    ]
    scale = 6
    row_h = 16 * scale + 6
    W = 12 + 250 + 16 * (16 * scale + 4) + 30
    H = 60 + len(anim) * row_h + 20
    out = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(out)
    d.text((12, 14), "Clouds 2 animation strips, every frame (6x) - top row = frame 0", font=FONT_B, fill=ACC)
    for r, (name, frames) in enumerate(anim):
        im = Image.open(os.path.join(CL, "block", name + ".png")).convert("RGBA")
        y = 60 + r * row_h
        d.text((12, y + 40), name.replace("_", " "), font=FONT_S, fill=FG)
        for f in range(frames):
            fr = im.crop((0, f * 16, 16, f * 16 + 16)).resize((16 * scale, 16 * scale), Image.NEAREST)
            bg = checker((16 * scale, 16 * scale), 6)
            bg.paste(fr, (0, 0), fr)
            x = 250 + f * (16 * scale + 4)
            out.paste(bg, (x, y))
            d.text((x + 2, y + 16 * scale + 0), str(f), font=FONT_S, fill=MUT)
    out.save(os.path.join(OUT_T, "sheet_clouds2_animation_frames.png"))
    print("anim", out.size)

    # rotten flesh 2 frames
    im = Image.open(os.path.join(CL, "item", "rotten_flesh.png")).convert("RGBA")
    tiles = []
    for f in range(2):
        fr = im.crop((f * 16, 0, f * 16 + 16, 16))
        tiles.append(tile(fr, 12, f"frame {f}", "16x16"))
    sheet(tiles, 2, os.path.join(OUT_T, "sheet_rotten_flesh_energy.png"),
          "rotten_flesh.png -> item.minecraft.rotten_flesh = 'Energy' (2 frames, 12x)", scale=12)

    # Space Race items
    items = []
    for n in ("carrot_on_a_stick", "acacia_sign"):
        im = Image.open(os.path.join(SP, "item", n + ".png"))
        items.append(tile(im, 12, n.replace("_", " "), "16x16"))
    sheet(items, 2, os.path.join(OUT_T, "sheet_space_race_items.png"),
          "Space Race - item textures (12x)", scale=12)

    # Space Race GUI
    gui = [
        ("widgets", 256), ("icons", 256), ("container/inventory", 256), ("recipe_book", 256),
        ("recipe_button", 256), ("book", 256), ("options_background", 16), ("accessibility", 32),
    ]
    tiles = []
    for n, src in gui:
        im = Image.open(os.path.join(SP, "gui", n + ".png"))
        scale = 2 if im.width >= 128 else 8
        im2 = im.convert("RGBA").resize((im.width * scale, im.height * scale), Image.NEAREST)
        bg = checker((im2.width, im2.height), 8)
        bg.paste(im2, (0, 0), im2)
        tiles.append((bg, n, f"{im.width}x{im.height} @{scale}x"))
    sheet(tiles, 3, os.path.join(OUT_T, "sheet_space_race_gui.png"),
          "Space Race - GUI overrides (5 of 8 are dead in 1.21.1)", scale=2)

    # pack icons
    icons = []
    for label, p in (("clouds2", os.path.join(CL, "..", "..", "..", "pack.png")),
                     ("yeti_set_go", os.path.join(YE, "..", "..", "..", "pack.png")),
                     ("space_race", os.path.join(SP, "..", "..", "..", "pack.png"))):
        im = Image.open(os.path.normpath(p)).convert("RGBA")
        bg = Image.new("RGBA", (im.width * 3, im.height * 3), (0, 0, 0, 0))
        bg.paste(im.resize((im.width * 3, im.height * 3), Image.NEAREST), (0, 0), im.resize((im.width * 3, im.height * 3), Image.NEAREST))
        icons.append((bg, label, f"{im.width}x{im.height} @3x"))
    sheet(icons, 3, os.path.join(OUT_F, "pack_icons.png"), "pack.png icons", scale=3)


if __name__ == "__main__":
    main()
