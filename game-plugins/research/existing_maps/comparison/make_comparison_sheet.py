"""Compile the cross-map comparison sheet from existing report images.

Research-only helper: reads PNGs produced by A1-A6c, A9, A5.
It does not touch a world, a map, a pack or the server.
Run from this folder:  python make_comparison_sheet.py
"""

from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = Path(__file__).resolve().parent / "renders" / "comparison_sheet.png"

W, H = 2000, 2460
BG = (15, 17, 22)
CARD = (24, 27, 34)
INK = (236, 239, 244)
MUTED = (158, 167, 180)
ACCENT = (255, 196, 84)
LINE = (52, 58, 70)

PANELS = [
    (
        "1  CLOUDS 2  —  ROUTE GRAMMAR",
        "What to take: animated chevron flow field, one silhouette per act, dense checkpoints.",
        "clouds2/renders/route_labeled.png",
    ),
    (
        "2  CITY MARATHON  —  URBAN PALETTE + SKYLINE",
        "What to take: road hierarchy, night light pools on the racing line, one station hero.",
        "city_marathon/frames/01_overview_route.png",
    ),
    (
        "3  SPACE RACE  —  VERTICALITY",
        "What to take: tuned lift-to-ring maths, chain-of-islands skyline, ring as landmark + grant.",
        "space_race/renders/route_overlay.png",
    ),
    (
        "4  YETI SET GO  —  COMPACTNESS",
        "What to take: 28.5k blocks, two-line lap, theme unity, 0.1 MB animation-only pack.",
        "yeti_set_go/renders/route_overlay.png",
    ),
    (
        "5  SKY BATTLE  —  TEAM IDENTITY + RESET",
        "What to take: four gift-box pods, fixed loot, traffic-light cages, palette-scoped building.",
        "minigames/sky_battle/frames/01_top_labelled.png",
    ),
    (
        "6  PARKOUR TAG  —  ROLE PADS + TIMER",
        "What to take: role pads, 60 s notched bossbar, opposed doors, red-to-lime start theatre.",
        "minigames/parkour_tag/frames/mg_pt_route_schematic.png",
    ),
    (
        "7  TGTTOS BADLANDS  —  JOURNEY + FINISH",
        "What to take: one landmark per leg, finish as a place, barrier pen, 1-block bridge economy.",
        "tgttos_badlands/renders/route_top_annotated.png",
    ),
    (
        "8  DECISION DOME  —  WHAT NOT TO SCALE",
        "Do not copy: display model, 0 seats, 42-deep unrailed pit, dial invisible from the terrace.",
        "decision_dome_recreation/renders/plan_labelled.png",
    ),
]


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = [
        r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf",
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf",
    ]
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def fit(img: Image.Image, box_w: int, box_h: int) -> Image.Image:
    img = img.convert("RGB")
    scale = min(box_w / img.width, box_h / img.height)
    size = (max(1, int(img.width * scale)), max(1, int(img.height * scale)))
    return img.resize(size, Image.LANCZOS)


def autocrop(img: Image.Image, pad: int = 18) -> Image.Image:
    """Trim a uniform render margin so the diagram fills more of the panel."""
    rgb = img.convert("RGB")
    bg = Image.new("RGB", rgb.size, rgb.getpixel((0, 0)))
    diff = ImageChops.difference(rgb, bg).convert("L").point(lambda v: 255 if v > 14 else 0)
    box = diff.getbbox()
    if not box:
        return rgb
    x0, y0, x1, y1 = box
    return rgb.crop(
        (
            max(0, x0 - pad),
            max(0, y0 - pad),
            min(rgb.width, x1 + pad),
            min(rgb.height, y1 + pad),
        )
    )


def main() -> None:
    canvas = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(canvas)

    d.text((54, 42), "EXISTING ACE RACE MAPS — WHAT EACH ONE CONTRIBUTES", font=font(46, True), fill=INK)
    d.text(
        (56, 102),
        "Cross-map synthesis for a premium, MCC-faithful 40-player Ace Race. "
        "Design lessons, not block-for-block copies.",
        font=font(23),
        fill=MUTED,
    )
    d.text(
        (56, 140),
        "All panels are existing report renders; no map was re-rendered. Scales and render styles differ — compare ideas, not metres.",
        font=font(19),
        fill=MUTED,
    )
    d.line([(54, 178), (W - 54, 178)], fill=LINE, width=2)

    margin_x, gap_x = 44, 24
    top = 198
    cols, rows = 2, 4
    card_w = (W - 2 * margin_x - gap_x) // cols
    card_h = 535
    img_h = 402

    for i, (title, sub, rel) in enumerate(PANELS):
        col, row = i % cols, i // cols
        x = margin_x + col * (card_w + gap_x)
        y = top + row * (card_h + 18)
        d.rounded_rectangle([x, y, x + card_w, y + card_h], radius=12, fill=CARD, outline=LINE, width=1)

        path = ROOT / rel
        if path.exists():
            with Image.open(path) as src:
                thumb = fit(autocrop(src), card_w - 28, img_h)
            px = x + (card_w - thumb.width) // 2
            py = y + 14 + (img_h - thumb.height) // 2
            canvas.paste(thumb, (px, py))
        else:
            d.text((x + 18, y + 40), f"MISSING: {rel}", font=font(18), fill=(255, 120, 120))

        d.text((x + 16, y + img_h + 24), title, font=font(24, True), fill=ACCENT)
        # wrap the takeaway line to the card width
        words, line, lines = sub.split(), "", []
        for word in words:
            trial = (line + " " + word).strip()
            if d.textlength(trial, font=font(18)) <= card_w - 32:
                line = trial
            else:
                lines.append(line)
                line = word
        if line:
            lines.append(line)
        for j, text in enumerate(lines[:3]):
            d.text((x + 16, y + img_h + 58 + j * 23), text, font=font(18), fill=MUTED)
        d.text((x + 16, y + card_h - 24), rel, font=font(14), fill=(110, 118, 132))

    footer_y = H - 54
    d.line([(54, footer_y - 18), (W - 54, footer_y - 18)], fill=LINE, width=1)
    d.text(
        (54, footer_y),
        "Sources: A1 clouds2, A2 city_marathon, A3 space_race, A4 yeti_set_go, A6a sky_battle, A6b parkour_tag, "
        "A7 tgttos_badlands, A5 decision_dome_recreation.",
        font=font(15),
        fill=MUTED,
    )

    OUT.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(OUT, optimize=True)
    print(f"wrote {OUT} ({OUT.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
