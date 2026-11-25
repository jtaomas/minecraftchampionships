#!/usr/bin/env python3
"""Build the Parkour Tag exports: commands.csv, signs.csv, blocks.csv, checkpoints.csv, entities.csv."""
import csv, collections, re, os

HERE = os.path.dirname(os.path.abspath(__file__))
rows = list(csv.DictReader(open(os.path.join(HERE, 'pt_commands.csv'), encoding='utf-8')))

# ---- role classifier -------------------------------------------------------
def classify(c):
    cmd = c['command'].strip()
    if not cmd:
        return ('broken/empty', 'No command', 'chain placeholder', 'BROKEN: empty command block', 'H')
    if 'firework_rocket' in cmd:
        if 'kill_parkourtag2' in cmd:
            return ('celebration', 'Spawns a corner firework when the hunter scores a kill', 'repeating, kill_parkourtag2=1', 'HIGH: old item NBT FireworksItem{id,Count,tag} is rejected by 1.21.1', 'H')
        if 'death_parkourtag=0' in cmd:
            return ('celebration', 'Firework over each surviving runner at round end', 'win chain', 'HIGH: old item NBT FireworksItem{id,Count,tag} is rejected by 1.21.1', 'H')
        if 'level=7' in cmd:
            return ('celebration', 'Firework over the hunter at round end', 'win chain', 'HIGH: old item NBT FireworksItem{id,Count,tag} is rejected by 1.21.1', 'H')
        return ('celebration', 'Firework', 'win chain', 'HIGH: old item NBT FireworksItem{id,Count,tag} is rejected by 1.21.1', 'H')
    if 'bossbar add' in cmd:
        return ('bossbar', 'Creates bossbar 1 named "Parkour Tag - Timer"', 'start chain', 'LOW: JSON text still accepted', 'H')
    if 'bossbar remove' in cmd:
        return ('bossbar', 'Removes the timer bossbar', 'start/end chain', 'LOW', 'H')
    if 'bossbar set minecraft:1 players' in cmd:
        return ('bossbar', 'Adds players in the start/arena box to the bossbar', 'start chain', 'LOGIC: box x1057..1065 y15..20 z979..1011 excludes upper balconies and E/W edges; list is static', 'H')
    if 'bossbar set minecraft:1 max' in cmd:
        return ('bossbar', 'Sets bossbar max to 60', 'start chain', 'LOW', 'H')
    if 'bossbar set minecraft:1 style' in cmd:
        return ('bossbar', 'Sets bossbar style notched_6', 'start chain', 'LOW', 'H')
    if 'store result bossbar' in cmd:
        if 'remove fakeplayer timer_parkourtag 1' in cmd:
            return ('timer', 'Decrements fakeplayer timer by 1 and displays it', '1-second clock', 'LOW', 'H')
        return ('timer', 'Displays current fakeplayer timer without decrementing', '1-second clock', 'LOW', 'H')
    if 'timer_parkourtag 60' in cmd:
        return ('timer', 'Sets fakeplayer timer to 60', 'start chain', 'LOW', 'H')
    if 'timer_parkourtag -1' in cmd:
        if c['x'] == '1064' and c['y'] == '9':
            return ('timer', 'Stops the fakeplayer timer after the round', 'end chain', 'LOW', 'H')
        return ('timer', 'Sets a player\'s personal timer score to -1 (not the fakeplayer)', 'win chain', 'LOGIC: ineffective for stopping the clock; fakeplayer is cleared by the end chain', 'H')
    if 'matches 0 run fill' in cmd:
        return ('win-check', 'When timer hits 0, places redstone at 1068 5 987 (runners-win trigger)', 'repeating, timer=0', 'LOW', 'H')
    if 'unless entity @a[level=7]' in cmd:
        return ('win-check', 'If no hunter (level 7) exists, places redstone at 1068 5 987 or opens the hunter pad', 'repeating', 'LOGIC: fires on disconnect, not on hunter death at the hub (hunter stays level 7)', 'H')
    if 'kill_parkourtag=' in cmd:
        m = re.search(r'kill_parkourtag=(\d)', cmd)
        n = m.group(1) if m else '?'
        if 'fill 1070 5 991' in cmd:
            return ('win-check', f'If hunter kill count = {n}, places redstone at 1070 5 991 (hunter-win trigger)', 'repeating, armed column only', 'DESIGN: one column armed by the start button; multiple armed columns would end early', 'H')
        return ('win-check', f'If hunter kill count = {n}, sets timer score', 'repeating, armed column only', 'LOGIC: sets player score, not fakeplayer', 'H')
    if 'tellraw' in cmd:
        if cmd.count('{') != cmd.count('}'):
            return ('win-message', 'Hunter-wins tellraw to runners', 'win chain', 'BROKEN: extra trailing } in JSON; command fails to parse (1.17 and 1.21)', 'H')
        return ('win-message', 'Win/lose tellraw', 'win chain', 'LOW', 'H')
    if 'title' in cmd:
        return ('countdown', 'Countdown title/subtitle/times', 'start chain', 'LOW: JSON text accepted', 'H')
    if 'playsound' in cmd:
        return ('countdown', 'Countdown note/bell sound', 'start chain', 'LOW', 'H')
    if 'fill' in cmd and 'stained_glass' in cmd:
        if 'black_stained_glass' in cmd:
            return ('door', 'Closes a start door with black glass (pre-countdown state)', 'start chain', 'LOW', 'H')
        if 'red_stained_glass' in cmd:
            return ('door', 'Sets a start door to red glass (countdown 3)', 'start chain', 'LOW', 'H')
        if 'yellow_stained_glass' in cmd:
            return ('door', 'Sets a start door to yellow glass (countdown 2)', 'start chain', 'LOW', 'H')
        if 'lime_stained_glass' in cmd:
            return ('door', 'Sets a start door to lime glass (countdown 1)', 'start chain', 'LOW', 'H')
        return ('door', 'Start-door glass state', 'start chain', 'LOW', 'H')
    if 'fill' in cmd and 'barrier' in cmd:
        return ('role-lock', 'Places a 2-high barrier on the hunter pad (locks the hunter slot)', 'hunter on blackstone pad', 'LOW', 'H')
    if 'fill' in cmd and '1049 28 999' in cmd:
        return ('role-lock', 'Removes the hunter-pad barrier (unlocks the hunter slot)', 'hunter on glowstone / no hunter', 'LOW', 'H')
    if 'tp @a[level=7]' in cmd and '1060 17 1007' in cmd:
        return ('start', 'Teleports the hunter to the hunter start room', 'start chain', 'LOW', 'H')
    if 'tp @a[level=6]' in cmd and '1061 17 982' in cmd:
        return ('start', 'Teleports runners to the runner start room', 'start chain', 'LOW', 'H')
    if 'tp @a[level=7]' in cmd:
        return ('reset', 'Teleports the hunter to the gallery', 'end chain', 'LOW', 'H')
    if 'tp @a[level=6]' in cmd:
        return ('reset', 'Teleports runners to the gallery', 'end chain', 'LOW', 'H')
    if 'tp @s 1064 1 985' in cmd:
        return ('role-select', 'Runner pad teleports to the underground waiting lobby', 'level 5 on chiseled stone bricks', 'LOW', 'H')
    if 'tp @s 1058 1 985' in cmd:
        return ('role-select', 'Hunter pad teleports to the underground waiting lobby', 'level 7 on chiseled polished blackstone', 'LOW', 'H')
    if 'xp set @s 5 levels' in cmd:
        return ('role-select', 'Runner pad sets XP level 5', 'level 4 on chiseled stone bricks', 'LOW', 'H')
    if 'xp set @s 7 levels' in cmd:
        return ('role-select', 'Hunter pad sets XP level 7', 'level 4 on chiseled polished blackstone', 'LOW', 'H')
    if 'xp set @s 6 levels' in cmd:
        return ('start', 'Promotes waiting runners from level 5 to level 6', 'start chain', 'LOW', 'H')
    if 'xp set @a[level=6] 4 levels' in cmd:
        return ('reset', 'Resets runners to level 4 after the round', 'end chain', 'LOW', 'H')
    if 'xp set @a[level=7] 4 levels' in cmd:
        return ('reset', 'Resets the hunter to level 4 after the round', 'end chain', 'LOW', 'H')
    if 'spawnpoint' in cmd:
        return ('respawn', 'Sets each runner\'s spawnpoint to the gallery (1061 28 995)', 'start chain', 'LOW', 'H')
    if 'clear @a[level=5]' in cmd:
        return ('loadout', 'Clears waiting-runner inventory every tick (removes hub carrot/lobby items)', 'repeating', 'LOW', 'H')
    if 'clear @a[level=7]' in cmd:
        return ('loadout', 'Clears hunter inventory every tick', 'repeating', 'LOW', 'H')
    if 'clear' in cmd:
        return ('loadout', 'Inventory clear', 'repeating', 'LOW', 'H')
    if 'effect give' in cmd:
        if 'saturation' in cmd:
            return ('loadout', 'Grants saturation (no hunger during the game)', 'repeating', 'LOW', 'H')
        if 'glowing' in cmd:
            return ('loadout', 'Outlines living runners (Glowing) for the hunter', 'repeating, death=0', 'LOW', 'H')
        if 'strength' in cmd:
            return ('loadout', 'Grants the hunter Strength 100 (one-hit tag)', 'repeating', 'LOW: amplifier 100 is valid', 'H')
    if 'team join pve' in cmd:
        return ('team', 'Joins players to team pve (friendly fire off)', 'repeating/chain', 'RISK: pve is the hunter/lobby team; runners are moved to Parkour_Tag at round start', 'H')
    if 'team empty Parkour_Tag' in cmd:
        return ('team', 'Empties team Parkour_Tag before re-joining runners', 'repeating, auto=0; runtime redstone at 1057 6 1001', 'RISK: works only while the runtime redstone block at 1057 6 1001 exists (PT-C060); if missing, runners stay on pve', 'H')
    if 'team join Parkour_Tag' in cmd:
        return ('team', 'Joins level-6 runners to team Parkour_Tag (gold)', 'chain from team-empty block', 'RISK: depends on the runtime team-split redstone', 'H')
    if 'kill @e[type=item' in cmd:
        return ('cleanup', 'Kills dropped items on the gallery deck', 'repeating', 'LOW', 'H')
    if 'setblock 1048 12 981' in cmd:
        return ('clock', 'Pulses the NW redstone clock at 1048 12 981', 'CB-17 pair', 'LOW', 'H')
    if 'setblock' in cmd and 'redstone_block' in cmd:
        return ('trigger', 'Places a redstone block to fire the next chain', 'chain', 'LOW', 'H')
    if 'setblock' in cmd and 'air' in cmd:
        return ('trigger', 'Removes a redstone block/pulse', 'chain', 'LOW', 'H')
    if 'fill' in cmd:
        return ('setup/reset', 'Fills/clears a small machine area', 'chain', 'LOW', 'H')
    if 'scoreboard players set' in cmd:
        return ('score-reset', 'Resets a scoreboard value', 'repeating/chain', 'LOW', 'H')
    return ('other', 'Unclassified', '?', 'CHECK', 'M')

