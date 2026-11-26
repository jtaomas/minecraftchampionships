import sys, numpy as np
sys.path.insert(0, r"C:\Users\LeonS\Desktop\Mcc\tools")
import extract_anvil as E

def load(world, out_npz, miny=-64, maxy=320):
    pal=[]; pidx={}
    def pid(s):
        i=pidx.get(s)
        if i is None: i=len(pal); pal.append(s); pidx[s]=i
        return i
    XS=[];YS=[];ZS=[];PS=[]
    for cx,cz,secs in E.iter_region_chunks(world):
        for s in secs:
            y_byte=int(s.get("Y",0)); base=y_byte*16
            bs=s.get("block_states")
            if not bs or not bs.get("palette"):
                if s.get("Palette") is not None and s.get("BlockStates") is not None:
                    bs={"palette":s["Palette"],"data":s["BlockStates"]}
                else:
                    continue
            palette=[E.state_string(p) for p in bs["palette"]]
            n=len(palette)
            ids=np.array([pid(p) for p in palette],dtype=np.int32)
            if n==1:
                idx=np.zeros(4096,dtype=np.int64)
            else:
                data=bs.get("data")
                if data is None: continue
                bits=max(4,(n-1).bit_length()); per=64//bits; mask=(1<<bits)-1
                longs=np.asarray(data,dtype=np.uint64)
                sh=np.arange(per,dtype=np.uint64)*np.uint64(bits)
                idx=((longs[:,None]>>sh)&np.uint64(mask)).reshape(-1)[:4096].astype(np.int64)
                idx=np.minimum(idx,n-1)
            gid=ids[idx]
            isair=np.array([p in E.AIR for p in palette])[idx]
            sel=np.nonzero(~isair)[0]
            if len(sel)==0: continue
            XS.append(cx*16+(sel&15)); ZS.append(cz*16+((sel>>4)&15)); YS.append(base+(sel>>8)); PS.append(gid[sel])
    X=np.concatenate(XS);Y=np.concatenate(YS);Z=np.concatenate(ZS);P=np.concatenate(PS)
    np.savez_compressed(out_npz,X=X,Y=Y,Z=Z,P=P,pal=np.array(pal,dtype=object),allow_pickle=True)
    print(world,len(X),"blocks",len(pal),"palette")
if __name__=="__main__":
    load(sys.argv[1],sys.argv[2])

