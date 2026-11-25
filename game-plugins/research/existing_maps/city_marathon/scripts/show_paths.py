import json
p=json.load(open(r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data\paths.json"))
for k,v in p.items():
    print("==",k,len(v))
    for i in range(0,len(v),max(1,len(v)//12)):
        print(v[i], end="  ")
    print(v[-1])
