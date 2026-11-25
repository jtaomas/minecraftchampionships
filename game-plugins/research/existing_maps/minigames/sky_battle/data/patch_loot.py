p="data/build_chests.py"
s=open(p,encoding="utf-8").read()
s=s.replace('''    else: item=s; rest=""
    count=int(re.match(r"(\\d+)$",rest.strip()).group(1)) if rest.strip() and re.match(r"(\\d+)$",rest.strip()) else 1
    return item.replace("minecraft:",""),count,nbt''','''    else: item=s; rest=""
    count=1
    if rest.strip() and re.match(r"(\\d+)$",rest.strip()):
        count=int(rest.strip())
    elif "{" not in s:
        m=re.match(r"(.+?)\\s+(\\d+)$",s)
        if m:
            item=m.group(1); count=int(m.group(2))
    return item.replace("minecraft:",""),count,nbt''')
open(p,"w",encoding="utf-8").write(s)
