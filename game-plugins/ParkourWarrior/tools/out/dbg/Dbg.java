package com.mcm.games.parkourwarrior.map;
public class Dbg { public static void main(String[] a) {
  W w = CourseMap.build(20261124L);
  int x=Integer.parseInt(a[0]), z=Integer.parseInt(a[2]), y0=Integer.parseInt(a[1]);
  for (int y=y0-3;y<=y0+5;y++) System.out.println(y+" "+w.get(x,y,z));
  for (Ob o : w.obs) if (o.id.startsWith("E-HARD")) System.out.println(o.id+" origin "+o.ox+","+o.oy+","+o.oz);
}}
