scoreboard players enable @a color

execute as @a[scores={color=1..}] at @s run tellraw @s ["",{"text":"---","color":"dark_gray"},{"text":" Select a Color","color":"gray"},{"text":" ---","color":"dark_gray"},{"text":"\n "},{"text":"[Red]","color":"red","clickEvent":{"action":"run_command","value":"/scoreboard players set @s selectColor 1"}},{"text":"     "},{"text":"[Orange]","color":"gold","clickEvent":{"action":"run_command","value":"/scoreboard players set @s selectColor 2"}},{"text":"\n "},{"text":"[Yellow]","color":"yellow","clickEvent":{"action":"run_command","value":"/scoreboard players set @s selectColor 3"}},{"text":"   "},{"text":"[Lime]","color":"green","clickEvent":{"action":"run_command","value":"/scoreboard players set @s selectColor 4"}},{"text":"\n "},{"text":"[Green]","color":"dark_green","clickEvent":{"action":"run_command","value":"/scoreboard players set @s selectColor 5"}},{"text":"   "},{"text":"[Cyan]","color":"dark_aqua","clickEvent":{"action":"run_command","value":"/scoreboard players set @s selectColor 6"}},{"text":"\n "},{"text":"[Aqua]","color":"aqua","clickEvent":{"action":"run_command","value":"/scoreboard players set @s selectColor 7"}},{"text":"    "},{"text":"[Blue]","color":"blue","clickEvent":{"action":"run_command","value":"/scoreboard players set @s selectColor 8"}},{"text":"\n "},{"text":"[Purple]","color":"dark_purple","clickEvent":{"action":"run_command","value":"/scoreboard players set @s selectColor 9"}},{"text":"  "},{"text":"[Pink]","color":"light_purple","clickEvent":{"action":"run_command","value":"/scoreboard players set @s selectColor 10"}},{"text":"\n"},{"text":"-------------------","color":"dark_gray"}]
execute as @a[scores={color=1..}] at @s run scoreboard players set @a color 0



#red
execute as @a[scores={selectColor=1}] at @s run tag @s add red
execute as @a[scores={selectColor=1}] at @s run tag @s remove orange
execute as @a[scores={selectColor=1}] at @s run tag @s remove yellow
execute as @a[scores={selectColor=1}] at @s run tag @s remove lime
execute as @a[scores={selectColor=1}] at @s run tag @s remove green
execute as @a[scores={selectColor=1}] at @s run tag @s remove cyan
execute as @a[scores={selectColor=1}] at @s run tag @s remove aqua
execute as @a[scores={selectColor=1}] at @s run tag @s remove blue
execute as @a[scores={selectColor=1}] at @s run tag @s remove purple
execute as @a[scores={selectColor=1}] at @s run tag @s remove pink

#orange
execute as @a[scores={selectColor=2}] at @s run tag @s add orange
execute as @a[scores={selectColor=2}] at @s run tag @s remove red
execute as @a[scores={selectColor=2}] at @s run tag @s remove yellow
execute as @a[scores={selectColor=2}] at @s run tag @s remove lime
execute as @a[scores={selectColor=2}] at @s run tag @s remove green
execute as @a[scores={selectColor=2}] at @s run tag @s remove cyan
execute as @a[scores={selectColor=2}] at @s run tag @s remove aqua
execute as @a[scores={selectColor=2}] at @s run tag @s remove blue
execute as @a[scores={selectColor=2}] at @s run tag @s remove purple
execute as @a[scores={selectColor=2}] at @s run tag @s remove pink

#yellow
execute as @a[scores={selectColor=3}] at @s run tag @s add yellow
execute as @a[scores={selectColor=3}] at @s run tag @s remove red
execute as @a[scores={selectColor=3}] at @s run tag @s remove orange
execute as @a[scores={selectColor=3}] at @s run tag @s remove lime
execute as @a[scores={selectColor=3}] at @s run tag @s remove green
execute as @a[scores={selectColor=3}] at @s run tag @s remove cyan
execute as @a[scores={selectColor=3}] at @s run tag @s remove aqua
execute as @a[scores={selectColor=3}] at @s run tag @s remove blue
execute as @a[scores={selectColor=3}] at @s run tag @s remove purple
execute as @a[scores={selectColor=3}] at @s run tag @s remove pink

#lime
execute as @a[scores={selectColor=4}] at @s run tag @s add lime
execute as @a[scores={selectColor=4}] at @s run tag @s remove red
execute as @a[scores={selectColor=4}] at @s run tag @s remove orange
execute as @a[scores={selectColor=4}] at @s run tag @s remove yellow
execute as @a[scores={selectColor=4}] at @s run tag @s remove green
execute as @a[scores={selectColor=4}] at @s run tag @s remove cyan
execute as @a[scores={selectColor=4}] at @s run tag @s remove aqua
execute as @a[scores={selectColor=4}] at @s run tag @s remove blue
execute as @a[scores={selectColor=4}] at @s run tag @s remove purple
execute as @a[scores={selectColor=4}] at @s run tag @s remove pink

