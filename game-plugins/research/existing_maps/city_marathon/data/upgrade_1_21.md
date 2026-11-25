# City Marathon — 1.19.2 → 1.21 upgrade notes

Scope: `mcc_citymarathon` (source `City Marathon`), **1.19.2 / DataVersion 3120**, 6,374 chunks, 67 command blocks, 140 shulkers, 84 banners, 7 signs. Research only; nothing was changed.

Tags: **H** observed, **M** inferred from two sources, **L** untested guess.

## 1. Summary

| Area | Risk | Finding |
|---|---|---|
| Command syntax | **High** | 7 blocks fail now and will still fail in 1.21: all are `/gamemode adventure` with no target. A12 verified `permissions.requires.player`; SuccessCount=0 in NBT agrees — **H**. |
| Command syntax | Low | The other 60 commands use 1.19-era syntax that is still valid in 1.21 (`execute if block`, `effect give`, `item replace`, `enchant`, `title`, `fill`, `setworldspawn`) — **M**. |
| Start gate | Medium | `/fill 3 0 10 13 2 10 air` has a leading slash; it executed successfully in 1.19.2 (`SuccessCount=1`, LastExecution 110345), so leading slashes are accepted by command blocks. Do not rely on that in a plugin reimplementation — **H** observed, **M** portability. |
| World data | Low | Terrain, block palette and world height (−64..320) are unchanged between 1.19.2 and 1.21; 6,374 chunks load — **M**. |
| Signs | Low | 1.19.2 `Text1..Text4` will be converted to `front_text.messages` by the 1.20 DataFixer. All 7 signs are front-only — **H** format / **M** upgrade. |
| Item frames | Medium | 39 item frames use the pre-1.20.5 `Item` compound (`id`, `Count`). 1.20.5+ uses the item-component form; the DataFixer should convert, but custom NBT is a common loss point — **M**. |
| Banners | Low | 84 banners have no `Base`/`Patterns` NBT; colour is in the block ID (e.g. `light_gray_banner`, `black_wall_banner`). Nothing pattern-specific to lose — **H**. |
| Command block NBT | Low | `Command`, `auto`, `conditionMet`, `powered`, `CustomName` are preserved. `LastExecution`/`UpdateLastExecution` may be dropped by newer formats; they are not gameplay state — **M/L**. |
| Containers | None | All 140 shulkers, 12 barrels, 3 campfires and 1 blast furnace are empty — **H**. No item-stack conversion risk. |
| Multiverse gamemode | **High** | If `mcc_citymarathon` is configured `gamemode: survival` (the pattern used by other worlds in `plugins/Multiverse-Core/worlds.yml`), the failed adventure commands leave players in survival. `level.dat` GameType=1 does not override a per-world Multiverse gamemode — **M** (worlds.yml read; live world entry not present in the file I can see). |

## 2. The seven failures

From A12 (server verification) and confirmed in `data/command_blocks.csv` / NBT `SuccessCount=0`:

| ID | Coords | Block | Command | Why it fails |
|---|---|---|---|---|
| CB03 | 11,−2,−6 | repeating | `gamemode adventure` | No target; command-block source has no player → `permissions.requires.player`. |
| CB05 | 11,−2,−4 | repeating | `/gamemode adventure` | Same. Leading slash is accepted; missing target is the failure. |
| CB11 | 7,−3,3 | chain | `gamemode adventure` | Same; this chain block also never fired (no LastExecution). |
| CB30 | −387,−8,45 | repeating | `gamemode adventure` | Same. |
| CB31 | −387,−8,47 | repeating | `/gamemode adventure` | Same. |
| CB52 | −208,−2,86 | repeating | `/gamemode adventure` | Same. |
| CB53 | −208,−2,87 | repeating | `gamemode adventure` | Same. |

**Effect:** none. These were the map's only attempt to force adventure mode on join. In 1.21 they remain failures; no DataFixer or syntax change makes a no-target `/gamemode` work from a command block.

**Multiverse implication (M):**
- Multiverse applies its own per-world `gamemode` on world change/join. If the imported world is `survival`, players spawn in survival; the seven adventure commands do nothing.
- Survival consequences: block breaking/placing (course damage), item pickup/crafting, hunger, and normal damage rules. `keepInventory=true` and `fallDamage=false` reduce death loss but do not stop editing.
- The world's `level.dat` has `GameType=1` and `allowCommands=1`; that is the **world default**, not a guarantee once Multiverse takes over — **M**.
- I could not find a `mcc_citymarathon` entry in `plugins/Multiverse-Core/worlds.yml` (read-only grep, H). Either the world is not currently imported in that file, or it is loaded by a different server instance. A12/coordinator should confirm the live per-world gamemode before anyone runs the map. Do not change it as part of this dossier.

## 3. Command-by-command compatibility

