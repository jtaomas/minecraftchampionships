import sys, struct, io, collections, json
import numpy as np
from amulet.leveldb import LevelDB

class R:
    def __init__(s,b): s.b=b; s.p=0
    def u8(s): v=s.b[s.p]; s.p+=1; return v
    def i16(s): v=struct.unpack_from("<h",s.b,s.p)[0]; s.p+=2; return v
    def i32(s): v=struct.unpack_from("<i",s.b,s.p)[0]; s.p+=4; return v
    def i64(s): v=struct.unpack_from("<q",s.b,s.p)[0]; s.p+=8; return v
    def f32(s): v=struct.unpack_from("<f",s.b,s.p)[0]; s.p+=4; return v
    def f64(s): v=struct.unpack_from("<d",s.b,s.p)[0]; s.p+=8; return v
    def str(s): n=s.i16() & 0xffff; v=s.b[s.p:s.p+n].decode("utf-8","replace"); s.p+=n; return v
def payload(r,t):
    if t==1: return r.u8()
    if t==2: return r.i16()
    if t==3: return r.i32()
    if t==4: return r.i64()
    if t==5: return r.f32()
    if t==6: return r.f64()
    if t==7: n=r.i32(); v=list(r.b[r.p:r.p+n]); r.p+=n; return v
    if t==8: return r.str()
    if t==9:
        et=r.u8(); n=r.i32(); return [payload(r,et) for _ in range(n)]
    if t==10:
        d={}
        while True:
            tt=r.u8()
            if tt==0: break
            nm=r.str(); d[nm]=payload(r,tt)
        return d
    if t==11: n=r.i32(); v=[r.i32() for _ in range(n)]; return v
    if t==12: n=r.i32(); v=[r.i64() for _ in range(n)]; return v
    raise ValueError(t)
def nbt(r):
    t=r.u8()
    if t==0: return None
    r.str(); return payload(r,t)
def stateStr(c):
    name=c.get("name","minecraft:air")
    st=c.get("states",{})
    if st:
        return name+"|"+",".join(f"{k}={('true' if v==1 else 'false') if False else v}" for k,v in sorted(st.items()))
    return name
def decode_sub(v):
    r=R(v); ver=r.u8()
    if ver in (8,9):
        n=r.u8()
        if ver==9: r.u8()
    elif ver==1: n=1
    else: raise ValueError("subchunk ver %d"%ver)
    layers=[]
    for _ in range(n):
        h=r.u8(); bits=h>>1; rt=h&1
        if rt: raise ValueError("runtime palette")
        if bits==0:
            idx=np.zeros(4096,dtype=np.int64)
            cnt=r.i32() if False else None
        else:
            per=32//bits; words=(4096+per-1)//per
            arr=np.frombuffer(r.b,dtype="<u4",count=words,offset=r.p).astype(np.uint64); r.p+=words*4
            sh=np.arange(per,dtype=np.uint64)*np.uint64(bits)
            idx=((arr[:,None]>>sh)&np.uint64((1<<bits)-1)).reshape(-1)[:4096].astype(np.int64)
        np_=r.i32()
        pal=[stateStr(nbt(r)) for _ in range(np_)]
        layers.append((idx,pal))
    return layers
def main(dbpath,out):
    db=LevelDB(dbpath)
    pal=[];pidx={}
    def pid(s):
        i=pidx.get(s)
        if i is None: i=len(pal); pal.append(s); pidx[s]=i
        return i
    XS=[];YS=[];ZS=[];PS=[];LS=[]
    bes=[]
    chunks=set()
    for k,v in db.items():
        k=bytes(k)
        if len(k)==10 and k[8]==0x2f:
            cx,cz=struct.unpack("<ii",k[:8]); sy=struct.unpack("b",k[9:10])[0]
            chunks.add((cx,cz))
            try: layers=decode_sub(bytes(v))
            except Exception as e: print("ERR",cx,cz,sy,e); continue
            for li,(idx,pl) in enumerate(layers):
                ids=np.array([pid(p) for p in pl],dtype=np.int32)
                isair=np.array([p.split("|")[0] in("minecraft:air",) for p in pl])
                gid=ids[np.minimum(idx,len(pl)-1)]
                sel=np.nonzero(~isair[np.minimum(idx,len(pl)-1)])[0]
                if len(sel)==0: continue
                lx=sel>>8; lz=(sel>>4)&15; ly=sel&15
                XS.append(cx*16+lx); ZS.append(cz*16+lz); YS.append(sy*16+ly); PS.append(gid[sel]); LS.append(np.full(len(sel),li))
        elif len(k)==9 and k[8]==0x31:
            cx,cz=struct.unpack("<ii",k[:8])
            r=R(bytes(v))
            while r.p<len(r.b):
                try: d=nbt(r)
                except Exception: break
                if d is None: break
                bes.append(d)
    X=np.concatenate(XS);Y=np.concatenate(YS);Z=np.concatenate(ZS);P=np.concatenate(PS);L=np.concatenate(LS)
    np.savez_compressed(out,X=X,Y=Y,Z=Z,P=P,L=L,pal=np.array(pal,dtype=object))
    json.dump(bes,open(out.replace(".npz","_be.json"),"w"),default=str)
    print(len(X),"blocks",len(pal),"palette",len(chunks),"chunks",len(bes),"block entities")
    print("layers:",collections.Counter(L.tolist()))
main(sys.argv[1],sys.argv[2])