# manual notes by coordinate
NOTES = {
    ('100',): '',
}
manual = {
    (1055, 6, 1001): 'Only command that separates runners from the hunter team; it depends on the runtime redstone block at 1057 6 1001.',
    (1054, 6, 1001): 'Runs only if the team-empty repeating block at 1055 6 1001 runs; that block is powered by the runtime redstone block at 1057 6 1001 (PT-C060).',
    (1066, 6, 997): 'Extra closing brace in the tellraw JSON.',
    (1049, 5, 997): 'Empty chain command block.',
    (1068, 5, 995): 'Surviving-runner firework; old item NBT.',
    (1064, 2, 992): 'Four corner fireworks, one per kill.',
    (1064, 3, 992): '',
    (1064, 4, 992): '',
    (1064, 5, 992): '',
    (1064, 5, 1001): '4-runner threshold column.',
    (1066, 5, 1001): '3-runner threshold column.',
    (1068, 5, 1001): '2-runner threshold column.',
    (1070, 5, 1001): '1-runner threshold column.',
    (1064, 5, 1003): '4-runner timer stop (sets player score, not fakeplayer).',
    (1066, 5, 1003): '3-runner timer stop.',
    (1068, 5, 1003): '2-runner timer stop.',
    (1070, 5, 1003): '1-runner timer stop.',
    (1071, 5, 999): 'Hunter -> gallery.',
    (1071, 6, 999): 'Runners -> gallery.',
    (1071, 7, 999): 'Runner XP reset to 4.',
    (1071, 8, 999): 'Hunter XP reset to 4.',
    (1052, 1, 984): 'Four-runner start button chain head.',
    (1052, 1, 986): 'Three-runner start button chain head (conditional, condition false in export).',
    (1053, 3, 984): 'Two-runner start button chain head.',
    (1053, 3, 986): 'One-runner start button chain head.',
    (1052, 2, 984): 'Arms 4-runner threshold at x1064.',
    (1052, 2, 986): 'Arms 3-runner threshold at x1066 (conditional).',
    (1053, 4, 984): 'Arms 2-runner threshold at x1068.',
    (1053, 4, 986): 'Arms 1-runner threshold at x1070.',
    (1060, 2, 990): 'Bossbar remove at countdown start.',
    (1060, 3, 990): 'Bossbar add.',
    (1060, 4, 990): 'Bossbar max 60.',
    (1060, 2, 992): 'Bossbar player box; excludes y21-25 balconies.',
    (1056, 2, 996): 'Hunter start teleport.',
    (1056, 2, 994): 'Promotes level 5 runners to 6.',
    (1056, 3, 994): 'Runner start teleport.',
    (1064, 9, 995): 'Stops fakeplayer timer at round end.',
    (1058, 3, 1000): 'Places the runtime redstone block at 1057 6 1001 that powers the runner team split.',
    (1068, 5, 986): 'Removes the runtime team-split redstone block at 1057 6 1001.',
    (1070, 5, 990): 'Removes the runtime team-split redstone block at 1057 6 1001.',
    (1068, 5, 983): 'Timer-expiry trigger.',
    (1068, 5, 986): 'Runners-win reset chain head; also removes the team-split redstone at 1057 6 1001.',
    (1068, 5, 995): 'Runners-win celebration head.',
    (1066, 5, 997): 'Hunter-win message head (runners message is broken).',
}

