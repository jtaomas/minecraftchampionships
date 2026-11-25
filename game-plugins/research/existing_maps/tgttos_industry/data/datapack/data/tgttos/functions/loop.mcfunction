kill @e[type=item]
effect give @e[type=chicken] resistance 1 5 true
fill 83 -30 10 64 -33 12 air replace #minecraft:mineable/pickaxe

#finish
execute as @a[scores={hit_chicken=1..}] at @s run tag @s add finished

execute as @a[tag=finished] at @s run gamemode spectator @s
execute as @a[tag=finished] at @s run scoreboard players add @s finished 1
execute as @a[tag=finished] at @s run tag @a remove playing
execute as @a[scores={finished=1..5}] at @s run title @a times 0 100 20
execute as @a[scores={finished=1..5}] at @s run title @s subtitle ["",{"text":"Time: ","bold":true,"color":"gray"},{"score":{"name":"@s","objective":"playtimeMi2"},"bold":true,"color":"gray"},{"score":{"name":"@s","objective":"playtimeMi1"},"bold":true,"color":"gray"},{"text":":","bold":true,"color":"gray"},{"score":{"name":"@s","objective":"playtimeSe2"},"bold":true,"color":"gray"},{"score":{"name":"@s","objective":"playtimeSe1"},"bold":true,"color":"gray"},{"text":":","bold":true,"color":"gray"},{"score":{"name":"@s","objective":"playtimeMs2"},"bold":true,"color":"gray"},{"score":{"name":"@s","objective":"playtimeMs1"},"bold":true,"color":"gray"}]
execute as @a[scores={finished=1..5}] at @s run title @a title {"text":"Finished","color":"green"}

execute as @a[scores={finished=60..}] at @s run execute as @e[tag=remove_block] at @s run fill ~-15 ~-25 ~-15 ~15 ~5 ~15 air replace red_concrete
execute as @a[scores={finished=60..}] at @s run execute as @e[tag=remove_block] at @s run fill ~-15 ~-25 ~-15 ~15 ~5 ~15 air replace orange_concrete
execute as @a[scores={finished=60..}] at @s run execute as @e[tag=remove_block] at @s run fill ~-15 ~-25 ~-15 ~15 ~5 ~15 air replace yellow_concrete
execute as @a[scores={finished=60..}] at @s run execute as @e[tag=remove_block] at @s run fill ~-15 ~-25 ~-15 ~15 ~5 ~15 air replace lime_concrete
execute as @a[scores={finished=60..}] at @s run execute as @e[tag=remove_block] at @s run fill ~-15 ~-25 ~-15 ~15 ~5 ~15 air replace green_concrete
execute as @a[scores={finished=60..}] at @s run execute as @e[tag=remove_block] at @s run fill ~-15 ~-25 ~-15 ~15 ~5 ~15 air replace cyan_concrete
execute as @a[scores={finished=60..}] at @s run execute as @e[tag=remove_block] at @s run fill ~-15 ~-25 ~-15 ~15 ~5 ~15 air replace light_blue_concrete
execute as @a[scores={finished=60..}] at @s run execute as @e[tag=remove_block] at @s run fill ~-15 ~-25 ~-15 ~15 ~5 ~15 air replace blue_concrete
execute as @a[scores={finished=60..}] at @s run execute as @e[tag=remove_block] at @s run fill ~-15 ~-25 ~-15 ~15 ~5 ~15 air replace purple_concrete
execute as @a[scores={finished=60..}] at @s run execute as @e[tag=remove_block] at @s run fill ~-15 ~-25 ~-15 ~15 ~5 ~15 air replace pink_concrete

execute as @a[scores={finished=60..}] at @s run kill @e[type=armor_stand,tag=remove_block]
execute as @a[scores={finished=65}] at @s run function tgttos:block_reset

execute as @a[scores={finished=140..}] at @s run fill 63 -29 9 84 -30 9 barrier
execute as @a[scores={finished=140..}] at @s run fill 84 -29 10 84 -33 13 barrier
execute as @a[scores={finished=140..}] at @s run fill 84 -29 13 63 -33 13 barrier
execute as @a[scores={finished=140..}] at @s run fill 63 -29 13 63 -33 10 barrier
execute as @a[scores={finished=140..}] at @s run tp @a 73 -33 11
execute as @a[scores={finished=140..}] at @s run tag @a remove finished
execute as @a[scores={finished=140..}] at @s run gamemode adventure @a
execute as @a[scores={finished=140..}] at @s run scoreboard players set @a hit_chicken 0

