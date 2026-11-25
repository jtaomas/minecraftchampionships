execute as @a[tag=starting] at @s run scoreboard players add @s countdown 1
execute as @a[scores={countdown=40..140}] at @s run title @a times 0 3 20
execute as @a[scores={countdown=40..140}] at @s run title @a title {"text":"Starting in","color":"aqua"}
execute as @a[scores={countdown=40..60}] at @s run title @a subtitle {"text":"5","bold":true}
execute as @a[scores={countdown=60..80}] at @s run title @a subtitle {"text":"4","bold":true}
execute as @a[scores={countdown=80..100}] at @s run title @a subtitle {"text":"3","bold":true}
execute as @a[scores={countdown=100..120}] at @s run title @a subtitle {"text":"2","bold":true}
execute as @a[scores={countdown=120..140}] at @s run title @a subtitle {"text":"1","bold":true}
execute as @a[scores={countdown=140..200}] at @s run title @a times 0 60 20
execute as @a[scores={countdown=140..200}] at @s run title @a title {"text":" ","color":"aqua"}
execute as @a[scores={countdown=140..200}] at @s run title @a subtitle {"text":"GO!!","bold":true,"color":"green"}

execute as @a[scores={countdown=140..}] at @s run tag @a add playing

execute as @a[scores={countdown=140..}] at @s run fill 63 -33 13 84 -29 9 air replace barrier
execute as @a[scores={countdown=140..}] at @s run tag @a remove starting
execute as @a[scores={countdown=140..}] at @s run scoreboard players set @a countdown 0