out = []
for i, r in enumerate(rows, 1):
    x, y, z = int(r['x']), int(r['y']), int(r['z'])
    role, effect, trigger, risk, conf = classify(r)
    if (x, y, z) in ((1052, 1, 984), (1052, 1, 986), (1053, 3, 984), (1053, 3, 986)):
        role, effect, trigger = 'start-button', 'Clears the armed-threshold row before arming the selected runner count', 'birch button in the waiting lobby'
    if (x, y, z) in ((1052, 2, 984), (1052, 2, 986), (1053, 4, 984), (1053, 4, 986)):
        role, effect, trigger = 'win-arm', 'Places the redstone block that arms one kill-threshold column', 'start-button chain'
    note = manual.get((x, y, z), '')
    cluster = 'CB-17' if y >= 10 else 'CB-16'
    out.append({
        'id': f'PT-C{i:03d}',
        'cluster': cluster,
        'x': x, 'y': y, 'z': z,
        'kind': r['kind'],
        'auto': r['auto'],
        'conditional': r['condition_met'],
        'role': role,
        'effect': effect,
        'trigger': trigger,
        'command': r['command'],
        '1_21_risk': risk,
        'notes': note,
        'confidence': conf,
    })

with open(os.path.join(HERE, 'commands.csv'), 'w', newline='', encoding='utf-8') as f:
    w = csv.DictWriter(f, fieldnames=list(out[0].keys()))
    w.writeheader(); w.writerows(out)
