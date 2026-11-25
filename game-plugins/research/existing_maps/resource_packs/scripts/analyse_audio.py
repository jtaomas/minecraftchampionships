#!/usr/bin/env python3
"""Analyse every distinct OGG in the Clouds 2 pack and draw waveform evidence.

Uses mutagen for codec/duration (authoritative for OggOpus) and soundfile for
envelopes. All sources are read-only.
"""
import csv
import os

import numpy as np
import soundfile as sf
from mutagen import File as MFile
from PIL import Image, ImageDraw, ImageFont

HERE = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs"
CL = os.path.join(HERE, "packs", "clouds2", "extracted", "assets", "minecraft", "sounds")
VAN = os.path.join(HERE, "data", "vanilla_compare")
OUT = os.path.join(HERE, "frames")
DATA = os.path.join(HERE, "data")
FONT = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 13)
FONT_B = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 18)

# pack file, codec, seconds, event(s) that reach it, trigger count in Clouds 2, note
ROWS = [
    ("acerace/jumppad.ogg", "acerace.jumppad (declared, never called)", 0, "copy of bell_use; acerace event unregistered"),
    ("acerace/megajumppad.ogg", "acerace.megajumppad (declared, never called)", 0, "copy of candle extinguish"),
    ("acerace/speed.ogg", "acerace.speed (declared, never called)", 0, "not copied to any vanilla path"),
    ("acerace/waterboost.ogg", "acerace.waterboost (declared, never called)", 0, "not copied to any vanilla path"),
    ("block/bell/bell_use01.ogg", "block.bell.use", 18, "jump pads (red/magenta/pink/yellow glazed)"),
    ("block/candle/extinguish1.ogg", "block.candle.extinguish", 6, "mega-jump pads (orange shulker)"),
    ("block/cherrywood_trapdoor/toggle1.ogg", "(no vanilla event uses this path)", 0, "OggOpus, 5:10 track, unreachable"),
    ("records/far.ogg", "music_disc.far", 1, "5:10 Ace Race track, start area"),
    ("block/cave_vines/break1.ogg", "block.cave_vines.break", 6, "lime shulker pads; vanilla break5 absent"),
    ("block/chain/break1.ogg", "block.chain.break", 6, "yellow glazed pads"),
    ("block/chain/step1.ogg", "block.chain.step", 6, "falling (cave_air below)"),
    ("block/brewing_stand/brew1.ogg", "block.brewing_stand.brew", 1, "start sequence, 12.9s"),
    ("block/azalea/break1.ogg", "block.azalea.break", 0, "unused short chime"),
]
VAN_MAP = {
    "acerace/jumppad.ogg": ("1.19.2", "minecraft_sounds_block_bell_bell_use01.ogg"),
    "acerace/megajumppad.ogg": ("1.19.2", "minecraft_sounds_block_candle_extinguish1.ogg"),
    "block/bell/bell_use01.ogg": ("1.19.2", "minecraft_sounds_block_bell_bell_use01.ogg"),
    "block/candle/extinguish1.ogg": ("1.19.2", "minecraft_sounds_block_candle_extinguish1.ogg"),
    "records/far.ogg": ("1.19.2", "minecraft_sounds_records_far.ogg"),
    "block/cave_vines/break1.ogg": ("1.19.2", "minecraft_sounds_block_cave_vines_break1.ogg"),
    "block/chain/break1.ogg": ("1.19.2", "minecraft_sounds_block_chain_break1.ogg"),
    "block/chain/step1.ogg": ("1.19.2", "minecraft_sounds_block_chain_step1.ogg"),
    "block/brewing_stand/brew1.ogg": ("1.19.2", "minecraft_sounds_block_brewing_stand_brew1.ogg"),
    "block/azalea/break1.ogg": ("1.19.2", "minecraft_sounds_block_azalea_break1.ogg"),
}


def meta(path):
    f = MFile(path)
    info = f.info
    codec = type(f).__name__
    dur = float(info.length)
    rate = getattr(info, "sample_rate", getattr(info, "original_sample_rate", None))
    ch = getattr(info, "channels", None)
    bitrate = getattr(info, "bitrate", None)
    title = ""
    if f.tags:
        try:
            for k, v in f.tags.items():
                if str(k).lower() in ("title", "tit2", "\xa9nam"):
                    title = str(v[0] if isinstance(v, list) else v)
                    break
        except Exception:
            title = ""
    return codec, dur, rate, ch, bitrate, title


def envelope(path, points=1200, seconds=None):
    with sf.SoundFile(path) as f:
        sr = f.samplerate
        total = int(f.frames)
        if seconds:
            total = min(total, int(seconds * sr))
        block = max(1, int(sr * 0.5))
        vals = []
        read = 0
        while read < total:
            data = f.read(min(block, total - read), dtype="float32", always_2d=True)
            if not len(data):
                break
            vals.append(float(np.max(np.abs(data))))
            read += len(data)
    if not vals:
        return np.zeros(points)
    vals = np.array(vals)
    idx = np.linspace(0, len(vals) - 1, points).astype(int)
    return vals[idx]


def main():
    rows = []
    series = []
    for rel, event, count, note in ROWS:
        p = os.path.join(CL, rel)
        codec, dur, rate, ch, bitrate, title = meta(p)
        van = ""
        vdur = ""
        if rel in VAN_MAP:
            ver, vn = VAN_MAP[rel]
            vp = os.path.join(VAN, ver, vn)
            if os.path.exists(vp):
                vcodec, vdur, vrate, vch, vbr, vtitle = meta(vp)
                van = f"{ver} {vn} ({vdur:.2f}s {vcodec})"
        rows.append({"pack_file": rel, "bytes": os.path.getsize(p), "codec": codec,
                     "seconds": round(dur, 3), "sample_rate": rate or "", "channels": ch or "",
                     "bitrate": bitrate or "", "title": title,
                     "event": event, "triggers_in_clouds2": count, "vanilla_counterpart": van,
                     "note": note})
        series.append((rel, dur, codec, event, envelope(p, seconds=60 if dur > 60 else None), "first 60 s" if dur > 60 else "full"))
    with open(os.path.join(DATA, "audio.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        w.writeheader()
        w.writerows(rows)
    for r in rows:
        print(r)

    W = 1600
    row_h = 92
    H = 60 + len(series) * row_h
    out = Image.new("RGB", (W, H), (22, 22, 26))
    d = ImageDraw.Draw(out)
    d.text((14, 16), "Clouds 2 pack audio - waveform (peak-normalised; long files first 60 s)", font=FONT_B, fill=(255, 190, 60))
    for i, (name, dur, codec, event, env, scope) in enumerate(series):
        y0 = 60 + i * row_h
        d.text((14, y0 + 2), name, font=FONT, fill=(235, 235, 235))
        d.text((14, y0 + 20), f"{dur:8.2f}s {codec:9} {scope:11} <- {event}", font=FONT, fill=(150, 150, 160))
        x0, x1 = 560, W - 20
        mid = y0 + 46
        amp = 30
        pts = []
        for j, v in enumerate(env):
            x = x0 + (x1 - x0) * j / (len(env) - 1)
            pts.append((x, mid - v * amp))
        pts2 = [(x, 2 * mid - y) for x, y in reversed(pts)]
        d.line([(x0, mid), (x1, mid)], fill=(60, 60, 70))
        d.polygon(pts + pts2, fill=(90, 160, 235))
    out.save(os.path.join(OUT, "waveforms_clouds2.png"))
    print("wrote waveform", out.size)


if __name__ == "__main__":
    main()
