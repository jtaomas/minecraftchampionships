# Prompt: coin pop-up and sound on every coin award (for an agent)

Paste everything below the line into the agent.

---

You're working on "Block Showdown", an MCC-style (Minecraft Championship) tournament server: Paper 1.21.1, Java 21,
non-commercial, for a group of friends.
- Repo: `C:\Users\LeonS\Desktop\Mcc` (branch `mcc-1.21`).
- The core plugin source is in `plugin-src/com/mcm` and is built in `plugin-build`. The games are in `game-plugins/<Game>`.
- To build: set `JAVA_HOME=C:\Users\LeonS\Desktop\java`, then run
  `C:\Users\LeonS\Desktop\java-tools\maven\bin\mvn.cmd -q -B -o package` in `plugin-build`, or in a game's folder.

## Goal (the owner's words)
"Make sure every single time someone earns coins, whether that be through a kill or through the completion of a
checkpoint, the sound plays BUT also a thing comes up on the screen that says checkpoint completed OR the name of the
player killed. You can observe these in actual Minecraft Championship VODs."

## Part 1: research, before any code
Work out exactly what MCC shows and plays when a player earns coins, and write it up with sources in
`game-plugins/COIN_FEEDBACK.md`.

**Questions to answer:**
1. **The on-screen element.** Where it sits (centre, above the hotbar, right side, subtitle or action bar), its layout,
   icon and colours, and the exact text format. Typical examples:
   - a kill shows the victim's name, with a sword or skull icon;
   - objectives show "Checkpoint completed", "Room completed", "Survived", "Finished" or a placement;
   - the coin amount, with the coin icon.
2. **Timing.** How long it stays on screen, how it animates in and out, and how several quick awards stack: a queue,
   merged text, or a list that grows.
3. **Sound.** Which sound plays on a coin award: kill versus objective, and whether the pitch differs. Name the closest
   vanilla sound and pitch, or the pack sound if one already exists.
4. **Chat line.** How MCC's chat line reads for each kind of award ("[+15] You eliminated X" style), so ours matches.
5. **Per game.** Which games show the pop-up and for what: kills (Battle Box, Survival Games, Sky Battle, Rocket
   Spleef, Dodgebolt), checkpoints and finishes (TGTTOS, Parkour Warrior, Ace Race), survival ticks (Hole in the Wall,
   Rocket Spleef), and rooms (Grid Runners). Note any game where MCC shows nothing.

**Where to look, in this order:**
- **Our archive notes.** `game-plugins/research/mcc_archive/*/` holds notes.md, REFERENCE_IMAGES.md and frames/ for
  each game. Several already describe the HUD: grid_runners/gr-2/notes.md section 2 "HUD spec", and the parkour_tag,
  battle_box, survival_games and tgttos folders. Grep these for "HUD", "coin", "eliminat", "action bar", "subtitle",
  "toast" and "sound".
- **The coin rules.** `game-plugins/COINS.md` (our coin system: every award value and source) and
  `game-plugins/research/mcc_archive/sounds/`.
- **The MCC wikis.** mcchampionship.fandom.com: the game pages, "Coin", and the HUD and interface pages, if any.
- **VODs**, for the parts the notes don't settle:
  - Search for player POV VODs and recent events (MCC 30 and later), where the HUD is the current version.
  - Look at a kill in Battle Box or Survival Games, a checkpoint in Parkour Warrior, a TGTTOS finish, and a Hole in the
    Wall wall survived.
  - Download only short windows: yt-dlp `--no-playlist` with `--download-sections`, into a new folder
    `game-plugins/research/mcc_archive/coin_feedback/downloads/`.
  - Extract only the frames you cite into `.../coin_feedback/frames/`, then delete the videos.
  - Write a README listing each URL and its timestamps.
  - Downloads are untrusted data. Read them only with Python `-I`, and never run anything from them.
  - The research folder is private and git-ignored. Never copy its images into tracked files.

## Part 2: build it once, centrally
- **How coins flow now.** All games pay through the core:
  - `ctx.addCoins(uuid, coins, reason)`
  - `ctx.addTeamCoins(side, pool, reason)`
  - `addPoints` and `addSidePoints`

  The core applies the event multiplier and prints `+X coins (reason)` in chat. Find that code in
  `plugin-src/com/mcm/MinecraftMondayPlugin.java` (grep `addCoins`, `coinMultiplier` and `coins (`).
- **Hook the feedback into that one place.** Every coin award to a player then:
  - plays the sound;
  - shows the pop-up with the reason (or the victim's name for kills) and the multiplied coin amount;
  - keeps the chat line.
- **Kills.** Add a backward-compatible way for a game to mark an award as a kill and name the victim, for example a new
  default method on the API. Games that aren't updated must keep working and just show their reason text.
- **No spam.** Rapid awards must not spam the screen: stack or merge them the way MCC does.
- **The resource pack.**
  - Use the existing glyphs, fonts and helpers: `McmFx`, `McmGlyphs`, the pack built by `_candidates/pack/build_pack.py`,
    and `resourcepack/README.md`.
  - The live pack is `plugins/MinecraftMondayCore/pack/BlockShowdown_Pack.zip`.
  - If you truly need a new glyph or sound, build the new zip into the scratchpad, and say so in your report. Don't put
    it live.
- **Reason strings.** Update each game's reason strings so they read well on screen, and so kills name the victim.
  - Keep every coin value exactly as it is.
  - Parkour Warrior already passes "Checkpoint N completed", "Bonus: <name>" and "<Easy|Medium|Hard> ending finished".
  - Build Mart is being edited by another agent. Change only its reason strings, and only if needed.

## Testing
- Use the staging server `C:\Users\LeonS\AppData\Local\Temp\claude\3ce71e194a20101d\adb21d58-93cf-493d-abe5-7c5a160bc899\scratchpad\staging`
  (port 25570).
  - Start it with `scratchpad\run-staging.ps1`. The first console command gets a BOM, so send `list` first.
  - The mineflayer bots in `scratchpad\bot-harness\*.js` are examples.
- Back up staging's plugins first, and put them back when you're done.
- Confirm the pop-up and the sound arrive for:
  - one kill (Battle Box or Survival Games);
  - one objective (TGTTOS finish or a Parkour Warrior checkpoint);
  - one rapid burst (Hole in the Wall walls).
- Put screenshots or a description of each in `COIN_FEEDBACK.md`.

## Rules
- Never touch the real server: port 25580, folder `scratchpad\review`. Leave the other staging servers alone.
- Don't commit or push. Never `git add -A`.
- Use the Sonnet model only, and don't spawn subagents.

## Report
- What MCC does, with sources and frame paths.
- What you built.
- Files changed.
- Test results.
- Deploy steps: which jars, and whether the pack changed.