print('commands.csv', len(out))

# ---- signs -----------------------------------------------------------------
srows = list(csv.DictReader(open(os.path.join(HERE, 'signs_full.csv'), encoding='utf-8')))
sign_role = {
    (1060, 1, 984): ('waiting-lobby label', 'Header over the underground waiting room'),
    (1062, 1, 984): ('waiting-lobby label', 'Runner role text'),
    (1057, 1, 985): ('runner-count selector', 'Three (left/south) / Four (right/north) runners; buttons at 1057 1 984 and 1057 1 986'),
    (1060, 1, 985): ('start prompt', '"Start the game when ready!" - the count buttons start the round'),
    (1062, 1, 985): ('waiting-lobby label', 'Header'),
    (1059, 1, 990): ('hint', 'Points to the on-screen counter (bossbar)'),
    (1048, 1, 992): ('hint', 'Clear carrot on a stick (hub teleporter item) on role selection'),
    (1051, 1, 992): ('hint', 'Saturation is granted continuously'),
    (1053, 1, 992): ('hint', 'Items free lobby - no containers exist in the arena; likely leftover/aspirational'),
    (1055, 1, 993): ('hint', 'General Runner Settings point east'),
    (1055, 1, 995): ('hint', 'Runner & Hunter Countdown point west'),
    (1057, 1, 997): ('hint', 'Hunter Settings point west'),
    (1059, 1, 998): ('hint', 'Reset game settings point west'),
    (1057, 2, 983): ('lobby-teleporter', 'Points to the hub carrot-on-a-stick teleporter'),
    (1063, 2, 983): ('lobby-teleporter', 'Same'),
    (1063, 2, 988): ('hint', 'Runner Settings point east'),
    (1065, 2, 988): ('hint', 'Hunter Settings point east'),
    (1057, 3, 985): ('runner-count selector', 'One (left/south) / Two (right/north) runners; buttons at 1057 3 984 and 1057 3 986'),
    (1064, 4, 1006): ('machine label', 'Four-runner threshold column (x1064)'),
    (1066, 4, 1006): ('machine label', 'Three-runner threshold column (x1066)'),
    (1068, 4, 1006): ('machine label', 'Two-runner threshold column (x1068)'),
    (1070, 4, 1006): ('machine label', 'One-runner threshold column (x1070)'),
    (1056, 5, 999): ('hint', 'Warning: if the hunter leaves the settings/selection, the round breaks'),
    (1050, 29, 991): ('gallery pad', 'Runner teleporter pad (chiseled stone bricks at 1049 27 990)'),
    (1050, 29, 1000): ('gallery pad', 'Hunter teleporter pad (chiseled polished blackstone at 1049 27 999)'),
}
sout = []
for r in srows:
    x, y, z = int(r['x']), int(r['y']), int(r['z'])
    role, note = sign_role.get((x, y, z), ('label', ''))
    sout.append({
        'x': x, 'y': y, 'z': z,
        'color': r['color'], 'glow': int(r['glow']),
        'front1': r['front1'], 'front2': r['front2'], 'front3': r['front3'], 'front4': r['front4'],
        'back': '', 'click_event': r['click'] or 'none',
        'role': role, 'notes': note, 'confidence': 'H',
    })
