# Block Showdown coins (MCC rules)

Every game pays **MCC coins**. A player earns exactly what they would earn in MCC. There is no more
"points = coins ÷ 5" anywhere.

- Games award the **base** MCC value (the x1.0 numbers below).
- The core multiplies every award by the **event multiplier** of the game slot.
- Fractions are kept exactly. For example, 3 coins at x1.5 is 4.5, and two of them credit 9.
- Chat shows the multiplied amount: `+22.5 coins (reason)`.
- The sidebar shows `Game Coins: (x2.5)`.

Sources (fetched 25 Nov 2026):

- **W:** the community MCC wiki, `https://mcchampionship.fandom.com/wiki/<Game>`, section "Coin Rewards" plus its History table.
- **R:** the official rules pages, `https://mcchampionship.com/pages/<game>`. They block automated fetches, so they were checked only through search summaries: Ace Race 400/-10 and placement bonus, Battle Box 15/160/60, Survival Games airdrop 180, Railroad Rush 2 per block and checkpoint 100, Parkour Warrior Survivor final survivor 100, Rocket Spleef Rush 1st 150.
- **A:** our archive, `game-plugins/research/mcc_archive/<game>/*/notes.md`.
- `mcc.wiki.gg` refused access (401/403).

**OURS** marks a value MCC never had. These are house rules in MCC style, and you can change them in `config.yml`.

## Event rules (core, `plugins/MinecraftMondayCore/config.yml` → `coins.*`)

