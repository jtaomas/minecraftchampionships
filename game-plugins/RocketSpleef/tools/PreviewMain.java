import com.mcm.games.rocketspleef.map.Canvas;
import com.mcm.games.rocketspleef.map.MapGen;

import java.io.File;
import java.util.List;

/**
 * Offline preview: generates each old-RS map (pure Java, no Bukkit), checks it has at least 40 spawn spots and
 * writes MCMDUMP files for tools/render_preview.py.  Usage: java PreviewMain outDir [mapId...]
 */
public final class PreviewMain {
    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "target/offline");
        out.mkdirs();
        boolean bad = false;
        for (MapGen m : MapGen.all()) {
            if (args.length > 1) {
                boolean want = false;
                for (int i = 1; i < args.length; i++) if (args[i].equals(m.id())) want = true;
                if (!want) continue;
            }
            Canvas c = m.generate();
            List<Canvas.Pos> spawns = c.pickSpawns(48);
            System.out.println(m.id() + ": " + c.count() + " blocks, y " + c.minY() + ".." + c.maxY()
                    + ", horizontal radius " + c.maxHorizRadius() + ", spawn spots " + spawns.size());
            if (spawns.size() < 40) { bad = true; System.out.println("  TOO FEW SPAWNS for 40 players"); }
            c.writeDump(new File(out, m.id() + "_full.mcmdump.gz"), Integer.MAX_VALUE);
            // view from below: mirror y (an iso render of this looks up at the map from underneath)
            Canvas below = new Canvas();
            c.forEach((x, y, z, id) -> below.set(x, -y, z, id));
            below.writeDump(new File(out, m.id() + "_below.mcmdump.gz"), Integer.MAX_VALUE);
            // side elevation: up becomes -z, so a --top render shows the map from the side (looking along +z)
            Canvas side = new Canvas();
            c.forEach((x, y, z, id) -> side.set(x, z, -y, id));
            side.writeDump(new File(out, m.id() + "_side.mcmdump.gz"), Integer.MAX_VALUE);
        }
        if (bad) System.exit(2);
    }
}