with open(os.path.join(HERE, 'signs.csv'), 'w', newline='', encoding='utf-8') as f:
    w = csv.DictWriter(f, fieldnames=list(sout[0].keys()))
    w.writeheader(); w.writerows(sout)
print('signs.csv', len(sout))

# ---- blocks ----------------------------------------------------------------
brows = list(csv.DictReader(open(os.path.join(HERE, 'pt_blocks.csv'), encoding='utf-8')))
counts = collections.Counter(r['block'].split(':')[1] for r in brows)
total = sum(counts.values())

USE = {
    'gray_concrete': ('structure', 'Bowl/outer shell, gallery deck, command room'),
    'cyan_terracotta': ('structure', 'Arena walls, start towers, trim', 'primary'),
    'stone_bricks': ('structure', 'Arena walls, start-room trim, stair blocks', 'primary'),
    'smooth_quartz': ('structure', 'Gallery trim, start-room floor, seating accents', 'accent'),
    'white_concrete': ('structure', 'Gallery rim, command-room floor, start pads', 'accent'),
    'dead_horn_coral_block': ('floor', 'Main arena floor (pink) and start-room floors', 'primary'),
    'barrier': ('barrier', 'Invisible roof over the arena (y27) and role-lock pad', 'invisible'),
    'brick_slab': ('platform', 'SW/NE/centre upper perches + NW decorative roof', 'accent'),
    'black_concrete': ('structure', 'Bowl rim, banner frame, dark trim'),
    'deepslate_bricks': ('structure', 'SW/SE room walls, lower ledges'),
    'red_concrete': ('accent', 'Banner letters, small jump pads, trim'),
    'waxed_copper_block': ('route', 'Copper ledges, pipes, NE reactor, walls', 'accent'),
    'smooth_quartz_stairs': ('stairs', 'Gallery seating, start-room stairs'),
    'waxed_exposed_copper': ('route', 'Copper ledges/pipes'),
    'waxed_weathered_copper': ('route', 'Copper ledges/pipes'),
    'redstone_wire': ('machine', 'Command-room wiring'),
    'iron_trapdoor': ('route', 'Grated catwalk surfaces at y11-20'),
    'repeater': ('machine', 'Delays in the command/clock circuit'),
    'stone_brick_stairs': ('stairs', 'Arena climbing steps, SE grandstand edge'),
    'waxed_oxidized_copper': ('route', 'Green oxidized copper trim'),
    'chain_command_block': ('machine', 'Sequenced commands'),
    'smooth_quartz_slab': ('platform', 'Gallery trim, small ledges'),
    'spruce_stairs': ('stairs', 'Start-room benches, SE grandstand'),
    'command_block': ('machine', 'Impulse/one-shot commands'),
    'repeating_command_block': ('machine', 'Always-on loops (timer, effects, win checks)'),
    'blue_ice': ('route', 'NW ice rink (slippery fast lane)'),
    'light': ('lighting', 'Invisible light sources (29)'),
    'terracotta': ('accent', 'Bowl trim'),
    'birch_wall_sign': ('sign', 'All 25 signs'),
    'deepslate_tiles': ('route', 'Central deepslate tile island at y12'),
    'tinted_glass': ('structure', 'Command-room glass'),
    'oak_slab': ('platform', 'Small half-block pads'),
    'polished_deepslate_wall': ('structure', 'Start-room/room walls'),
    'obsidian': ('route', 'Slime-pad frame at the south floor'),
    'stone_brick_wall': ('structure', 'Low walls'),
    'polished_deepslate_stairs': ('stairs', 'NE/SE room details'),
    'ladder': ('route', 'Vertical connectors (15)'),
    'spruce_slab': ('platform', 'Start-room benches, SE grandstand'),
    'deepslate_brick_stairs': ('stairs', 'SW/NE room details'),
    'sticky_piston': ('machine', 'Item/role-lock mechanisms; some decorative'),
    'iron_door': ('hiding', 'Closet/room doors (SW, SE, start rooms)'),
    'slime_block': ('route', 'Bounce pads (2x2 south floor, 2x2 y13 centre)'),
    'red_terracotta': ('accent', 'Bowl trim'),
    'stone_brick_slab': ('platform', 'Small ledges'),
    'birch_button': ('machine', 'Four runner-count buttons in the waiting lobby'),
    'redstone_block': ('machine', 'Pulse sources'),
    'soul_lantern': ('lighting', 'Warm accent lights'),
    'spruce_trapdoor': ('route', 'Start-room bench details'),
    'iron_block': ('structure', 'Start-room door frames'),
    'bookshelf': ('decor', 'Command-room decoration'),
    'deepslate_tile_slab': ('platform', 'Central nook details'),
    'piston_head': ('machine', 'Piston mechanism'),
    'waxed_cut_copper_stairs': ('route', 'Copper stair trim'),
    'stone_pressure_plate': ('machine', 'Two plates at 1060/1061 11 1001'),
    'polished_deepslate_slab': ('platform', 'Small ledges'),
    'potted_cornflower': ('decor', 'Command-room decoration'),
    'deepslate_brick_slab': ('platform', 'Small ledges'),
    'glowstone': ('lighting', 'Glowstone pad in the waiting lobby'),
    'sea_lantern': ('lighting', 'Command-room light'),
    'chiseled_stone_bricks': ('route', 'Runner role pad at 1049 27 990'),
    'chiseled_polished_blackstone': ('route', 'Hunter role pad at 1049 27 999'),
    'piston': ('machine', 'Piston mechanism'),
    'barrel': ('container', 'Empty storage barrel at 1067 12 992 (Items: [])'),
    'lectern': ('container', 'Decorative instruction lecterns at 1049 28 993/996 (written guide "Parkour Tag"; no Items)'),
    'polished_blackstone_pressure_plate': ('trigger', 'SE-room pressure plate at 1070 14 1003'),
    'raw_gold_block': ('decor', 'Decorative gold block at 1061 15 990'),
    'waxed_exposed_cut_copper_stairs': ('route', 'NE copper stair trim'),
    'infested_stone_bricks': ('decor', 'Hidden infested stone brick in the gallery step at 1047 28 993'),
}

