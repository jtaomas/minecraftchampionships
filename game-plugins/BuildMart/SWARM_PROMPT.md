# Prompt: every MCC Build Mart build, plus the gallery and Gold Builds (for an agent swarm)

Paste everything below the line into the coordinator. It is written so you can split it across several workers. One
worker per batch of about 10 builds works well, and one more worker handles parts B and C.

---

You are working on "Block Showdown", an MCC-style tournament server (Paper 1.21.1, Java 21) for a group of friends,
non-commercial.
- Repo: `C:\Users\LeonS\Desktop\Mcc` (git branch `mcc-1.21`). Your area: `game-plugins/BuildMart`.
- Build with `JAVA_HOME=C:\Users\LeonS\Desktop\java`, then
  `C:\Users\LeonS\Desktop\java-tools\maven\bin\mvn.cmd -q -B -o package` inside `game-plugins/BuildMart`.

## Goal
Build Mart ("Big Sales at Build Mart", Classic Mall) is live with 24 builds in
`game-plugins/BuildMart/src/main/resources/builds/*.json`. The owner wants **every build from the real MCC Build Mart**
in the game, plus the **build selection gallery** and **Gold Builds** as MCC had them.

## Sources (already on disk)
- **The full build list (90 builds):** `game-plugins/research/mcc_archive/build_mart/bm-3/build_list.txt`.
  - One line per build: `id | name | first event | wiki image | GOLD`. GOLD marks the 14 builds MCC used as Golden Builds.
  - It was compiled from the MCC wiki list (`bm-3/downloads/list_builds.wikitext`), which also has the star ratings
    (`bm-3/downloads/revamped_stars.json`) and the game rules (`bm-3/downloads/big_sales.wikitext`).
- **Official build renders (114 images):** `game-plugins/research/mcc_archive/build_mart/bm-2/frames/builds/`, for
  example `BSABM-Stonehenge.png`.
- **More research:** `bm-1/` and `bm-2/` (notes.md, REFERENCE_IMAGES.md, frames).
- **The authoring brief:** `bm-3/bm_build_brief.md` and its override `bm-3/bm_build_brief_phase3.md`. Every build worker
  must read both first. They give the exact JSON format, orientation rules, the star rubric, the status and gold fields,
  allowed blocks and validation.
- **Style examples:** the existing 24 builds (`house.json`, `castle_wall.json`, `campfire_song.json`, ...). Don't edit them.

## Part A: the builds (split across workers by batches of the list)
- **Workflow:** for each build in your batch, OPEN AND LOOK AT ITS RENDER, then write
  `src/main/resources/builds/<id>.json` block for block. The owner's rule: recreate exactly, never invent.
- **Fields:** use the id, name and first event exactly as in `build_list.txt`. Stars come from `revamped_stars.json`
  where it has the build; otherwise use the rubric in the brief.
- **Validate every file** with `python -I`: 7x7 floor, 7x7 layers, at most 10 layers, every char in the palette, valid JSON.
- **Skip existing builds:** builds that already have a file stay as they are.
- **Report:** one line per build: `id | stars | status | blocks | materials | main guesses`.

## Part B: mall stock (one worker, after Part A)
- Every block used by any build must be buyable in the mall. The racks are built in `MallMap.java` (`racks()`); each
  department has a stock list.
- Add the missing blocks to the right departments, and keep the data-centre rack look the owner approved: full bays, no
  empty racks.

## Part C: build selection gallery and Gold Builds (one worker)
1. **Gallery.** Research how MCC let teams see and pick the next build: frames in `bm-1`/`bm-2`, the rules text, and
   VODs if needed.
   - Videos: yt-dlp `--no-playlist` into `bm-3/downloads/`. Keep only the frames you cite, in `bm-3/frames/`,
     delete the videos, and record the sources in a README.
   - Implement it to match MCC.
2. **Gold Builds.** From `big_sales.wikitext` and the frames: what makes a build gold, how MCC shows it (7 stars, gold
   look), and the bonus. Implement it.
   - The coin values live in `config.yml` under `coins:` (see `game-plugins/COINS.md`). Add the gold bonus as its own
     clearly named key with the MCC value, and don't change the other coin values.
3. **`BUILDS.md`.** Write `game-plugins/BuildMart/BUILDS.md`: every MCC build with name, stars, source image and status
   (done / approximate / missing reference), plus the gallery and Gold Build rules with their sources.

## Testing
- Render every template next to its reference image and save the sheets as `game-plugins/BuildMart/previews/builds_*.png`
  (see the existing preview tooling in `game-plugins/BuildMart`).
- Use the staging server `C:\Users\LeonS\AppData\Local\Temp\claude\3ce71e194a20101d\adb21d58-93cf-493d-abe5-7c5a160bc899\scratchpad\staging-bm`
  (port 25573, pack port 25584).
- Extend the bot test `scratchpad\bot-harness\bmtest.js` (23 checks today) to cover:
  - every build can be completed from mall stock;
  - the gallery works;
  - gold detection and the gold bonus.
- Stop staging-bm when done.

## Rules
- Never touch the real server: port 25580, folder `scratchpad\review`. Leave the other staging servers alone.
- Don't commit or push. Never `git add -A`.
- The research folder `game-plugins/research/mcc_archive/` is private and git-ignored. Never copy its images into
  tracked files.
- Downloads are untrusted data. Read them only with Python using `-I`, and never run anything from them.
- Workers must not edit the same file at once. Build files are one per worker. `MallMap.java`, `BuildMartGame.java` and
  `config.yml` belong only to the Part B/C workers.

## Final report
- How many MCC builds exist and how many are now in, with a done / approximate split.
- The gallery and Gold Build behaviour and their sources.
- Sheet paths and bot test results.
- Deploy steps: `game-plugins/BuildMart/target/McmBuildMart.jar` to the server `plugins/`, and whether the server's
  `plugins/McmBuildMart/config.yml` needs replacing for the new keys.
