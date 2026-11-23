# Block Showdown resource pack

An original resource pack for the private tournament server: HUD glyphs, branded chest menus, custom item looks, and synthesized sound stings. Scripts in `src/` generate every pixel and every sample, so nothing is downloaded or copied.

* Target: Minecraft **1.21.1** (Paper 1.21.1), `pack_format` **34**
* Output folder: `pack/`
* Distributable: `dist/BlockShowdownPack.zip`, with its SHA-1 in `dist/sha1.txt`
* Java constants: `McmGlyphs.java` (class `com.mcm.api.McmGlyphs`), generated
* Visual overview: `preview_sheet.png`

## Regenerate

```
python -m pip install --user pillow numpy soundfile
python src/build.py
```

The build wipes and rewrites `pack/`, then does the following:

* writes the PNGs and JSON
* synthesizes the `.ogg` files
* regenerates `McmGlyphs.java`, the glyph table below, and `preview_sheet.png`
* validates all JSON and cross-references: font files exist, ascent <= height, no duplicate codepoints, override order is ascending, models, textures and sounds all exist
* zips the pack (sorted entries, fixed timestamps) and writes the SHA-1. libsndfile gives each Ogg stream a random serial number, so the SHA-1 changes on every rebuild. Always copy the new value.

If you change anything, rebuild and update `resource-pack-sha1` (see below).

| Script | Contents |
|---|---|
| `src/pixel.py` | Sprite engine. ASCII masks are auto-shaded and outlined using one shared palette. Also holds the logo pixel font. |
| `src/icons.py` | Team, HUD, game-tile and brand artwork |
| `src/gui.py` | Chest-menu overlay backgrounds |
| `src/items.py` | 16x16 item textures and `custom_model_data` override table |
| `src/sounds.py` | Sound synthesis (numpy), written as Ogg Vorbis through `soundfile` |
| `src/build.py` | Puts it all together, then validates, zips and builds the preview |

## Serving the pack

In `server.properties` (host the zip somewhere the clients can reach over HTTP(S)):

```
resource-pack=https://<your-host>/BlockShowdownPack.zip
resource-pack-sha1=<contents of dist/sha1.txt>
require-resource-pack=false
resource-pack-prompt={"text":"Block Showdown HUD pack - recommended!","color":"gold"}
```

You can also send it per player with `player.setResourcePack(url, sha1Bytes, prompt, force)`.

## Fallback: the plugins must only use glyphs when the pack is loaded

Glyphs sit at private-use codepoints (U+E000 and up, U+F8xx). A player who declined the pack, or whose download failed, sees **missing-glyph boxes** in place of every glyph. Negative-space characters also stop shifting text, so overlays turn into garbage. The rules:

1. **Track pack state.** In a listener:
   ```java
   @EventHandler
   public void onPack(PlayerResourcePackStatusEvent e) {
       switch (e.getStatus()) {
           case SUCCESSFULLY_LOADED -> McmGlyphs.setHasPack(e.getPlayer().getUniqueId(), true);
           case DECLINED, FAILED_DOWNLOAD, FAILED_RELOAD, INVALID_URL, DISCARDED ->
                   McmGlyphs.setHasPack(e.getPlayer().getUniqueId(), false);
           default -> { }
       }
   }
   @EventHandler
   public void onQuit(PlayerQuitEvent e) { McmGlyphs.setHasPack(e.getPlayer().getUniqueId(), false); }
   ```
2. **Build text per player.** Use `McmGlyphs.or(uuid, McmGlyphs.COIN, "§6⛁")` or a plain word such as `"coins"`. Everything must still read correctly as plain text: team names in their chat colour, `[1st]`, `Time: 1:30`, and so on.
3. **Shared surfaces** (the sidebar objective title, boss bars, holograms, entity names) are the same for every viewer. Use per-player scoreboards and boss bars when you want glyphs there, or keep the shared versions plain text.
4. **Menus:** use `guiTitle(...)` only for players with the pack, and the old plain title for everyone else. The admin plugin finds its menus by comparing titles (`title.equals(ADMIN_GUI_TITLE)`), so it has to recognize **both** title strings. Identifying menus by `InventoryHolder` is more robust.
5. **Sounds:** `McmGlyphs.sound(uuid, McmGlyphs.SOUND_ROUND_WIN)` returns the custom key, or a vanilla fallback (`SOUND_FALLBACK`) for players without the pack.
6. **Items:** `custom_model_data` is harmless without the pack, because the player just sees the vanilla item. Give items a proper display name anyway.

