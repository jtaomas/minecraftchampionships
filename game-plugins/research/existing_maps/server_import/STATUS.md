# A12 — server-import verification status

**Status:** done for the requested read-only scope. No server/world changes.
**Checked:** 2026-11-25 00:10–02:40 local
**Server:** Paper 1.21.1-133-3cb8529, MC 1.21.1, DataVersion 3955, port 25580, RCON 25576 `(H)`
**Players at check:** 0; TPS 20.0/20.0/20.0 `(H)`

## Done

- All nine worlds load: `mv info` + `execute in <world> run gamerule doDaylightCycle` succeeded for each; import window log 2026-11-24-18 shows each world loading at 23:31 `(H)`.
- Spawns read block-by-block: all nine are clear at feet/head and have solid ground; DecisionDome was auto-adjusted from 0,4,0 to 0,76,0 `(H)`.
- Gamerules, Multiverse flags (`survival`, `allow-flight=false`, `keep-spawn-in-memory=true`, PvP/hunger true), command-block counts/types/auto state, signs, datapack state and resource packs recorded per world `(H)`.
- 455 command blocks in Clouds 2 / City Marathon / Space Race / Yeti decoded: 41 exact syntax failures, 8 behaviour risks, 406 OK `(H)`.
- TGTTOS Badlands/Industry/Pits: pack_format 10, old plural `functions`/`tags/functions` layout, 45 old item-NBT lines, packs disabled in `level.dat` though enabled in the source archives `(H)`.
- Resource packs checked: Clouds 2 format 15 (missing two OGGs), Yeti 9, Space Race 6 with a wrapper folder and no root `pack.mcmeta`, City `advancements.zip` is a world backup `(H)`.
- Start instructions produced from observed signs/buttons/commands `(H/M)`.
- Known-broken and safe-to-test lists produced `(H/M)`.
- Outputs written only under `game-plugins/research/existing_maps/server_import/`.

## Blocked / not done by design

- **No live player test.** Command blocks were not triggered; no player joined. Runtime conclusions are from NBT, source `LastExecution`/`LastOutput`, command syntax and config `(H/M)`.
- **No resource pack/datapack enabled.** Per brief, nothing was turned on `(H)`.
- **Minigames 942 command blocks not audited.** Out of the four-world audit scope; A6 owns the arena world `(H)`.
- **No 1.21.1 parser execution.** FAIL classifications are based on the 1.20.5 item-component change and the 1.21 folder rename, not a live command run `(H/M)`.
- **No elytra/allow-flight live test.** Multiverse `allow-flight=false` + `enforce-flight=true` clears flight for survival/adventure players; creative players keep it. Elytra glide should be separate, but unconfirmed `(M)`.

## Key confirmed failures

1. Clouds 2: 30 pre-1.20.5 item-NBT boot commands fail; one `setblock 5 4 22` is missing its block argument `(H)`.
2. City Marathon: 7 no-target `/gamemode adventure` blocks fail `permissions.requires.player`; Yeti: 3 `(H)`.
3. TGTTOS packs are disabled and structurally incompatible with 1.21.1 (`functions` vs `function`; format 10 vs 48) `(H)`.
4. Space Race `resources.zip.zip` is not a valid pack; Clouds 2 pack references two missing OGGs `(H)`.
5. Multiverse forces survival and `allow-flight=false` for survival/adventure players; creative players keep flight `(H config/code; M player effect)`.

## Open questions

- Does Multiverse `allow-flight=false` block elytra glide, or only clear flight permission for non-creative players?
- Does Paper send per-world `resources.zip` to clients on a Multiverse switch?
- Are the 6 Clouds 2 kill planes and 2 Space Race kill planes intended to be global?
- Why do DecisionDome (2) and Minigames (942) have command blocks when the plan says 0? A6/A13 must handle this.

## Safety note

The review server remains read-only for this agent. No `stop`, `reload`, `mv import/unload`, `kill`, `fill`, `setblock`, `gamemode` or resource/datapack enable was issued. The only RCON commands used were `mv info`, `mv list`, `gamerule`, `execute in <world> run gamerule`, `version`, `plugins`, `tps`, `list`. `(H)`
