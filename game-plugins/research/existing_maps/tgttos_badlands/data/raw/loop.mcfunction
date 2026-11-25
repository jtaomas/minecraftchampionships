kill @e[type=item]
effect give @e[type=chicken] resistance 1 5 true
fill 9 -40 21 11 -42 40 air replace blue_concrete

item replace entity @a weapon.offhand with blue_concrete{CanPlaceOn:["minecraft:blue_concrete","minecraft:gray_wool","minecraft:jungle_planks","minecraft:jungle_fence","minecraft:red_wool","minecraft:light_gray_concrete_powder","minecraft:gray_concrete","minecraft:nether_brick_wall","minecraft:red_concrete","minecraft:sea_lantern","minecraft:chain","minecraft:red_terracotta","minecraft:diorite","minecraft:white_terracotta","minecraft:white_concrete","minecraft:cactus","minecraft:soul_sand","minecraft:red_sand","minecraft:cyan_concrete_powder","minecraft:smooth_stone","minecraft:stripped_spruce_log","minecraft:acacia_leaves","minecraft:gravel","minecraft:diorite_wall","minecraft:nether_brick_slab","minecraft:iron_trapdoor","minecraft:gray_terracotta","minecraft:andesite","minecraft:red_sandstone_stairs","minecraft:light_gray_concrete","minecraft:light_gray_terracotta","minecraft:spruce_planks","minecraft:brown_concrete_powder","minecraft:granite","minecraft:terracotta","minecraft:stone","minecraft:smooth_red_sandstone","minecraft:stone_bricks","minecraft:blackstone","minecraft:iron_bars","minecraft:cyan_terracotta","minecraft:oak_leaves","minecraft:black_terracotta","minecraft:black_concrete"]} 2
item replace entity @a weapon.offhand with blue_concrete{CanPlaceOn:["minecraft:blue_concrete","minecraft:gray_wool","minecraft:jungle_planks","minecraft:jungle_fence","minecraft:red_wool","minecraft:light_gray_concrete_powder","minecraft:gray_concrete","minecraft:nether_brick_wall","minecraft:red_concrete","minecraft:sea_lantern","minecraft:chain","minecraft:red_terracotta","minecraft:diorite","minecraft:white_terracotta","minecraft:white_concrete","minecraft:cactus","minecraft:soul_sand","minecraft:red_sand","minecraft:cyan_concrete_powder","minecraft:smooth_stone","minecraft:stripped_spruce_log","minecraft:acacia_leaves","minecraft:gravel","minecraft:diorite_wall","minecraft:nether_brick_slab","minecraft:iron_trapdoor","minecraft:gray_terracotta","minecraft:andesite","minecraft:red_sandstone_stairs","minecraft:light_gray_concrete","minecraft:light_gray_terracotta","minecraft:spruce_planks","minecraft:brown_concrete_powder","minecraft:granite","minecraft:terracotta","minecraft:stone","minecraft:smooth_red_sandstone","minecraft:stone_bricks","minecraft:blackstone","minecraft:iron_bars","minecraft:cyan_terracotta","minecraft:oak_leaves","minecraft:black_terracotta","minecraft:black_concrete"]} 1
item replace entity @a hotbar.0 with iron_pickaxe{CanDestroy:["minecraft:blue_concrete"],Enchantments:[{id:"minecraft:efficiency",lvl:2s}]}

execute as @a[scores={start=1..}] at @s run scoreboard players add @a countdown 1
execute as @a[scores={countdown=20..120}] at @s run title @a times 0 3 0
execute as @a[scores={countdown=20..120}] at @s run title @a title {"text":"Starting in","color":"aqua"}
execute as @a[scores={countdown=20..40}] at @s run title @a subtitle {"text":"5","bold":true}
execute as @a[scores={countdown=40..60}] at @s run title @a subtitle {"text":"4","bold":true}
execute as @a[scores={countdown=60..80}] at @s run title @a subtitle {"text":"3","bold":true}
execute as @a[scores={countdown=80..100}] at @s run title @a subtitle {"text":"2","bold":true}
execute as @a[scores={countdown=100..120}] at @s run title @a subtitle {"text":"1","bold":true}
execute as @a[scores={countdown=120..130}] at @s run title @a times 0 20 10
execute as @a[scores={countdown=120..130}] at @s run title @a title {"text":"GO!!!","color":"aqua"}
execute as @a[scores={countdown=120..130}] at @s run title @a subtitle {"text":" "}
execute as @a[scores={countdown=120..130}] at @s run fill 9 -38 41 12 -42 20 air replace barrier
execute as @a[scores={countdown=130..}] at @s run scoreboard players set @a playing 1
execute as @a[scores={countdown=130..}] at @s run scoreboard players set @a start 0
execute as @a[scores={countdown=130..}] at @s run scoreboard players set @a countdown 0

