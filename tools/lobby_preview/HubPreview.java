import com.mcm.lobby.HubGenerator;
import com.mcm.lobby.HubLayout;
import com.mcm.lobby.Vox;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * Offline hub generator runner. Usage:
 *   java HubPreview outDir name:x1:y1:z1:x2:y2:z2[:step] ...
 * Writes outDir/name.mcmdump.gz for each region (step > 1 = downsample by taking every step-th column/row).
 */
public class HubPreview {
    public static void main(String[] args) throws Exception {
        long t0 = System.currentTimeMillis();
        Vox v = new Vox();
        HubLayout L = new HubGenerator(v, 20261123L).generate();
        long t1 = System.currentTimeMillis();
        System.out.println("generated " + v.countBlocks() + " blocks in " + (t1 - t0) + " ms; palette " + v.palette().size()
                + "; bounds y " + v.minY() + ".." + v.maxY());
        System.out.println("spawn " + L.spawnX + "," + L.spawnY + "," + L.spawnZ + "  dome " + L.domeX + "," + L.domeZ
                + " stage " + L.stageY + " podium " + L.podium[0] + "," + L.podium[1] + "," + L.podium[2]);
        System.out.println("venue blocks: " + v.countBlocks(L.domeX - 62, 50, L.domeZ - 62, L.domeX + 62, 200, L.domeZ + 62));
        System.out.println("plaza+gate blocks: " + v.countBlocks(-60, 66, 30, 60, 140, 125));
        String spawnFloor = v.get(L.spawnX, L.spawnY - 1, L.spawnZ);
        System.out.println("spawn floor=" + spawnFloor + " feet=" + v.get(L.spawnX, L.spawnY, L.spawnZ) + " head=" + v.get(L.spawnX, L.spawnY + 1, L.spawnZ));
        for (int i = 1; i < args.length; i++) {
            String[] p = args[i].split(":");
            String name = p[0];
            int x1 = Integer.parseInt(p[1]), y1 = Integer.parseInt(p[2]), z1 = Integer.parseInt(p[3]);
            int x2 = Integer.parseInt(p[4]), y2 = Integer.parseInt(p[5]), z2 = Integer.parseInt(p[6]);
            int step = p.length > 7 ? Integer.parseInt(p[7]) : 1;
            File f = new File(args[0], name + ".mcmdump.gz");
            dump(v, f, x1, y1, z1, x2, y2, z2, step);
        }
        System.out.println("done in " + (System.currentTimeMillis() - t0) + " ms");
    }

    static void dump(Vox v, File f, int x1, int y1, int z1, int x2, int y2, int z2, int step) throws IOException {
        int sx = (x2 - x1) / step + 1, sy = y2 - y1 + 1, sz = (z2 - z1) / step + 1;
        long n = 0;
        try (Writer w = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(f), 1 << 16), "UTF-8"), 1 << 16)) {
            w.write("MCMDUMP 1 " + sx + " " + sy + " " + sz + "\n");
            StringBuilder sb = new StringBuilder();
            for (int x = x1, ix = 0; x <= x2; x += step, ix++)
                for (int z = z1, iz = 0; z <= z2; z += step, iz++)
                    for (int y = y1; y <= y2; y++) {
                        String b = v.get(x, y, z);
                        if (b == null) continue;
                        sb.setLength(0);
                        sb.append(ix).append(' ').append(y - y1).append(' ').append(iz).append(' ').append(b).append('\n');
                        w.write(sb.toString());
                        n++;
                    }
        }
        System.out.println("  " + f.getName() + ": " + n + " blocks (" + sx + "x" + sy + "x" + sz + ")");
    }
}