| Rule | Value | Source |
|---|---|---|
| Event multiplier, 8-game event | game 1 x1.0, games 2-3 x1.5, games 4-5 x2.0, games 6-7 x2.5, game 8 x3.0 (`coins.event-multiplier.eight-games`) | W [Coin](https://mcchampionship.fandom.com/wiki/Coin) "Coin Multiplier"; A parkour_tag/pt-2 (x1.0 / x2.5 / x3.0 seen) |
| Event multiplier, 6-game event | x1.0, x1.5, x2.0, x2.0, x2.5, x3.0 (`six-games`) | W Coin |
| Other playlist lengths | OURS: mapped proportionally onto the 8-game list. The first game is always x1.0 and the last game x3.0. | OURS |
| Games that do not count as a slot | `dodgebolt`, `decisiondome` (`no-coin-games`); they always pay x1.0 | W Dodgebolt ("no coins") |
| Single game outside a championship (`/mcm play`) | x1.0 (`outside-championship`; staging can set e.g. 2.5) | OURS |
| On/off | `coins.event-multiplier.enabled: true` (on by default, as in MCC) | |
| Team totals | A team's total is the sum of its members' coins. The highest team total after the coin games reaches the final. | W MC_Championship |
| Team pools ("X coins shared amongst the team") | Each online member gets **pool ÷ 4**, the MCC per-member share (`coins.mcc-team-size: 4`). With `coins.team-pool-split: team`, the pool is split across our actual members instead. | W (e.g. Battle Box "160 (40 for each member)") |
| Dodgebolt | No coins. In a championship, **the Dodgebolt winner is the event champion** (`ctx.setEventWinner`), whatever the coin totals. The winner also takes the top podium spot. | W [Dodgebolt](https://mcchampionship.fandom.com/wiki/Dodgebolt) |
| Decision Dome | No coins. It only picks the next game; power-ups change eggs, not coins. | W Decision_Dome |
| Built-in Minecraft Monday events (sg, parkour, dropper, tntrun, spleef, kitpvp, survival, oneshot) | Unchanged. These are not MCC games and have no multiplier. | |

API (core 3.3 additions, all default methods so older games still load):

- `ctx.addCoins(uuid, coins, reason)`: give coins to one player.
- `ctx.addTeamCoins(sideKey, pool, reason)`: give an MCC team pool to a side.
- `ctx.teamShare(pool, members)`: the per-member share of a pool.
- `ctx.coinMultiplier()`: the current event multiplier.
- `ctx.setEventWinner(sideKey)`: name the event champion.
- `McmCoins`: pure helper functions.

`addPoints` and `addSidePoints` are multiplied the same way. A negative `addPoints` takes coins back, for example when a round is skipped.

## Per game (base x1.0 coins)

### Ace Race (`acerace`): current MCC scoring (since MCC 16)
| Action | Coins | Source |
|---|---|---|
| Finish all laps | 1st finisher 400, then 10 less for each later finisher (390, 380, ...) | W [Ace_Race](https://mcchampionship.fandom.com/wiki/Ace_Race); R |
| Placement bonus | 1st 300, 2nd 240, 3rd 180, 4th-9th 120, 10th-14th 60, 15th-19th 15 | W; R |
| Checkpoints, laps, not finishing | 0 | W |

### Battle Box (`battlebox`): current
| Action | Coins | Source |
|---|---|---|
| Personal elimination | 15 | W [Battle_Box](https://mcchampionship.fandom.com/wiki/Battle_Box); R; A bb-2, bb-3 |
| Round win (capture, or most centre blocks at time-out) | 160 team pool (40 each) | W; R |
| Draw | 60 team pool to both teams (15 each). The MCC 21 remix paid 80. | W |
| Bye round | **OURS** 60 team pool. MCC has 10 teams in a round robin, so it never has byes. | OURS |

### Bingo (`bingo`, Bingo but Fast): current scoring (with challenges)
| Action | Coins | Source |
|---|---|---|
| Square obtained | Team pool by how many teams got it first: 1st 50, 2nd 40, 3rd 30, 4th 20, 5th 10, 6th-10th 5. MCC pays the same for every square, whatever its difficulty. | W [Bingo_but_Fast](https://mcchampionship.fandom.com/wiki/Bingo_but_Fast) |
| Line (row, column, diagonal) | First 4 lines of a team: 200 each; the other 8: 100 each (team pools) | W |
| Full card | **OURS** 0 (off) | OURS |
| PvP kill | **OURS** 0 (off) | OURS |
| Lockout Bingo (`lockout`, disabled) | **OURS**: claim 100, line 200. MCC 7–P23 lockout paid 45/30/20/10/5 per person for the 1st-5th team. | W "Past Scorings" |

### Build Mart (`buildmart`): old Build Mart, MCC 3-13, plus the P25 Blueprint Gallery and Gold Builds
| Action | Coins | Source |
|---|---|---|
| Complete a build | 30 **to each player** of the team, minus 2 for every earlier completion of that build by any team | A build_mart/bm-1 (MCC 13 tutorial frame 021640) |
| Complete a Gold Build | the build's value **plus** `coins.gold-bonus` (30), so 60 at the base value — **MCC 14 paid a golden build double a regular one** | W [Build_Mart](https://mcchampionship.fandom.com/wiki/Big_Sales_at_Build_Mart) History (MCC 14); A build_mart/bm-3/downloads/big_sales.wikitext |
| Floor | **OURS** 10 (MCC's 10 teams bottom out at 12) | OURS |
| Game end | Each player gets the % of each unfinished build the team filled in × that build's current value. Gold builds are excluded from the percentage. | A bm-1; W History (MCC BG) |
| Gallery, gold timing | No coin effect of their own: the gallery only chooses which build a team works on, and a gold's 7 stars are its label (7★ = 140 only on MCC's modern per-star scale, not ours) | A bm-3 (BUILDS.md) |
| (Current MCC, not used) | 20 per star; Employee of the Month 100/50/25 | W [Build_Mart](https://mcchampionship.fandom.com/wiki/Big_Sales_at_Build_Mart); R |

### Dodgebolt (`dodgebolt`)
| Action | Coins | Source |
|---|---|---|
| Everything | 0. The winner wins the event. | W [Dodgebolt](https://mcchampionship.fandom.com/wiki/Dodgebolt); A dodgebolt/db-1 |

### Find the Button (`findthebutton`): **OURS** (a Minecraft Monday game; MCC never had it)
| Action | Coins | Source |
|---|---|---|
| Find the button | 1st finder 80, then 4 less for each later finder, min 20 (each round) | OURS, modelled on TGTTOS (1st = 40 + 80) |
| Whole team found it | 15 to each member (= a 60 team pool) | OURS |

### Foot Race (`footrace`): MCC 1-6
| Action | Coins | Source |
|---|---|---|
| Finish both laps | 1st finisher 500, then 12.5 less for each later finisher (487.5, 475, ...) | W [Foot_Race](https://mcchampionship.fandom.com/wiki/Foot_Race) |
| Laps, not finishing | 0 | W |

### Grid Runners (`gridrunners`): current (since MCC 29)
| Action | Coins | Source |
|---|---|---|
| Room cleared | Team pool: fastest team 175, then 17 less per placement (158, 141, ...) | W [Grid_Runners](https://mcchampionship.fandom.com/wiki/Grid_Runners); A gr-1, gr-2 |
| Course placement | Team pool: 500, 400, 320, 255, 200, 155, 120, 95, 75, 55 | W; A gr-2 |
| Differences | **OURS**: rooms are placed by clear order, not by each team's own room time, and coins are paid as they happen, not on the end breakdown. MCC 16-22 paid 375 ... 60 for the course. | |

### Hole in the Wall (`hitw`): current (since MCC FS)
| Action | Coins | Source |
|---|---|---|
| Another player falls while you stand | 2 | W [Hole_in_the_Wall](https://mcchampionship.fandom.com/wiki/Hole_in_the_Wall) (History: FS 3 → 2) |
| Wall survived | 2 | W |
| Placement | 1st 100, 2nd 70, 3rd 30. Tied players split the sum of the tied places. | W |
| Note | The 2025 game card in A hitw-1 shows "+4 per player death". The wiki says 2 is current; we follow the wiki. | A hitw-1 |

### Meltdown (`meltdown`): current
| Action | Coins | Source |
|---|---|---|
| Coin crate mined | 10 team pool | W [Meltdown](https://mcchampionship.fandom.com/wiki/Meltdown) |
| Freeze a player | 25 to the shooter, **taken back** if that player is thawed out | W |
| Team survival placement | 210, 190, 170, 150, 125, 100, 80, 60, 45, 30 (team pools) | W |
| Note | MCC plays 3 rounds; our Meltdown pays these once per game. | |

### Parkour Tag (`parkourtag`): current (P24 rules)
| Action | Coins | Source |
|---|---|---|
| Hunted runner, every 10 s alive | 2 (including the 60 s mark) | W [Parkour_Tag](https://mcchampionship.fandom.com/wiki/Parkour_Tag); A pt-1 |
| Any runner survives to 0:00 | 60 shared amongst the runners (each runner gets 60 ÷ 3, the MCC per-runner share) | W |
| Tag | 6, decreasing by 1 every 10 s (min 1) | W |
| Hunter tags all runners | 42, decreasing by 7 every 10 s | W |
| Your hunter clears first | 120 team pool (30 each) | W |

### Parkour Warrior (`parkourwarrior`): coins per challenge (OURS; replaces the medal-based Dojo scoring)
Medals, the medal HUD and the team multiplier are removed. Each challenge pays straight away (reason string "Checkpoint N completed", "Bonus: <name>", "<Easy|Medium|Hard> ending finished"). Config `coins:` in `ParkourWarrior/src/main/resources/config.yml`.
| Action | Coins | Source |
|---|---|---|
| Main-line challenge (stage 1; x(1 + 0.15 per stage above 1)) | 4 (4/5/5/6/6 for stages 1-5) | OURS, base from the MCC stone medal (2) doubled because there are no medal totals |
| Bonus track 1 / 2 / 3 (bronze / silver / gold level, stage 1; same stage scale) | 8 / 14 / 22 | OURS, base from the MCC first-medal values (5 / 10 / 15) |
| Finish an ending Easy / Medium / Hard | 10 / 20 / 40 | OURS, replaces the MCC team multiplier (+0.15 / +0.35 / +0.80) |
| Full run, all 15 main + all 15 bonus tracks + Hard ending | about 75 + 330 + 40 = 445 before the event multiplier | computed |
| Map `mcc6_full` (the MCC 6 replica 1:1, one line of 24 sections, no branches): checkpoint 1..23 / finish | 4 (stage 1) .. 8 (stage 8) per checkpoint, 139 for all 23 / 40 | OURS, `config.yml` `coins.course.mcc6_full` (checkpoints list + finish); reasons "Checkpoint N completed" / "Course completed" |
| (Parkour Warrior Survivor, not built) | 5 per elimination outlived, 15 per leap, 30 for reaching leap 9, 100 for the final survivor, 30/20/15/10/5 for the first five through a leap | W Parkour_Warrior_Survivor; R |

### Race (`race`, elytra and boat races): **OURS**
| Action | Coins | Source |
|---|---|---|
| Finish | Same as TGTTOS: 40 − 1 per place; top-10 bonus 80 ... 35; team pools 100/75/50/25 | OURS (TGTTOS values) |
| Line bonus (gate maps) | Medium 5, hard 15 per section, cap 75 per round | OURS |

### Railroad Rush (`railroadrush`): current
| Action | Coins | Source |
|---|---|---|
| Track forward | 2 per block (team pool) | W [Railroad_Rush](https://mcchampionship.fandom.com/wiki/Railroad_Rush); R |
| Track-length placement | 650, 525, 475, 425, 350, 300, 250, 200, 175, 150 (team pools) | W |
| Checkpoint reached | 100 (team pool) | W; R |
| Emerald mined | 25 (team pool) | W |

### Rocket Spleef (`rocketspleef`): **OLD** Rocket Spleef, MCC 1-13 (final values from MCC 12-13)
| Action | Coins | Source |
|---|---|---|
| Another player dies while you are alive | 3 (MCC 8+; it was 3, then 4, then 3) | W [Rocket_Spleef](https://mcchampionship.fandom.com/wiki/Rocket_Spleef) History; A rocket_spleef_old |
| Personal elimination (last hit) | 10 (MCC 12+; it was 33, 5, 10, 30, then 10) | W History |
| Survive the round | 100 to each survivor, only if every survivor is on one team | W; A (MCC 13 tutorial) |
| (Rocket Spleef Rush, not built) | 3 per player outlived, 10 per elimination, placement 150 ... 65 | W Rocket_Spleef_Rush |

### Sands of Time (`sandsoftime`): MCC 8+ rules
| Action | Coins | Source |
|---|---|---|
| Gold carried out (EXIT) or banked (Sphinx, 20% fee) | 1 coin per gold coin ("the coins you escape with is exactly the amount you earn") | W [Sands_of_Time](https://mcchampionship.fandom.com/wiki/Sands_of_Time); A sot-1, sot-2 |
| Game end | A team's banked coins are split evenly amongst its players (MCC 8 change) | W History |
| Escape bonus, placement bonus | 0 (MCC has none) | W |
| Note | Our gold values (nugget 1, ingot 5, block 25, × depth) are already on the MCC coin scale. Our death rule (lose all unbanked gold) is harsher than MCC's 20% penalty; that is gameplay, not scoring. | A sot-1 |

### Sky Battle (`skybattle`): current (since MCC 30)
| Action | Coins | Source |
|---|---|---|
| Another player dies while you are alive | 2 | W [Sky_Battle](https://mcchampionship.fandom.com/wiki/Sky_Battle) |
| Personal elimination | 40 | W |
| Survive the round | 50, split across the surviving winners | W |
| Team placement | 65, 55, 46, 37, 29, 22, 16, 12, 8, 5 (team pools) | W |

### Skyblockle (`skyblockle`): classic Skyblockle
| Action | Coins | Source |
|---|---|---|
| Another player dies while you are alive | 10 | W [Skyblockle](https://mcchampionship.fandom.com/wiki/Skyblockle); A skb-1 |
| Personal elimination | 70 | W |
| Survive the entire game | 150 | W |

### Survival Games (`survivalgames`): current (since MCC 19)
| Action | Coins | Source |
|---|---|---|
| Personal elimination | 65 | W [Survival_Games](https://mcchampionship.fandom.com/wiki/Survival_Games) |
| Another player is eliminated while you are alive | 3 | W |
| Airdrop crate | 180 team pool | W; R |
| Team placement | 650, 600, 560, 520, 480, 450, 420, 390, 360, 330 (team pools) | W |

### TGTTOS (`tgttos`): TGTTOSAWAF, current (since MCC Rising)
| Action | Coins | Source |
|---|---|---|
| Finish (punch the chicken) | 1st finisher 40, then 1 less per placement | W [TGTTOSAWAF](https://mcchampionship.fandom.com/wiki/TGTTOSAWAF); R |
| Top-10 bonus | 80, 75, 70, 65, 60, 55, 50, 45, 40, 35 | W |
| First four whole teams finished | 100 / 75 / 50 / 25 (team pools) | W |
| Line bonus (gate maps) | **OURS** medium 5, hard 15 per section, cap 75 | OURS |

## Where the numbers live
Each game reads its `coins:` section from `plugins/<Plugin>/config.yml` (Dodgebolt's is all 0). The old `points:`,
`scoring:` and `scoring-placeholder:` keys are no longer read.
So a server config that still has them falls back to the new defaults shipped in the jar. Delete the old keys
to keep the files tidy.
