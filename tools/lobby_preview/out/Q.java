import com.mcm.lobby.*;
public class Q { public static void main(String[] a){ Vox v=new Vox(); new HubGenerator(v,20261123L).generate();
 int n=0,l=0; java.util.Map<String,Integer> m=new java.util.TreeMap<>();
 for(int x=-45;x<=45;x++)for(int z=35;z<=125;z++){double r=Math.hypot(x,z-80); if(r>38&&r<=40.5){n++; String s=v.id(x,71,z); m.merge(String.valueOf(s),1,Integer::sum);} }
 System.out.println(n+" "+m); } }