execute as @a[scores={finished=140..}] at @s run clear @a
execute as @a[scores={finished=140..}] at @s run scoreboard players set @a playtimeMs1 0
execute as @a[scores={finished=140..}] at @s run scoreboard players set @a playtimeMs2 0
execute as @a[scores={finished=140..}] at @s run scoreboard players set @a playtimeSe1 0
execute as @a[scores={finished=140..}] at @s run scoreboard players set @a playtimeSe2 0
execute as @a[scores={finished=140..}] at @s run scoreboard players set @a playtimeMi1 0
execute as @a[scores={finished=140..}] at @s run scoreboard players set @a playtimeMi2 0

execute as @a[scores={finished=140..}] at @s run scoreboard players set @s finished 0



#timer
execute as @a[tag=playing] at @s run scoreboard players add @s playtimeMs1 5
execute as @a[tag=playing] at @s run scoreboard players add @s spawntime 1

execute as @a[scores={spawntime=20..}] at @s run summon armor_stand ~ ~10 ~ {NoGravity:1b,Silent:1b,Invulnerable:1b,Small:1b,Invisible:1b,NoBasePlate:1b,Tags:["remove_block"]}
execute as @a[scores={respawn=..-45}] at @s run execute as @s[scores={spawntime=5..}] at @s run summon armor_stand ~ ~10 ~ {NoGravity:1b,Silent:1b,Invulnerable:1b,Small:1b,Invisible:1b,NoBasePlate:1b,Tags:["remove_block"]}
execute as @a[scores={spawntime=20..}] at @s run scoreboard players set @s spawntime 0


execute as @a[scores={playtimeMs1=10..}] at @s run scoreboard players add @s playtimeMs2 1
execute as @a[scores={playtimeMs1=10..}] at @s run scoreboard players set @s playtimeMs1 0
execute as @a[scores={playtimeMs2=10..}] at @s run scoreboard players add @s playtimeSe1 1
execute as @a[scores={playtimeMs2=10..}] at @s run scoreboard players set @s playtimeMs2 0
execute as @a[scores={playtimeSe1=10..}] at @s run scoreboard players add @s playtimeSe2 1
execute as @a[scores={playtimeSe1=10..}] at @s run scoreboard players set @s playtimeSe1 0
execute as @a[scores={playtimeSe2=6..}] at @s run scoreboard players add @s playtimeMi1 1
execute as @a[scores={playtimeSe2=6..}] at @s run scoreboard players set @s playtimeSe2 0
execute as @a[scores={playtimeMi1=10..}] at @s run scoreboard players add @s playtimeMi2 1
execute as @a[scores={playtimeMi1=10..}] at @s run scoreboard players set @s playtimeMi1 0


