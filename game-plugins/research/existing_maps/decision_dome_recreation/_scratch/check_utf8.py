s=open("README.md",encoding="utf-8").read()
print(repr(s[1200:1450]))
print("arrows",s.count("→"),"times",s.count("×"),"en",s.count("–"),"replacement",s.count("\ufffd"))
