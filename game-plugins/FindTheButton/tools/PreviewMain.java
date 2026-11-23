import com.mcm.games.findthebutton.map.FtbMap;
import com.mcm.games.findthebutton.map.Grid;
import com.mcm.games.findthebutton.map.Maps;
import com.mcm.games.findthebutton.map.Reach;
import com.mcm.games.findthebutton.map.Spot;
import com.mcm.games.findthebutton.map.States;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Offline tool (no Paper on the classpath): builds every map, runs the reachability validation for every hiding
 * spot plus the staircase check, and writes MCMDUMP files for the preview renderer:
 *   <id>_cut.mcmdump.gz    cut-away (ceiling and the two camera-side walls removed)
 *   <id>_spots.mcmdump.gz  same, with every hiding spot marked by a redstone block (for the top-down spot map)
 * Exit code 1 if any map has a problem.
 * Usage: java PreviewMain outDir [mapId...]
 */
public final class PreviewMain {
    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "target/offline");
        out.mkdirs();
        List<String> only = new ArrayList<>();
        for (int i = 1; i < args.length; i++) only.add(args[i]);
        boolean failed = false;
        for (FtbMap m : Maps.all()) {
            if (!only.isEmpty() && !only.contains(m.id())) continue;
            Grid g = m.grid();
            List<Reach.Result> results = new ArrayList<>();
            List<String> problems = Reach.validate(m, results);
            Reach r = new Reach(m);
            System.out.println("== " + m.displayName() + " (" + m.id() + "): " + g.count() + " blocks, "
                    + g.distinctIds() + " block types, " + m.spots().size() + " spots, "
                    + r.reachableNodes() + " reachable standing nodes, " + g.lanes().size() + " staircase(s)");
            for (Reach.Result res : results) {
                Spot s = res.spot();
                System.out.printf("   %-4s %-38s %-6s (%d,%d,%d) %-5s %s%n", res.ok() ? "OK" : "FAIL", s.name(), s.difficulty(),
                        s.x(), s.y(), s.z(), s.support(), res.detail());
            }
            for (String p : problems) System.out.println("   PROBLEM: " + p);
            if (!problems.isEmpty()) failed = true;
            final int h = m.previewCutY();
            Grid.DumpFilter cut = (x, y, z) -> !States.id(g.get(x, y, z)).equals("light") && (y <= 0 || (y <= h && x <= 20 && z <= 20));
            g.writeDump(new File(out, m.id() + "_cut.mcmdump.gz"), cut);
            Grid marked = new Grid(g.minX, g.minY, g.minZ, g.maxX, g.maxY, g.maxZ);
            g.forEach(marked::set);
            for (Spot s : m.spots()) marked.set(s.x(), s.y(), s.z(), "minecraft:redstone_block");
            marked.writeDump(new File(out, m.id() + "_spots.mcmdump.gz"), cut);
        }
        System.out.println(failed ? "VALIDATION FAILED" : "ALL MAPS VALID");
        if (failed) System.exit(1);
    }
}
