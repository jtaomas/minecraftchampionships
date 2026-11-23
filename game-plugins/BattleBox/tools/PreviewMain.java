import com.mcm.games.battlebox.arena.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Offline preview + checks: generates each theme into a Grid, validates staircases and the gate passages,
 * runs the sightline check and writes MCMDUMP files (no Bukkit needed).
 * Usage: java PreviewMain outDir [themeId...]
 *   <id>_full.mcmdump.gz   everything
 *   <id>_open.mcmdump.gz   ceiling/roof removed only
 *   <id>_cut.mcmdump.gz    ceiling removed and the two near walls lowered (cut-away)
 *   <id>_floor.mcmdump.gz  everything above y = 3 removed (ground-floor plan)
 * Exit code 2 if any staircase, gate passage or route (exit / lane) is broken (the render script then stops).
 */
public final class PreviewMain {
    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "previews");
        out.mkdirs();
        ArenaGeo geo = new ArenaGeo();
        boolean broken = false;
        StringBuilder report = new StringBuilder();
        for (ArenaTheme t : Themes.all()) {
            if (args.length > 1) {
                boolean want = false;
                for (int i = 1; i < args.length; i++) if (args[i].equals(t.id())) want = true;
                if (!want) continue;
            }
            Grid g = t.generate(geo, 12345L, "red", "blue", "white_concrete");
            System.out.println(t.id() + ": " + g.count() + " blocks, " + g.distinctIds() + " block types, "
                    + g.lanes().size() + " stair lanes, " + t.removedCarpets() + " carpets dropped");

            List<String> errs = g.validateStairs();
            for (String e : errs) System.out.println("  STAIRS BROKEN " + t.id() + ": " + e);
            if (!errs.isEmpty()) broken = true;
            else System.out.println("  staircases OK (" + g.lanes().size() + " lanes walked bottom to top)");
            for (int sg = -1; sg <= 1; sg += 2)
                for (int x = -geo.GATE_HALF; x <= geo.GATE_HALF; x++)
                    for (int y = 1; y <= 2; y++)
                        for (int a : new int[]{geo.W + 1, geo.R}) {
                            String b = g.get(x, y, sg * a);
                            if (!Shapes.passable(b)) { broken = true; System.out.println("  GATE BLOCKED " + t.id() + " at (" + x + "," + y + "," + sg * a + "): " + b); }
                        }

            boolean[] ok = {true};
            String rep = new Sightlines(g, geo, new Routes(g, geo).reachableSpots()).report(t.id(), ok);
            System.out.print(rep);
            if (!ok[0]) System.out.println("  WARNING " + t.id() + ": spawn sightlines not fully blocked");
            report.append(rep);

            boolean[] rok = {true};
            String rr = new Routes(g, geo).report(t.id(), rok);
            System.out.print(rr);
            if (!rok[0]) { broken = true; System.out.println("  ROUTES BROKEN " + t.id()); }
            report.append(rr);

            g.writeDump(new File(out, t.id() + "_full.mcmdump.gz"), null);
            final int ceil = geo.CEIL;
            final int ww = geo.W;
            g.writeDump(new File(out, t.id() + "_open.mcmdump.gz"), (x, y, z) -> y < ceil && (y < ceil - 2 || Math.abs(x) >= ww || Math.abs(z) >= ww));
            final int w = geo.W;
            g.writeDump(new File(out, t.id() + "_cut.mcmdump.gz"), (x, y, z) -> {
                if (y >= ceil) return false;
                boolean wall = Math.abs(x) >= w || Math.abs(z) >= w;
                if (!wall && y >= ceil - 2) return false;   // hanging trusses / lamps
                if ((z >= w || x >= w) && y > 2) return false;
                return true;
            });
            g.writeDump(new File(out, t.id() + "_floor.mcmdump.gz"), (x, y, z) -> y <= 3);
        }
        Files.write(new File(out, "sightlines.txt").toPath(), report.toString().getBytes(StandardCharsets.UTF_8));
        if (broken) System.exit(2);
    }
}