execute as @a[tag=red] at @s run title @s actionbar ["",{"score":{"name":"@s","objective":"playtimeMi2"},"bold":true,"color":"red"},{"score":{"name":"@s","objective":"playtimeMi1"},"bold":true,"color":"red"},{"text":":","bold":true,"color":"red"},{"score":{"name":"@s","objective":"playtimeSe2"},"bold":true,"color":"red"},{"score":{"name":"@s","objective":"playtimeSe1"},"bold":true,"color":"red"},{"text":":","bold":true,"color":"red"},{"score":{"name":"@s","objective":"playtimeMs2"},"bold":true,"color":"red"},{"score":{"name":"@s","objective":"playtimeMs1"},"bold":true,"color":"red"}]
execute as @a[tag=orange] at @s run title @s actionbar ["",{"score":{"name":"@s","objective":"playtimeMi2"},"bold":true,"color":"gold"},{"score":{"name":"@s","objective":"playtimeMi1"},"bold":true,"color":"gold"},{"text":":","bold":true,"color":"gold"},{"score":{"name":"@s","objective":"playtimeSe2"},"bold":true,"color":"gold"},{"score":{"name":"@s","objective":"playtimeSe1"},"bold":true,"color":"gold"},{"text":":","bold":true,"color":"gold"},{"score":{"name":"@s","objective":"playtimeMs2"},"bold":true,"color":"gold"},{"score":{"name":"@s","objective":"playtimeMs1"},"bold":true,"color":"gold"}]
execute as @a[tag=yellow] at @s run title @s actionbar ["",{"score":{"name":"@s","objective":"playtimeMi2"},"bold":true,"color":"yellow"},{"score":{"name":"@s","objective":"playtimeMi1"},"bold":true,"color":"yellow"},{"text":":","bold":true,"color":"yellow"},{"score":{"name":"@s","objective":"playtimeSe2"},"bold":true,"color":"yellow"},{"score":{"name":"@s","objective":"playtimeSe1"},"bold":true,"color":"yellow"},{"text":":","bold":true,"color":"yellow"},{"score":{"name":"@s","objective":"playtimeMs2"},"bold":true,"color":"yellow"},{"score":{"name":"@s","objective":"playtimeMs1"},"bold":true,"color":"yellow"}]
execute as @a[tag=lime] at @s run title @s actionbar ["",{"score":{"name":"@s","objective":"playtimeMi2"},"bold":true,"color":"green"},{"score":{"name":"@s","objective":"playtimeMi1"},"bold":true,"color":"green"},{"text":":","bold":true,"color":"green"},{"score":{"name":"@s","objective":"playtimeSe2"},"bold":true,"color":"green"},{"score":{"name":"@s","objective":"playtimeSe1"},"bold":true,"color":"green"},{"text":":","bold":true,"color":"green"},{"score":{"name":"@s","objective":"playtimeMs2"},"bold":true,"color":"green"},{"score":{"name":"@s","objective":"playtimeMs1"},"bold":true,"color":"green"}]
execute as @a[tag=green] at @s run title @s actionbar ["",{"score":{"name":"@s","objective":"playtimeMi2"},"bold":true,"color":"dark_green"},{"score":{"name":"@s","objective":"playtimeMi1"},"bold":true,"color":"dark_green"},{"text":":","bold":true,"color":"dark_green"},{"score":{"name":"@s","objective":"playtimeSe2"},"bold":true,"color":"dark_green"},{"score":{"name":"@s","objective":"playtimeSe1"},"bold":true,"color":"dark_green"},{"text":":","bold":true,"color":"dark_green"},{"score":{"name":"@s","objective":"playtimeMs2"},"bold":true,"color":"dark_green"},{"score":{"name":"@s","objective":"playtimeMs1"},"bold":true,"color":"dark_green"}]
execute as @a[tag=cyan] at @s run title @s actionbar ["",{"score":{"name":"@s","objective":"playtimeMi2"},"bold":true,"color":"dark_aqua"},{"score":{"name":"@s","objective":"playtimeMi1"},"bold":true,"color":"dark_aqua"},{"text":":","bold":true,"color":"dark_aqua"},{"score":{"name":"@s","objective":"playtimeSe2"},"bold":true,"color":"dark_aqua"},{"score":{"name":"@s","objective":"playtimeSe1"},"bold":true,"color":"dark_aqua"},{"text":":","bold":true,"color":"dark_aqua"},{"score":{"name":"@s","objective":"playtimeMs2"},"bold":true,"color":"dark_aqua"},{"score":{"name":"@s","objective":"playtimeMs1"},"bold":true,"color":"dark_aqua"}]
execute as @a[tag=aqua] at @s run title @s actionbar ["",{"score":{"name":"@s","objective":"playtimeMi2"},"bold":true,"color":"aqua"},{"score":{"name":"@s","objective":"playtimeMi1"},"bold":true,"color":"aqua"},{"text":":","bold":true,"color":"aqua"},{"score":{"name":"@s","objective":"playtimeSe2"},"bold":true,"color":"aqua"},{"score":{"name":"@s","objective":"playtimeSe1"},"bold":true,"color":"aqua"},{"text":":","bold":true,"color":"aqua"},{"score":{"name":"@s","objective":"playtimeMs2"},"bold":true,"color":"aqua"},{"score":{"name":"@s","objective":"playtimeMs1"},"bold":true,"color":"aqua"}]
execute as @a[tag=blue] at @s run title @s actionbar ["",{"score":{"name":"@s","objective":"playtimeMi2"},"bold":true,"color":"blue"},{"score":{"name":"@s","objective":"playtimeMi1"},"bold":true,"color":"blue"},{"text":":","bold":true,"color":"blue"},{"score":{"name":"@s","objective":"playtimeSe2"},"bold":true,"color":"blue"},{"score":{"name":"@s","objective":"playtimeSe1"},"bold":true,"color":"blue"},{"text":":","bold":true,"color":"blue"},{"score":{"name":"@s","objective":"playtimeMs2"},"bold":true,"color":"blue"},{"score":{"name":"@s","objective":"playtimeMs1"},"bold":true,"color":"blue"}]
execute as @a[tag=purple] at @s run title @s actionbar ["",{"score":{"name":"@s","objective":"playtimeMi2"},"bold":true,"color":"dark_purple"},{"score":{"name":"@s","objective":"playtimeMi1"},"bold":true,"color":"dark_purple"},{"text":":","bold":true,"color":"dark_purple"},{"score":{"name":"@s","objective":"playtimeSe2"},"bold":true,"color":"dark_purple"},{"score":{"name":"@s","objective":"playtimeSe1"},"bold":true,"color":"dark_purple"},{"text":":","bold":true,"color":"dark_purple"},{"score":{"name":"@s","objective":"playtimeMs2"},"bold":true,"color":"dark_purple"},{"score":{"name":"@s","objective":"playtimeMs1"},"bold":true,"color":"dark_purple"}]
execute as @a[tag=pink] at @s run title @s actionbar ["",{"score":{"name":"@s","objective":"playtimeMi2"},"bold":true,"color":"light_purple"},{"score":{"name":"@s","objective":"playtimeMi1"},"bold":true,"color":"light_purple"},{"text":":","bold":true,"color":"light_purple"},{"score":{"name":"@s","objective":"playtimeSe2"},"bold":true,"color":"light_purple"},{"score":{"name":"@s","objective":"playtimeSe1"},"bold":true,"color":"light_purple"},{"text":":","bold":true,"color":"light_purple"},{"score":{"name":"@s","objective":"playtimeMs2"},"bold":true,"color":"light_purple"},{"score":{"name":"@s","objective":"playtimeMs1"},"bold":true,"color":"light_purple"}]