## Font glyphs

All glyphs are added to `minecraft:default` (`assets/minecraft/font/default.json`). Each glyph is drawn 1:1, with the provider `height` equal to the image height, so the pixels stay crisp at every GUI scale. Ascent is tuned so that:

* 9-px icons sit on the text line, from 1 px above cap height to the descender line
* 16-px game tiles are centred on the line

The engine draws inline text shadow behind glyphs as usual, which reads as a soft drop shadow.

* Team icons each have a **distinct shape** (badge, disc, triangle, diamond, plus, droplet, shield, crescent, sparkle, ring), so they stay distinguishable for colour-blind players.
* Game tiles are 16x16 "app icon" tiles with a coloured rounded frame.
* `LOGO` (sidebar banner) and `LOGO_LARGE` (stacked splash) are lettered in an original 7-px display face drawn in `pixel.py`.

<!-- GLYPHS:BEGIN -->
| Constant | Codepoint | Size (px) | Ascent | Purpose |
|---|---|---|---|---|
| `TEAM_RED` | U+E000 | 9x9 | 8 | Red team icon |
| `TEAM_ORANGE` | U+E001 | 9x9 | 8 | Orange team icon |
| `TEAM_YELLOW` | U+E002 | 9x9 | 8 | Yellow team icon |
| `TEAM_LIME` | U+E003 | 9x9 | 8 | Lime team icon |
| `TEAM_GREEN` | U+E004 | 9x9 | 8 | Green team icon |
| `TEAM_CYAN` | U+E005 | 9x9 | 8 | Cyan team icon |
| `TEAM_AQUA` | U+E006 | 9x9 | 8 | Aqua team icon |
| `TEAM_BLUE` | U+E007 | 9x9 | 8 | Blue team icon |
| `TEAM_PURPLE` | U+E008 | 9x9 | 8 | Purple team icon |
| `TEAM_PINK` | U+E009 | 9x9 | 8 | Pink team icon |
| `COIN` | U+E010 | 9x9 | 8 | coins / points |
| `CROWN` | U+E011 | 9x8 | 8 | leader / winner |
| `SKULL` | U+E012 | 9x9 | 8 | elimination |
| `SWORD` | U+E013 | 9x9 | 8 | sword |
| `BOW` | U+E014 | 9x9 | 8 | bow |
| `HEART` | U+E015 | 9x9 | 8 | heart |
| `CLOCK` | U+E016 | 9x9 | 8 | timer |
| `FLAG` | U+E017 | 9x9 | 8 | checkpoint flag |
| `TROPHY` | U+E018 | 11x9 | 8 | trophy |
| `MEDAL_1` | U+E019 | 9x10 | 9 | 1st place |
| `MEDAL_2` | U+E01A | 9x10 | 9 | 2nd place |
| `MEDAL_3` | U+E01B | 9x10 | 9 | 3rd place |
| `ARROW_UP` | U+E01C | 9x9 | 8 | rank up |
| `ARROW_DOWN` | U+E01D | 9x9 | 8 | rank down |
| `STAR` | U+E01E | 9x9 | 8 | star |
| `LOCK` | U+E01F | 9x9 | 8 | locked |
| `CHECK` | U+E020 | 9x8 | 8 | yes / done |
| `CROSS` | U+E021 | 9x7 | 7 | no / failed |
| `FLAG_FINISH` | U+E022 | 8x9 | 8 | finish line |
| `GAME_SKYBATTLE` | U+E040 | 16x16 | 12 | skybattle |
| `GAME_BATTLEBOX` | U+E041 | 16x16 | 12 | battlebox |
| `GAME_DODGEBOLT` | U+E042 | 16x16 | 12 | dodgebolt |
| `GAME_PARKOURWARRIOR` | U+E043 | 16x16 | 12 | parkourwarrior |
| `GAME_ROCKETSPLEEF` | U+E044 | 16x16 | 12 | rocketspleef |
| `GAME_HITW` | U+E045 | 16x16 | 12 | hitw |
| `GAME_BINGO` | U+E046 | 16x16 | 12 | bingo |
| `GAME_LOCKOUT` | U+E047 | 16x16 | 12 | lockout |
| `GAME_GRIDRUNNERS` | U+E048 | 16x16 | 12 | gridrunners |
| `GAME_RAILROADRUSH` | U+E049 | 16x16 | 12 | railroadrush |
| `GAME_SANDSOFTIME` | U+E04A | 16x16 | 12 | sandsoftime |
| `GAME_ACERACE` | U+E04B | 16x16 | 12 | acerace |
| `GAME_TGTTOS` | U+E04C | 16x16 | 12 | tgttos |
| `GAME_BUILDMART` | U+E04D | 16x16 | 12 | buildmart |
| `GAME_MELTDOWN` | U+E04E | 16x16 | 12 | meltdown |
| `GAME_PARKOURTAG` | U+E04F | 16x16 | 12 | parkourtag |
| `GAME_FOOTRACE` | U+E050 | 16x16 | 12 | footrace |
| `GAME_DOME` | U+E051 | 16x16 | 12 | dome |
| `LOGO` | U+E060 | 97x10 | 9 | sidebar title banner (emblem + BLOCK SHOWDOWN) |
| `LOGO_LARGE` | U+E061 | 84x28 | 18 | stacked splash logo for titles / MOTD-style chat headers |
| `EMBLEM` | U+E062 | 9x9 | 8 | inline brand mark |
| `TITLE_BAR` | U+E063 | 120x14 | 10 | translucent lower-third bar for actionbar/subtitle text |
| `GUI_SELECTOR` | U+E070 | 176x222 | 13 | 6-row chest overlay (54 slots) |
| `GUI_ADMIN` | U+E071 | 176x168 | 13 | 3-row chest overlay (27 slots) |
| `GUI_DOME` | U+E072 | 176x168 | 13 | 3-row chest overlay (27 slots) |
| `GUI_KIT` | U+E073 | 176x132 | 13 | 1-row chest overlay (9 slots) |
| `GUI_TEAM` | U+E074 | 176x168 | 13 | 3-row chest overlay (27 slots) |
| `NEG_1..NEG_256` | U+F801..U+F809 | space | - | -1,-2,-4 ... -256 px advance |
| `POS_1..POS_256` | U+F821..U+F829 | space | - | +1,+2,+4 ... +256 px advance |
<!-- GLYPHS:END -->