bout = []
for name, n in counts.most_common():
    cat, use, *pal = USE.get(name, ('other', 'Unclassified', ''))
    bout.append({
        'block': 'minecraft:' + name,
        'count': n,
        'share_pct': round(100.0 * n / total, 2),
        'category': cat,
        'palette_role': pal[0] if pal else '',
        'use': use,
        'light_source': 'yes' if name in ('light', 'glowstone', 'sea_lantern', 'soul_lantern') else '',
        'confidence': 'H',
    })
with open(os.path.join(HERE, 'blocks.csv'), 'w', newline='', encoding='utf-8') as f:
    w = csv.DictWriter(f, fieldnames=list(bout[0].keys()))
    w.writeheader(); w.writerows(bout)
print('blocks.csv', len(bout), 'total', total)

# ---- checkpoints -----------------------------------------------------------
cp = [
    ('CP-0', 'hub', 'main-hub pad', -212, 10, 975, 'n/a', 0,
     'Orange carpet, XP level 4; command tp 1061 28 995', 'one-way entry', 'H'),
    ('CP-1', 'arrival', 'gallery arrival', 1061, 28, 995, 'n/a', 0,
     'Stadium walkway / spectator deck; respawn point for runners', 'arrival + runner respawn', 'H'),
    ('CP-2', 'role', 'runner role pad', 1049, 27, 990, 'n/a', 1,
     'Chiseled stone bricks; level 4 -> 5 -> tp 1064 1 985', 'runner select', 'H'),
    ('CP-3', 'role', 'hunter role pad', 1049, 27, 999, 'n/a', 1,
     'Chiseled polished blackstone; level 4 -> 7 -> tp 1058 1 985; barrier locks the pad', 'hunter select', 'H'),
    ('CP-4', 'waiting', 'runner lobby spot', 1064, 1, 985, 'n/a', 0,
     'Underground waiting lobby; four count buttons at 1057 1/3 984/986', 'pre-round hold', 'H'),
    ('CP-5', 'waiting', 'hunter lobby spot', 1058, 1, 985, 'n/a', 0,
     'Underground waiting lobby; glowstone pad 1058 0 982 unlocks the hunter pad', 'pre-round hold', 'H'),
    ('CP-6', 'start', 'runner start gate', 1061, 17, 982, 'z', 4,
     'North start room; door 1059..1062 y17..18 z984; opens when countdown ends', 'runner spawn', 'H'),
    ('CP-7', 'start', 'hunter start gate', 1060, 17, 1007, 'z', 4,
     'South start room; door 1059..1062 y17..18 z1005; opens when countdown ends', 'hunter spawn', 'H'),
    ('CP-8', 'timer', 'timer start', 1060, 2, 994, 'n/a', 0,
     'fakeplayer timer_parkourtag = 60; bossbar 1 max 60', 'round clock', 'H'),
    ('CP-9', 'win', 'timer expiry trigger', 1068, 5, 983, 'n/a', 0,
     'timer=0 -> redstone 1068 5 987 -> Runners win', 'runner win', 'H'),
    ('CP-10', 'win', 'kill-threshold trigger', 1064, 5, 1001, 'n/a', 0,
     'kill_parkourtag = armed runner count -> redstone 1070 5 991 -> Hunter win', 'hunter win', 'H'),
    ('CP-11', 'win', 'hunter-absent trigger', 1055, 6, 999, 'n/a', 0,
     'level 6 exists and no level 7 -> Runners win', 'runner win on hunter disconnect', 'H'),
    ('CP-12', 'reset', 'post-round reset', 1071, 9, 999, 'n/a', 0,
     'All players -> 1061 28 995, XP level 4', 'reset', 'H'),
]
with open(os.path.join(HERE, 'checkpoints.csv'), 'w', newline='', encoding='utf-8') as f:
    w = csv.writer(f)
    w.writerow(['id', 'stage', 'name', 'x', 'y', 'z', 'gate_axis', 'gate_width',
                'what_it_grants_or_clears', 'role', 'confidence'])
    w.writerows(cp)
