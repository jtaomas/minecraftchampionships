# The Build Mart cart — history, current equivalent, and an original supply cart

**Tier:** the old cart is **Tier 3 historical context** (non-current MCC). Current MCC has no cart. The supply-cart proposal is **original** and must be labelled as such in-game.

Source IDs match `README.md` / `REFERENCE_IMAGES.md`: **S1** wiki.gg Build Mart page, **S2** Fandom old-map page, **S3** MCC P25 Philza VOD, **S4** MCC 13 Illumina VOD, **S5** wiki images, **S6** current code.

---

## 1. What the old cart actually was

| Claim | Evidence | Confidence |
|---|---|---|
| The cart was a **rideable minecart on rails** inside the old indoor mart. | S4 2:30:42–2:30:45 shows a player seated in a plain minecart on a rail, first- and third-person; S1 History (MCC 14) says “Shopping cart traveling is replaced with elytra travel.” | **H** for rideable cart on rails; **M** for “vanilla minecart” vs a custom cart entity || It was **not a chest cart** and did not store blocks. | The cart in S4 2:30:45 is an empty vanilla minecart (no chest model); the player’s hotbar still holds the gathered blocks. | **H** visual; **M** that no alternate storage cart existed |
| It was **not** a shopping-list UI and did not track a list. | S1 describes a “shopping list book” only from MCC P25; the old map page (S2) describes the cart purely as travel. | **H** |
| Players could choose **cart or feet** at will. | S2: “players would either run on their feet or use the shopping carts to travel faster but with less control in the mart. The players could change their method of traveling at will.” | **H** |
| Carts were **faster but less controllable** than running. | S2, same sentence. | **H** |
| The old mart had **pitfalls**; falling in lost all gathered resources. | S2. | **H** |
| The old map was used from **MCC 3 through MCC 13**, plus Jingle Jam and All-Stars/Scuffed; **11 plays** total. | S2 appearances table (“Times Played 11”); S1 History says MCC 14 introduced the new resource map. | **H** |
| The cart was replaced in **MCC 14** by elytra travel. | S1 History (MCC 14): new map, resources on floating islands, elytra, central fan, launch pads/speed boosts, nether/coral resources, terracotta removed, Gold Builds added, time 12→10 min. | **H** |

### What it carried

**Nothing.** The cart was a vehicle, not a container. The player carried the blocks in their inventory. Losing the cart (or falling) did not delete the cart; falling into a pitfall deleted the **player’s** resources. This is the key correction to the modern assumption that a “shopping cart” is a stash or a list.

### How players used it

1. Enter the mart from the team build area.
2. Choose a rail line or run on foot.
3. Ride the cart to a department (`LUMBER`, `STONE`, `COLOURS`, `ORES`, …).
4. Exit the cart, mine the stall/aisle, and carry the blocks in the inventory.
5. Repeat, or run back; falling into a pitfall loses everything carried.

The cart was a **route commitment**: fast, fixed, less control. Running was slower but let you turn immediately. That trade-off is the interaction the owner remembers.

---

## 2. Why it was replaced

S1’s MCC 14 history lists the changes together:

- new resource-gathering map;
- resources on **floating islands**;
- **elytra** travel;
- giant **fan** in the centre;
- **launch pads** and **speed boosts** on each resource island;
- nether and coral resources;
- terracotta removed;
- Gold Builds added;
- game time 12 → 10 minutes.

The wiki does **not** state a design motive. The structural reason is clear and should be marked **M**: a rail-bound cart cannot serve resources at different heights. Elytra + fan + launch pads turn the mart into a 3D course. Keeping the cart would have meant either flat islands or a much larger rail network. The current map’s whole fantasy is flying between islands; a cart would fight it.

**Do not** restore cart travel as the main movement system. It would replace the elytra loop the current map is built around.

---

## 3. Current equivalent (verified)

| Old cart role | Current MCC equivalent | Evidence | Confidence |
|---|---|---|---|
| Move quickly through the mart | Elytra + central fan + launch pads + speed boosts | S1 Build Mart section; S3 27:52–28:10 shows flying between islands | **H** |
| Carry gathered blocks | Player inventory only | S3 book/hotbar frames; no cart or container visible in the sampled mart window; S1 never mentions one | **H** |
| Know what to gather | **Shopping list book** in each player’s inventory + material text at the plot | S1 Introduction; S3 27:34, 27:45–27:46 | **H** |
| Know what the target looks like | 3D reference on the plot pedestal | S1 Build Area; S3 27:34; S5 build-area image | **H** |
| Choose a build | Physical **Blueprint Gallery** (7 pedestals, 90 s rotation) | S1; S5 gallery image | **H** |
| Team storage | Five colour-coded chests in the build area (not a cart) | S1 Build Area | **H** |