execute as @a[scores={playing=1..}] at @s run scoreboard players add @s spawntime 1
execute as @a[scores={playing=1..}] at @s run scoreboard players add @s playtimeT 1
execute as @a[scores={playtimeT=20..}] at @s run scoreboard players add @s playtimeS 1
execute as @a[scores={playtimeT=20..}] at @s run scoreboard players set @s playtimeT 0

execute as @a[scores={spawntime=20..}] at @s run summon armor_stand ~ ~10 ~ {NoGravity:1b,Silent:1b,Invulnerable:1b,Small:1b,Invisible:1b,NoBasePlate:1b,Tags:["remove_block"]}
execute as @a[scores={spawntime=20..}] at @s run scoreboard players set @s spawntime 0

execute as @a[scores={hit_chicken=1..}] at @s run scoreboard players set @s playing 0
execute as @a[scores={hit_chicken=1..}] at @s run title @a times 0 100 20
execute as @a[scores={hit_chicken=1..}] at @s run title @a subtitle ["",{"text":"Time: ","italic":true,"color":"dark_gray"},{"score":{"name":"@s","objective":"playtimeS"},"italic":true,"color":"dark_gray"},{"text":" Seconds and ","italic":true,"color":"dark_gray"},{"score":{"name":"@s","objective":"playtimeT"},"italic":true,"color":"dark_gray"},{"text":" Ticks","italic":true,"color":"dark_gray"}]
execute as @a[scores={hit_chicken=1..}] at @s run title @a title {"text":"Finished","color":"green"}
execute as @a[scores={hit_chicken=1..}] at @s run gamemode spectator @s
execute as @a[scores={hit_chicken=1..}] at @s run scoreboard players add @s finished 1
execute as @a[scores={finished=1}] at @s run particle minecraft:totem_of_undying ~ ~ ~ 0 0.3 0 1 50 normal
execute as @a[scores={finished=1}] at @s run playsound minecraft:entity.arrow.hit_player ambient @a ~ ~ ~ 1 1 0
execute as @a[scores={finished=60..}] at @s run fill 9 -38 20 12 -42 20 barrier
execute as @a[scores={finished=60..}] at @s run fill 12 -38 20 12 -42 41 barrier
execute as @a[scores={finished=60..}] at @s run fill 12 -42 41 9 -38 41 barrier
execute as @a[scores={finished=60..}] at @s run clear @s
execute as @a[scores={finished=60..}] at @s run gamemode adventure @s
execute as @a[scores={finished=60..}] at @s run tp @a 10 -42 31
execute as @a[scores={finished=60..}] at @s run scoreboard players set @s spawntime 0
execute as @a[scores={finished=60..}] at @s run scoreboard players set @s playtimeT 0
execute as @a[scores={finished=60..}] at @s run scoreboard players set @s playtimeS 0


execute as @a[scores={finished=60..}] at @s run execute as @e[tag=remove_block] at @s run fill ~-15 ~-25 ~-15 ~15 ~5 ~15 air replace blue_concrete
execute as @a[scores={finished=60..}] at @s run kill @e[tag=remove_block]