print('checkpoints.csv', len(cp))

# ---- entities --------------------------------------------------------------
with open(os.path.join(HERE, 'entities.csv'), 'w', newline='', encoding='utf-8') as f:
    w = csv.writer(f)
    w.writerow(['type', 'x', 'y', 'z', 'note', 'confidence'])
    w.writerow(['none', '', '', '', 'No entity-region records inside x1035..1078 y0..58 z972..1017; the 8 entities in the world are elsewhere', 'H'])
print('entities.csv 0')

# ---- containers ------------------------------------------------------------
# A14 QA: 1 storage barrel + 2 decorative lecterns = 3 container block entities; all empty.
with open(os.path.join(HERE, 'containers.csv'), 'w', newline='', encoding='utf-8') as f:
    w = csv.writer(f)
    w.writerow(['type', 'x', 'y', 'z', 'items', 'contents', 'note', 'confidence'])
    w.writerow(['minecraft:barrel', 1067, 12, 992, 0, '', 'Empty storage barrel; no loot.', 'H'])
    w.writerow(['minecraft:lectern', 1049, 28, 993, 0,
                'written_book: "Parkour Tag" by daniellaur (guide)',
                'Decorative lectern; holds a guide Book, no Items. Empty storage.', 'H'])
    w.writerow(['minecraft:lectern', 1049, 28, 996, 0,
                'written_book: "Parkour Tag" by daniellaur (guide)',
                'Decorative lectern; holds a guide Book, no Items. Empty storage.', 'H'])
print('containers.csv 3 (1 storage barrel + 2 decorative lecterns)')