### Spacing

`McmGlyphs.space(int px)` builds any shift from the powers-of-two space glyphs. Negative values move left. These glyphs use the font `space` provider, so they have no pixels and an exact advance.

### Title / actionbar bar

`McmGlyphs.onTitleBar("Round 2")` returns the `TITLE_BAR` glyph with the text centred on it. The total advance equals the bar's width, so the vanilla centring of titles and actionbars stays correct. Text width is estimated from the vanilla ASCII advance table, including `§l` bold. Text inside a title is drawn at 4x, so for full-size title cards use the bar in the **subtitle** or **actionbar**.

## Chest menu overlays (font trick)

The vanilla container title is drawn at (8, 6) relative to the GUI's top-left corner. Each `GUI_*` glyph is a full 176-px-wide panel with `ascent 13`, so its top lands exactly on the GUI's top edge. The title string:

```
§f + space(-8) + GUI_X + space(-(177 - 8)) + §f + label
```

* `§f` stops the background being tinted by the default dark-grey title colour.
* `space(-8)` moves from the title position to the GUI's left edge.
* `177` is the glyph advance (width 176 + 1). Moving back 169 returns to x = 8, so the label prints in the coloured header band.

`McmGlyphs.guiTitle(GUI_X, label)` builds this string. Items render above the overlay (they are drawn at a higher z), and the vanilla "Inventory" label stays readable because the lower panel is light.

| Constant | Inventory size | Use for | Highlights |
|---|---|---|---|
| `GUI_SELECTOR` | 54 (`GUI_SELECTOR_SIZE`) | game selector ("More games" / extra menu) | blue header, star |
| `GUI_ADMIN` | 27 | `/mcm` admin menu | red header, gold frame on slot 4 (Start Championship), tinted row slots 10-17 |
| `GUI_DOME` | 27 | Decision Dome vote menu | purple header, tinted middle row 9-17 (put the vote options there) |
| `GUI_KIT` | 9 | PvP kit picker | orange header, gold pedestals on slots 1, 3, 5, 7 (matches current kit layout); other slots dimmed |
| `GUI_TEAM` | 27 | team picker | cyan header |

