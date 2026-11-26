# V4-C — Gaps, unknowns and judgement calls

Everything here is either (a) not visible in the references, (b) visible but
not reproducible at 1:1 from a screenshot, or (c) a decision I had to make.
Nothing here has been silently invented: where I had to choose, the choice is
named below.

---

## 1. Player identities — the big gap

**Only three roster slots in the whole section could be filled. Three of the
forty slots (ten teams × four players) have a name; thirty-seven do not.**

### What the references actually give

`reference/StageAreaIntro.png` contains the only team-roster statement in the
whole pack. Chat line, bottom left:

> `The Lime Llamas: Chocolate_Orange, Kaos21la_Faniel49 and HeerTimothy`

(`crops/txt_chat.png`, `crops/txt_names.png`) — three names for a four-player
team, so the fourth member is not named. The same screenshot shows the three
name tags on players standing beside the lime plot; `MainHub-StageArea.png`
shows `Chocolate_Orange` there again.

### Why the other names cannot be used

Every other name tag in the Stage Area shots belongs to a **live player standing
in the lobby**, not to a statue. That distinction matters:

* `StageAreaBuilding.png` — `Wibbade` and `Byrt` are name tags over live players
  on the red stage: one on a black pedestal, one on the checkered floor.
* `StageAreaAnC.png` — `Tomeroo` stands *next to* the four pink pedestal heads;
  `Dwittyy`, `TurnJUS`, `oreo_noob` are over players in the background.
* `StageAreaPride.png` — `plantskid` is on the player riding the blue horse.
* `MainHub-StageArea.png` — `Chocolate_Orange`, `Mountain63`, `SomethingnIdam`,
  `Crazy66` are all over walking players.

Not one pedestal head carries a name plate, so **no visible name can be attached
to a pedestal**. Putting a spectator's name on a team pedestal would be an
invention, so I did not.

Observed name tags, for the record (all live players, none used in the build):
`Chocolate_Orange`, `Kaos21la_Faniel49`, `HeerTimothy`, `Wibbade`, `Byrt`,
`Tomeroo`, `plantskid`, `Dwittyy`, `TurnJUS`, `oreo_noob`, `Mountain63`,
`SomethingnIdam`, `Crazy66`.

### What is in the build instead

| | |
|---|---|
| named heads | 6 — the three lime names, once on the lime column, once on the lime plot |
| placeholder heads | 74 — bare `minecraft:player_head` |

A bare `minecraft:player_head` is used on purpose: it displays the default skin,
so an empty roster slot is visible as an empty slot rather than as a plausible
but wrong identity. Slot order inside a column/plot is roster order, so filling
the gaps later is a straight text edit of `TEAMS` in `gen_v4c.py` and a re-run.

### Open questions to answer before this ships

1. **The lime slot order is my choice.** The chat lists
   `Chocolate_Orange`, `Kaos21la_Faniel49`, `HeerTimothy`; I used that order for
   slots 1, 2, 3 and left slot 4 empty. The real roster order is unknown.
2. **`Kaos21la_Faniel49` — the 7th glyph is ambiguous.** Both crops render a
   bare vertical stroke between `21` and `_Faniel49`. Read as `l` it is
   `Kaos21la_…`; read as `1` it is `Kaos21a_…`. I used the `l` reading because
   the crop at 12× shows a glyph without the `1`'s diagonal head stroke, but this
   needs confirming against a roster source. `crops/word0.png` has the pixels.
3. **Three names for four slots.** Either the fourth member was not announced
   yet, or this was a three-player team. Both are possible from a screenshot.

## 2. Team names

Only **Lime Llamas** is legible in the references (chat line plus the llama icon
on the name tags). The other nine names in the build —
`Red Rabbits`, `Orange Ocelots`, `Yellow Yaks`, `Green Geckos`, `Cyan Coyotes`,
`Aqua Axolotls`, `Blue Bats`, `Purple Pandas`, `Pink Parrots` — are the standard
MC Championship roster for the season that "Lime Llamas" belongs to. They are a
*consistent inference*, not something the screenshots prove. **If the owner knows
this hub was a different event, rename them** — one table at the top of
`gen_v4c.py`, and the plot signs / column nameplates follow.

The colour order (red, orange, yellow, lime, green, cyan, aqua, blue, purple,
pink) *is* legible, twice: the hanging banners in `reference/MainHub.png` read
left to right in exactly that order, and the rainbow ring on the plinth in
`reference/MCCHubSeason1.png` runs the same sequence. That order is safe.

## 3. Column dimensions vs. the reference

The brief fixes the type at **3 × 3, 7 high, four head pedestals**. The
references do not show that type:

* `crops/s6_columns_left.png`, `crops/sd_columns_left.png` (entrance columns):
  solid team colour, roughly 3–4 wide, 8–10 visible, flat topped, stepped cap,
  snowmen on top in the festive variant. No head pedestals.
* `crops/s1_cols_zoom.png` (Season 1 team columns): one is a 2 × 2 purple shaft
  about 16 high with a **single** head on top; the next is a chunky stepped
  4 × 4 orange tower. They are individually custom-built, different heights,
  different footprints.