**Verdict:** there is **no cart/container** in current MCC. The “shopping cart” survives only as the **shopping list book** and the **inventory**. Any cart we add is original, not a recreation.

---

## 4. Proposed original interaction: the **Supply Cart**

**One-line pitch:** the old travel cart returns as a **stationary team supply cart** parked at the build area. It carries the blocks the team has gathered, not the players. Travel stays elytra.

### 4.1 Why this fits the current map

- It preserves the current elytra/fan/launch-pad traversal; no rail network, no competing travel mode.
- It restores the old cart’s **“get the resources home safely”** feeling: the cart is at the plot, so a dangerous trip still risks what you are carrying, but delivered blocks are safe.
- It solves the real pressure of **three simultaneous builds**: the team can pool blocks without one player’s inventory becoming the bottleneck.
- It gives the shopping list a physical partner: the book says what is needed, the cart shows what has arrived.

### 4.2 Physical form

- A **chest minecart** on a short 5-block rail spur at the back of the team build area, beside the portal. Team-coloured banner on a fence post; a floating hologram above it.
- **Not rideable.** Right-click opens the cart. This is deliberate: a rideable cart would undermine the elytra map and the “carry it home” risk.
- **Not breakable**, not movable, no hoppers, no comparator output. Team-only access.
- One cart per team, shared by all members. In solo, the same cart exists for the one player.
- Visual state: the cart’s hologram shows `CART 34/58` and a small list of the top three missing materials. The minecart itself can hold a visible chest model (chest minecart) so it reads as storage at a glance.

### 4.3 Rules

| Rule | Value | Rationale |
|---|---|---|
| Capacity | 27 slots (one chest) | Enough for 3 builds’ worth of common blocks; still forces trips |
| Accepted items | Only materials on the **active manifest** (3 regular builds + gold, if active) | Keeps the cart a shopping cart, not a dump chest |
| Rejected items | Action bar `Not on the list: <item>`; item stays in inventory | Prevents clutter and accidental dumping |
| Access | Team members only; others get `This cart belongs to <team>` | Prevents griefing |
| Survival | Cart contents persist through death/void; cleared at game end | Delivered blocks are the reward for surviving the trip |
| Withdrawal | Normal chest clicks; shift-click quick-move | Vanilla, no new UI to learn |
| Manifest update | On selection, completion, rotation, gold spawn/expiry | The cart always reflects the current three builds |
| Surplus | Items for a completed/rotated build are marked `SURPLUS` in gray; they stay until withdrawn; they do **not** count toward the new manifest | Avoids deleting a team’s work |
| Full cart | Deposit denied with `Cart full — withdraw or finish a build` | Clear failure mode |
| Scoring | Cart contents are never scored; only placed blocks count | No “bank” exploit |

### 4.4 Click flow

1. **Right-click cart** → opens a 27-slot chest GUI titled `Supply Cart — <team>`.
2. **Shift-click a manifest item** from inventory → moves the stack into the cart; hologram updates.
3. **Shift-click a cart item** → moves it back to inventory; if inventory is full, deny with action bar.
4. **Right-click cart with a manifest item** → deposits one item (vanilla behaviour); useful for precise counts.
5. **Look at cart** → hologram `CART 34/58` + `Missing: 6 Quartz, 4 Diorite, …`.
6. **Book last page** → `Cart manifest` with `have/need` per material; clicking the page arrow returns to build pages.
7. **Build completes** → leftover items for that build are marked surplus; the hologram switches to the next build’s needs. No automatic withdrawal.

### 4.5 Data model

```java
class SupplyCart {
    UUID teamId;
    Inventory inventory;                  // 27 slots, custom holder
    Map<Material, Integer> counted;       // only manifest materials
    Map<Material, Integer> surplus;       // materials no longer needed
    long lastManifestRevision;
}

class CartManifest {                      // rebuilt from active builds
    Map<Material, Integer> required;      // sum of all active build counts
    Map<Material, Integer> delivered;     // counted from cart
    Map<Material, Integer> missing;       // max(0, required - delivered)
    int totalRequired, totalDelivered;
}
```