| Command family | Count | 1.21 status | Notes |
|---|---:|---|---|
| `execute as @a at @s if block ~ ~-1 ~ <block> run effect give ...` | 36 | **Low risk** | `execute if block` predicate and `effect give` syntax unchanged. Use namespaced effect ids (`minecraft:jump_boost`, `minecraft:speed`) — already present. |
| `execute ... run item replace entity @a armor.chest with elytra` | 6 | **Low risk** | Simple item id still accepted; if the target has no chest slot (spectator), it fails for that target only. |
| `execute ... run item replace entity @a armor.chest with air` | 1 | **Low risk** | `air` is a valid item id. |
| `execute ... run setworldspawn` | 3 | **Low risk** | `setworldspawn` with no coordinates uses the source position; the source is the player from `as @a at @s`. |
| `execute ... run effect clear` / `effect clear @a` | 2 | **Low risk** | `effect clear` with no effect clears all. |
| `enchant @a minecraft:unbreaking 3` | 3 | **Low-Medium** | Requires the player to hold an enchantable item; if the trident is not in hand, the command fails for that player. Does not error the whole block. |
| `enchant @a minecraft:riptide` | 3 | **Low-Medium** | Same; riptide is only valid on a trident and only one riptide level. |
| `title @a actionbar "You are in 1st place"` | 3 | **Low risk** | String form of text component still accepted in 1.21. |
| `title @a title/subtitle {json}` | 4 | **Low risk** | JSON text components still accepted; 1.21.5+ also accepts SNBT components. |
| `item replace entity @a hotbar.0 with minecraft:trident 1` | 1 | **Low risk** | Item syntax unchanged for a simple item. |
| `/fill 3 0 10 13 2 10 air` | 1 | **Low risk** | Leading slash accepted in 1.19.2; normalize in any reimplementation. |
| `/gamemode adventure` (no target) | 7 | **High risk / already broken** | See §2. |

## 4. Data conversion details

### Signs (7)
- 1.19.2 stores text in `Text1..Text4`, `Color`, `GlowingText`.
- 1.20+ stores `front_text.messages`, `front_text.color`, `front_text.has_glowing_text`, plus `back_text`.
- The 7 signs are all front-only and use black text with glow — **H**. The 1.20 DataFixer should move them; verify after upgrade — **M**.
- Click events: none — **H**.

### Item frames (39)
- Pre-1.20.5: `Item: {id:"minecraft:anvil", Count:1b}`.
- 1.20.5+: item stack is `item: {id:"minecraft:anvil", count:1}` plus `components`.
- The frames are invisible (`Invisible:1`) and hold anvils or stained-glass panes as props — **H**. Visual loss would be cosmetic but noticeable on the train windows/traffic lights — **M**.

### Banners (84)
- 1.19.2 banner colour is in the block ID (`light_gray_banner`, `black_wall_banner`, …), and patterns live in `Patterns`.
- These banners have **no** `Base`/`Patterns` NBT; they are plain colour flags — **H**.
- No pattern conversion risk; colour ID remains — **M**.

### Command blocks (67)
- `Command`, `auto`, `conditionMet`, `powered`, `CustomName` are the gameplay fields; all are preserved by world upgrade — **M**.
- `LastExecution`/`UpdateLastExecution` are bookkeeping; newer formats may drop them. They are not needed to run the map — **M/L**.
- All 60 repeating blocks are `auto=1`, so they run every tick in loaded chunks. The 5 impulse / 2 chain blocks are redstone-driven; the chain blocks never fired in the saved world — **H**.

### Containers (156)
- 140 `light_gray_shulker_box`, 12 `barrel`, 3 `campfire`, 1 `blast_furnace`; all empty — **H**.
- No item-stack conversion risk; the shulker block entity itself is stable — **M**.

## 5. 1.21 test checklist (read-only, for A12/coordinator)

1. Join `mcc_citymarathon` and record `gamemode` immediately. If survival, the map's adventure assumption is broken — **H** test.
2. Press the polished-blackstone button at **8,0,8**; confirm the barrier gate at z10 disappears (the `/fill` block), the trident appears in hotbar 0, and the title sequence runs — **H** test.
3. Stand on a yellow-glazed strip; confirm Speed IV and `setworldspawn` — **H** test.
4. Stand on gray wool; confirm the chestplate is cleared (elytra removal) — **H** test.
5. Stand on polished diorite at the long x−351 strip; confirm elytra equip — **H** test.
6. Check the 7 signs render their text and the 39 item frames still show their items — **H** test.
7. Check the 84 banners keep their colours — **H** test.
8. Re-check the 7 no-target `gamemode` blocks; expect the same failures — **H** test.
9. Do not “fix” anything in this pass; record only — plan rule.

## 6. Upgrade verdict

- **Terrain/blocks:** safe to upgrade; 1.21 keeps the palette and height — **M**.
- **Commands:** 60/67 are portable; 7 are already broken and remain broken — **H**.
- **Entities/signs:** cosmetic DataFixer work; verify item frames and signs — **M**.
- **Game mode:** the real blocker is not 1.21 syntax but the missing player-target gamemode and the Multiverse per-world setting — **H** for the command failure / **M** for the live setting.
- **Elytra/route logic:** independent of version; the rooftop pads are unreachable by ground in 1.19.2 and will remain so unless the build adds access — **H** geometry.