#green
execute as @a[scores={selectColor=5}] at @s run tag @s add green
execute as @a[scores={selectColor=5}] at @s run tag @s remove red
execute as @a[scores={selectColor=5}] at @s run tag @s remove orange
execute as @a[scores={selectColor=5}] at @s run tag @s remove yellow
execute as @a[scores={selectColor=5}] at @s run tag @s remove lime
execute as @a[scores={selectColor=5}] at @s run tag @s remove cyan
execute as @a[scores={selectColor=5}] at @s run tag @s remove aqua
execute as @a[scores={selectColor=5}] at @s run tag @s remove blue
execute as @a[scores={selectColor=5}] at @s run tag @s remove purple
execute as @a[scores={selectColor=5}] at @s run tag @s remove pink

#cyan
execute as @a[scores={selectColor=6}] at @s run tag @s add cyan
execute as @a[scores={selectColor=6}] at @s run tag @s remove red
execute as @a[scores={selectColor=6}] at @s run tag @s remove orange
execute as @a[scores={selectColor=6}] at @s run tag @s remove yellow
execute as @a[scores={selectColor=6}] at @s run tag @s remove lime
execute as @a[scores={selectColor=6}] at @s run tag @s remove green
execute as @a[scores={selectColor=6}] at @s run tag @s remove aqua
execute as @a[scores={selectColor=6}] at @s run tag @s remove blue
execute as @a[scores={selectColor=6}] at @s run tag @s remove purple
execute as @a[scores={selectColor=6}] at @s run tag @s remove pink

#aqua
execute as @a[scores={selectColor=7}] at @s run tag @s add aqua
execute as @a[scores={selectColor=7}] at @s run tag @s remove red
execute as @a[scores={selectColor=7}] at @s run tag @s remove orange
execute as @a[scores={selectColor=7}] at @s run tag @s remove yellow
execute as @a[scores={selectColor=7}] at @s run tag @s remove lime
execute as @a[scores={selectColor=7}] at @s run tag @s remove green
execute as @a[scores={selectColor=7}] at @s run tag @s remove cyan
execute as @a[scores={selectColor=7}] at @s run tag @s remove blue
execute as @a[scores={selectColor=7}] at @s run tag @s remove purple
execute as @a[scores={selectColor=7}] at @s run tag @s remove pink

#blue
execute as @a[scores={selectColor=8}] at @s run tag @s add blue
execute as @a[scores={selectColor=8}] at @s run tag @s remove red
execute as @a[scores={selectColor=8}] at @s run tag @s remove orange
execute as @a[scores={selectColor=8}] at @s run tag @s remove yellow
execute as @a[scores={selectColor=8}] at @s run tag @s remove lime
execute as @a[scores={selectColor=8}] at @s run tag @s remove green
execute as @a[scores={selectColor=8}] at @s run tag @s remove cyan
execute as @a[scores={selectColor=8}] at @s run tag @s remove aqua
execute as @a[scores={selectColor=8}] at @s run tag @s remove purple
execute as @a[scores={selectColor=8}] at @s run tag @s remove pink

#purple
execute as @a[scores={selectColor=9}] at @s run tag @s add purple
execute as @a[scores={selectColor=9}] at @s run tag @s remove red
execute as @a[scores={selectColor=9}] at @s run tag @s remove orange
execute as @a[scores={selectColor=9}] at @s run tag @s remove yellow
execute as @a[scores={selectColor=9}] at @s run tag @s remove lime
execute as @a[scores={selectColor=9}] at @s run tag @s remove green
execute as @a[scores={selectColor=9}] at @s run tag @s remove cyan
execute as @a[scores={selectColor=9}] at @s run tag @s remove aqua
execute as @a[scores={selectColor=9}] at @s run tag @s remove blue
execute as @a[scores={selectColor=9}] at @s run tag @s remove pink

#pink
execute as @a[scores={selectColor=10}] at @s run tag @s add pink
execute as @a[scores={selectColor=10}] at @s run tag @s remove red
execute as @a[scores={selectColor=10}] at @s run tag @s remove orange
execute as @a[scores={selectColor=10}] at @s run tag @s remove yellow
execute as @a[scores={selectColor=10}] at @s run tag @s remove lime
execute as @a[scores={selectColor=10}] at @s run tag @s remove green
execute as @a[scores={selectColor=10}] at @s run tag @s remove cyan
execute as @a[scores={selectColor=10}] at @s run tag @s remove aqua
execute as @a[scores={selectColor=10}] at @s run tag @s remove blue
execute as @a[scores={selectColor=10}] at @s run tag @s remove purple