- The cart inventory is the source of truth; `counted`/`delivered` are derived on open/close/deposit.
- Do not store counts in the book item; the book is a view.
- Persist the cart in the game state, not in the world block, so a server restart mid-game can restore it (or safely clear it at game end).

### 4.6 Edge cases

| Case | Behaviour |
|---|---|
| **Solo** | One cart, one team; manifest is the three active builds; no team-access check beyond the solo player. |
| **Late join** | Cart is already there; new member can open it immediately; book is issued with the current manifest. |
| **Disconnect** | Cart stays; contents stay; no one can open it while the team has no online members? Prefer: any team member can open; offline members do not block. |
| **Three simultaneous builds** | Manifest is the union of all three; one cart serves all; completing one removes its lines from `missing` and marks leftovers surplus. |
| **Build rotation** | Unselected gallery slots rotate; active builds do **not**; cart manifest does not change until a build is completed/cancelled. |
| **Repeat rule** | If a completed build is reselected before gallery rotation, its materials become required again; surplus for that material is re-counted automatically. |
| **Gold spawn** | Gold materials are added to the manifest only while the gold is active; at expiry they become surplus. |
| **Gold failure** | Placed gold blocks are removed; cart materials for gold become surplus; no refund. |
| **Cart full** | Deposit denied; action bar tells the player to withdraw or finish a build. |
| **Item no longer needed** | Marked surplus, not deleted; player can withdraw; it does not count toward `delivered`. |
| **Game end** | Cart is cleared with the arena; contents are not scored. |
| **Exploit: deposit then break** | Cart is not a block; cannot be broken. Placing blocks still consumes inventory, so no duplication. |
| **Exploit: alt account** | Team check by UUID; spectators cannot open. |

### 4.7 Build checklist for the cart

- [ ] Chest minecart entity is invulnerable, persistent, and removed in `cleanupArena`.
- [ ] Right-click opens a custom 27-slot holder; non-team players are denied.
- [ ] Only manifest materials are accepted; rejection message is clear.
- [ ] Hologram shows `delivered/required` and the top three missing materials.
- [ ] Cart survives void/death; contents are not stripped by `voidCheck`.
- [ ] Manifest rebuilds on selection, completion, cancellation, gold spawn/expiry.
- [ ] Surplus is gray, retrievable, and not counted.
- [ ] Full-cart and full-inventory paths are tested.
- [ ] Cart is not rideable, not breakable, not hopper-accessible.
- [ ] Cart is cleared at game end with no orphan entities.

### 4.8 Optional cosmetic: the **Gallery Trolley**

If the owner wants a visible nod to the old cart without changing gameplay: when a blueprint is selected, a small minecart rolls along a 6–8 block rail from the gallery pedestal to the team’s plot and “delivers” the reference hologram. It is a 2-second animation, no storage, no travel. This is a nice title-screen/theatre touch, but it is **not required** and should not delay the core loop.

---

## 5. What not to build

- **Do not** add rideable carts to the mart. The current map’s traversal is elytra + fan + launch pads; a rail network would flatten the islands or add a second, worse movement system.
- **Do not** make the cart a second inventory that follows the player. That removes the “carry it home” risk and trivialises the void.
- **Do** keep the cart stationary at the plot. The danger is the trip, not the storage.
- **Do** keep the book as the primary list. The cart is a physical complement, not a replacement UI.

---

## 6. Open questions

1. Did the old cart have a chest variant or a custom “shopping cart” entity? Visual evidence says plain minecart; no storage observed. **M/L.**
2. Could players spawn/place carts, or were they all pre-placed? Not observed. **L.**
3. Was the old cart on a loop or a point-to-point line? The sampled frame shows a straight aisle; the full network is not mapped. **L.**
4. Did the old cart have a speed toggle/boost? S2 says faster/less control, no toggle mentioned. **M.**
5. In current MCC, does a selected build survive the 90 s gallery rotation? The wiki says the copy is sent to the team area; likely yes, but not explicitly stated. **M.**
6. Does the current book list materials for the Gold Build? The wiki says no reference; the book may still list materials. **L.**
7. Are all teams offered the same Gold Build at the same time? Not stated. **L.**
8. Are cart contents in our plugin allowed to include crafted blocks (e.g., Glass) or only raw materials? Design choice: final required blocks, because the book lists final blocks.