#fill cactus
execute as @a[scores={finished=60..}] at @s run fill 229 -4 47 229 -2 47 cactus
execute as @a[scores={finished=60..}] at @s run fill 227 -4 49 227 -3 49 cactus
execute as @a[scores={finished=60..}] at @s run fill 224 -4 27 224 -2 27 cactus
execute as @a[scores={finished=60..}] at @s run fill 218 -8 28 218 -4 28 cactus
execute as @a[scores={finished=60..}] at @s run fill 218 -8 34 218 -6 34 cactus
execute as @a[scores={finished=60..}] at @s run fill 217 -8 35 217 -7 35 cactus
execute as @a[scores={finished=60..}] at @s run fill 216 -8 29 216 -6 29 cactus
execute as @a[scores={finished=60..}] at @s run fill 206 -14 13 206 -12 13 cactus
execute as @a[scores={finished=60..}] at @s run fill 206 -16 27 206 -13 27 cactus
execute as @a[scores={finished=60..}] at @s run fill 204 -14 15 204 -13 15 cactus
execute as @a[scores={finished=60..}] at @s run fill 201 -18 28 201 -15 28 cactus
execute as @a[scores={finished=60..}] at @s run fill 199 -18 26 199 -16 26 cactus
execute as @a[scores={finished=60..}] at @s run setblock 193 -20 43 cactus
execute as @a[scores={finished=60..}] at @s run fill 150 -21 13 150 -19 13 cactus
execute as @a[scores={finished=60..}] at @s run fill 149 -21 14 149 -20 14 cactus
execute as @a[scores={finished=60..}] at @s run fill 147 -21 29 147 -19 29 cactus
execute as @a[scores={finished=60..}] at @s run fill 144 -23 28 144 -20 28 cactus
execute as @a[scores={finished=60..}] at @s run fill 72 -30 37 72 -28 37 cactus
execute as @a[scores={finished=60..}] at @s run fill 71 -30 36 71 -29 36 cactus
execute as @a[scores={finished=60..}] at @s run setblock 70 -30 13 cactus
execute as @a[scores={finished=60..}] at @s run fill 69 -30 21 69 -28 21 cactus
execute as @a[scores={finished=60..}] at @s run fill 69 -30 42 69 -27 42 cactus
execute as @a[scores={finished=60..}] at @s run setblock 67 -30 20 cactus
execute as @a[scores={finished=60..}] at @s run fill 60 -30 28 60 -29 28 cactus
execute as @a[scores={finished=60..}] at @s run fill 59 -30 26 59 -28 26 cactus
execute as @a[scores={finished=60..}] at @s run fill 58 -30 49 58 -28 49 cactus
execute as @a[scores={finished=60..}] at @s run setblock 57 -30 50 cactus
execute as @a[scores={finished=60..}] at @s run fill 56 -30 15 56 -29 15 cactus
execute as @a[scores={finished=60..}] at @s run fill 54 -30 18 54 -27 18 cactus
execute as @a[scores={finished=60..}] at @s run fill 53 -30 16 53 -29 16 cactus
execute as @a[scores={finished=60..}] at @s run fill 53 -32 54 53 -30 54 cactus
execute as @a[scores={finished=60..}] at @s run fill 51 -34 55 51 -32 55 cactus
execute as @a[scores={finished=60..}] at @s run fill 46 -32 13 46 -30 13 cactus
execute as @a[scores={finished=60..}] at @s run fill 44 -32 15 44 -31 15 cactus
execute as @a[scores={finished=60..}] at @s run fill 43 -36 24 43 -33 24 cactus
execute as @a[scores={finished=60..}] at @s run fill 41 -36 53 41 -33 53 cactus
execute as @a[scores={finished=60..}] at @s run fill 39 -36 52 39 -34 52 cactus
execute as @a[scores={finished=60..}] at @s run setblock 38 -36 51 cactus
execute as @a[scores={finished=60..}] at @s run fill 31 -38 51 31 -36 51 cactus
execute as @a[scores={finished=60..}] at @s run fill 30 -40 54 30 -37 54 cactus
execute as @a[scores={finished=60..}] at @s run fill 28 -40 19 28 -37 19 cactus
execute as @a[scores={finished=60..}] at @s run fill 27 -40 35 27 -38 35 cactus
execute as @a[scores={finished=60..}] at @s run fill 26 -40 18 26 -39 18 cactus
execute as @a[scores={finished=60..}] at @s run fill 23 -42 24 23 -39 24 cactus
execute as @a[scores={finished=60..}] at @s run fill 22 -42 25 22 -41 25 cactus

execute as @a[scores={finished=60..}] at @s run fill 192 2 22 192 3 22 cactus
execute as @a[scores={finished=60..}] at @s run fill 191 2 24 191 5 24 cactus
execute as @a[scores={finished=60..}] at @s run fill 176 0 31 176 5 31 cactus
execute as @a[scores={finished=60..}] at @s run fill 174 0 32 174 3 32 cactus
execute as @a[scores={finished=60..}] at @s run fill 170 -1 16 170 2 16 cactus
execute as @a[scores={finished=60..}] at @s run fill 169 -1 14 169 1 14 cactus
execute as @a[scores={finished=60..}] at @s run fill 169 -1 44 169 1 44 cactus
execute as @a[scores={finished=60..}] at @s run fill 167 -1 46 167 2 46 cactus
execute as @a[scores={finished=60..}] at @s run fill 155 -4 17 155 -3 17 cactus
execute as @a[scores={finished=60..}] at @s run fill 155 -3 35 155 -2 35 cactus
execute as @a[scores={finished=60..}] at @s run fill 154 -4 18 154 -2 18 cactus
execute as @a[scores={finished=60..}] at @s run fill 154 -4 18 154 -2 18 cactus
execute as @a[scores={finished=60..}] at @s run fill 151 -6 4 151 -5 4 cactus
execute as @a[scores={finished=60..}] at @s run fill 151 -6 56 151 -3 56 cactus
execute as @a[scores={finished=60..}] at @s run setblock 150 -6 5 cactus
execute as @a[scores={finished=60..}] at @s run setblock 149 -6 54 cactus
execute as @a[scores={finished=60..}] at @s run fill 146 -8 60 146 -5 60 cactus
execute as @a[scores={finished=60..}] at @s run fill 146 -9 62 146 -7 62 cactus
execute as @a[scores={finished=60..}] at @s run setblock 145 -8 58 cactus


execute as @a[scores={finished=60..}] at @s run scoreboard players set @a hit_chicken 0
execute as @a[scores={finished=60..}] at @s run scoreboard players set @a finished 0