So: **the columns here follow the brief, not the photographs.** I added two
things the photographs do show and the brief does not forbid — a 3 × 3
smooth-stone footing pad under each shaft (so a team's column does not read as a
fragment of its same-coloured plot) and the four descending pedestal heights
that implement "ordered highest first and descending". If the owner wants the
columns to read like the entrance shots instead, the height needs to go to
8–10 and the heads to one per column.

## 4. The blank wall panels

Each plot's backdrop has a 9 × 3 recessed `light_gray_concrete` panel. In the
references that panel is where the team's custom art goes, and each team's is
different:

| reference | what the panel holds |
|---|---|
| `StageAreaTwitch.png` | purple frame + grey Twitch "glitch" mark |
| `StageAreaAnC.png` | giant red/white letters and gold coin piles |
| `StageAreaPride.png` | a stepped rainbow arch, quartz steps, a horse |
| `StageAreaBuilding.png` | an orange portal-wall building, black/red carpet, checkerboard |
| `MainHub-StageArea.png` | coloured decks with saplings, flowers, hedges |

Ten different custom builds cannot be reconstructed from five screenshots, and
inventing nine of them would be fabrication. The panels are therefore left
blank, and no custom props were built — that is also V4-D's scope.

## 5. Plot proportions are not measurable

Every Stage Area screenshot is a perspective shot with no known-distance
reference. I set the plot at **13 × 13**, a 2-course stone skirt, a deck 2 above
the surrounding path, a 5-course backdrop with a 9 × 3 panel and a 2-course
quartz front step, by eye against `crops/twitch_plot.png` and
`crops/sa_plots_right.png`. The real plots could plausibly be 12–16 wide and
3–5 courses up. Landmark-level proportions (columns 3 wide, plots side by side
along a path, four heads per plot) are right; the metre-level numbers are
estimates.

Also unverifiable from the screenshots: whether the plot floor is carpet or a
full block (I used carpet over concrete, which matches the visible texture seam
in `crops/twitch_plot.png`), and whether the dark inset line is black or a dark
dye of the team colour (I used the team's own terracotta, because the blue plot
shows a dark navy line and the lime plot a black-ish one).

## 6. NBT that needs a live check

Two bits of syntax could not be tested, because I was told not to touch 25580 or
25591 and had no other 1.21.1 world to paste into.

1. **Named heads.** Emitted as
   `minecraft:player_head{profile:{name:"Chocolate_Orange"}}`, i.e. the 1.20.5+
   block-entity key. On a pre-1.20.5 server the key was `SkullOwner`. Targets
   1.21.1, so `profile` should be right — but if those six `setblock`s error,
   the heads simply stay air and the fix is a find/replace to
   `SkullOwner:{Name:…}`.
2. **Skull skins need an online-mode server.** A name-only profile resolves from
   the Mojang session service; on an offline server all six named heads will
   render as the default skin, exactly like the placeholders. If the review
   world is offline, the only way to show real skins is to bake full texture
   values into the profile, which needs the actual skin data — not available in
   the reference pack.

Signs use `minecraft:oak_wall_sign[facing=…]{front_text:{color:"black",
messages:[…]},is_waxed:1b}`, which is the 1.20+ format and should be safe. If a
sign errors it leaves air; nothing else in the section depends on it.

## 7. Site placement is authored, and needs V4-E's blessing

The references do not show a plan view of the Stage Area, so the avenue is my
own composition: two rows of five plots facing a north spur, teams met in roster
order. It occupies `x -22..22, z -112..-36`, which is deliberately:

* clear of the dome plinth (`r=30`, nearest block 37.4 out)
* clear of the avenue itself (`x -4..4`, left for V4-A's paving)
* clear of V4-A's entrance (south), V4-B's west flank and V4-D's east flank
* inside V4-A's island except at the far north end, where the last two plots
  reach `r=114` of an island of roughly `r=120`

**Unresolved dependency:** if V4-D's six training portals or V4-A's path network
claim the north spur, this section has to move or rotate. Nothing else of mine
overlaps anybody, but that has not been confirmed against A/B/D's actual
footprints — they had not published any when this was built.

## 8. Not done, by design

* No island, terraces, water, paving or avenue floor (V4-A).
* No dome, plinth, crown, clock (V4-E / other bot).
* No mountain, Hall of Fame, rainbow bridge, snow globe (V4-B).
* No aqua building, no training portals, no trees, lamps, balloons, presents,
  candy canes, stalls, snowmen (V4-D). The plots therefore look barer than the
  references, which show saplings, flowers, coin piles and lamp posts on them.
* **No festive/snow dressing.** The Stage Area references are the non-snow hub.
  `MCCHub-SD.png` shows the team columns wearing snowmen in the festive build;
  that is prop scope, so it is not here. A snow pass on top of this (snow layers
  on the caps and skirts) would be a cheap V4-D/V4-E follow-up.
* No lighting audit, no jump/step audit, no mob-proofing, no `gamerule` or
  world setup.