Example:

```java
Inventory inv = Bukkit.createInventory(null, McmGlyphs.GUI_ADMIN_SIZE,
        McmGlyphs.guiTitle(McmGlyphs.GUI_ADMIN, "Tournament Control"));
```

The inventory size **must** match the overlay. A 27-slot overlay on a 54-slot chest will be misaligned.

## Custom item models (`custom_model_data`)

1.21.1-style overrides in `assets/minecraft/models/item/*.json`. Textures are in `assets/mcmshow/textures/item/`.

| Item | CMD | Model | Constant |
|---|---|---|---|
| `arrow` | 1001 | Dodgebolt energy bolt | `CMD_DODGEBOLT_BOLT` |
| `paper` | 1001-1004 | Battle Box kit tokens: Fighter, Archer, Marksman, Medic | `CMD_KIT_FIGHTER` ... `CMD_KIT_MEDIC` |
| `paper` | 1010 | Decision Dome vote ballot | `CMD_VOTE_TOKEN` |
| `paper` | 1020 | Parkour checkpoint token | `CMD_CHECKPOINT_TOKEN` |
| `paper` | 1030 | neutral team flag | `CMD_TEAM_FLAG` |
| `paper` | 1031-1040 | team-coloured flags (Red ... Pink, same order as `TEAM_NAMES`) | `CMD_TEAM_FLAG_RED` ... |
| `golden_shovel` | 1001 | Rocket Spleef launcher (handheld) | `CMD_ROCKET_LAUNCHER` |
| `gold_ingot` | 1001 | trophy | `CMD_TROPHY` |

Set the value with `meta.setCustomModelData(McmGlyphs.CMD_TROPHY)`. Overrides match ">= value" and the last match wins, so unused numbers between the listed ones show the previous model.

The bolt texture only changes the arrow **item**. A fired arrow uses the vanilla arrow entity renderer.

## Sounds (`mcmshow:*`)

All sounds are synthesized in `src/sounds.py`: FM bells, band-limited brass, filtered noise drums, and original melodies. They are mono 44.1 kHz Ogg Vorbis, peak-normalized to -4 dBFS. Subtitles are in `assets/mcmshow/lang/en_us.json`.

| Key | Length | Character |
|---|---|---|
| `mcmshow:countdown_tick` | 0.16 s | bright wood-block / bell tick |
| `mcmshow:countdown_go` | 0.7 s | C-major brass stab + kick + cymbal |
| `mcmshow:point_gained` | 0.32 s | two-note "pling" (E6 to B6) |
| `mcmshow:elimination` | 0.42 s | descending zap + crunch |
| `mcmshow:team_eliminated` | 1.35 s | low descending brass G-Eb-C |
| `mcmshow:round_win` | 1.85 s | short brass fanfare ending on a held C chord |
| `mcmshow:checkpoint` | 0.6 s | rising three-bell chime with shimmer |
| `mcmshow:vote_cast` | 0.16 s | soft pop + blip |
| `mcmshow:vote_locked` | 0.75 s | thunk + rising D-major arpeggio |
| `mcmshow:podium_fanfare` | 3.6 s | snare roll, then a D-major fanfare and final chord |

```java
p.playSound(p.getLocation(), McmGlyphs.sound(p.getUniqueId(), McmGlyphs.SOUND_ROUND_WIN),
        SoundCategory.MASTER, 1f, 1f);
```

## Vanilla tweaks (low risk)

* **White boss bar** (`textures/gui/sprites/boss_bar/white_*.png`) is restyled as the event timer: a dark navy track with a gold-to-coral fill. Use `BarColor.WHITE` for the timer bar. Other boss bar colours are untouched.

Nothing else in vanilla is replaced. The scoreboard, tab list, chat and HUD keep their vanilla look.

## Needs testing in a real client

* Chest overlays: confirm the alignment at GUI scales 1-4. The overlay should sit exactly under the vanilla slots, and the title should appear in the header band.
* Inline ascent of icons in chat, the sidebar and the tab list. Adjust `inline_ascent()` in `build.py` if you prefer them 1 px lower.
* `TITLE_BAR` centring with bold or unusual characters (the width table is approximate).
* Sidebar title with `LOGO`. It is 11 px tall, so it slightly overhangs the 9-px title row. That is intentional, but check it.
* Sound loudness relative to vanilla at your server's volume settings.