#items
item replace entity @a hotbar.0 with iron_pickaxe{CanDestroy:["minecraft:red_concrete","minecraft:blue_concrete","minecraft:lime_concrete","minecraft:pink_concrete","minecraft:green_concrete","minecraft:orange_concrete","minecraft:purple_concrete","minecraft:yellow_concrete","minecraft:light_blue_concrete","minecraft:cyan_concrete","black_concrete","white_concrete"],Unbreakable:1b,Enchantments:[{id:"minecraft:efficiency",lvl:2s}]} 1

item replace entity @a[tag=red] weapon.offhand with red_concrete{CanPlaceOn:["minecraft:red_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 2
item replace entity @a[tag=red] weapon.offhand with red_concrete{CanPlaceOn:["minecraft:red_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 1

item replace entity @a[tag=orange] weapon.offhand with orange_concrete{CanPlaceOn:["minecraft:orange_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 2
item replace entity @a[tag=orange] weapon.offhand with orange_concrete{CanPlaceOn:["minecraft:orange_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 1

item replace entity @a[tag=yellow] weapon.offhand with yellow_concrete{CanPlaceOn:["minecraft:yellow_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 2
item replace entity @a[tag=yellow] weapon.offhand with yellow_concrete{CanPlaceOn:["minecraft:yellow_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 1

item replace entity @a[tag=lime] weapon.offhand with lime_concrete{CanPlaceOn:["minecraft:lime_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 2
item replace entity @a[tag=lime] weapon.offhand with lime_concrete{CanPlaceOn:["minecraft:lime_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 1

item replace entity @a[tag=green] weapon.offhand with green_concrete{CanPlaceOn:["minecraft:green_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 2
item replace entity @a[tag=green] weapon.offhand with green_concrete{CanPlaceOn:["minecraft:green_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 1

item replace entity @a[tag=cyan] weapon.offhand with cyan_concrete{CanPlaceOn:["minecraft:cyan_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 2
item replace entity @a[tag=cyan] weapon.offhand with cyan_concrete{CanPlaceOn:["minecraft:cyan_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 1

item replace entity @a[tag=aqua] weapon.offhand with light_blue_concrete{CanPlaceOn:["minecraft:light_blue_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 2
item replace entity @a[tag=aqua] weapon.offhand with light_blue_concrete{CanPlaceOn:["minecraft:light_blue_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 1

item replace entity @a[tag=blue] weapon.offhand with blue_concrete{CanPlaceOn:["minecraft:blue_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 2
item replace entity @a[tag=blue] weapon.offhand with blue_concrete{CanPlaceOn:["minecraft:blue_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 1

item replace entity @a[tag=purple] weapon.offhand with purple_concrete{CanPlaceOn:["minecraft:purple_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 2
item replace entity @a[tag=purple] weapon.offhand with purple_concrete{CanPlaceOn:["minecraft:purple_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 1

item replace entity @a[tag=pink] weapon.offhand with pink_concrete{CanPlaceOn:["minecraft:pink_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 2
item replace entity @a[tag=pink] weapon.offhand with pink_concrete{CanPlaceOn:["minecraft:pink_concrete","minecraft:light_blue_stained_glass","minecraft:blackstone","minecraft:white_concrete","minecraft:red_concrete","minecraft:black_concrete","minecraft:yellow_concrete","minecraft:blackstone_stairs","minecraft:gray_wool","minecraft:polished_andesite","minecraft:smooth_stone","minecraft:smooth_stone_slab","minecraft:acacia_planks","cobblestone_wall","yellow_wool","black_wool","red_sandstone_stairs","red_sandstone","red_sandstone_wall","iron_trapdoor","acacia_fence","oak_fence"]} 1













