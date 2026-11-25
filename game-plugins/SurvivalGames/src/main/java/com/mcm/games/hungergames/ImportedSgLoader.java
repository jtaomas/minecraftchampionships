package com.mcm.games.hungergames;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Discovers and safely parses imported Survival Games worlds from plugins/McmSurvivalGames/worlds/<id>/map.yml.
 */
public final class ImportedSgLoader {
    private static final Pattern VALID_ID = Pattern.compile("^[a-z0-9_]{1,32}$");
    private static final Pattern VALID_REPO_WORLD = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    private ImportedSgLoader() {}

    public static Map<String, ImportedSgWorld> loadAll(File worldsDir, Logger logger) {
        File serverRoot = inferServerRoot(worldsDir);
        return loadAll(worldsDir, serverRoot, logger);
    }

    public static Map<String, ImportedSgWorld> loadAll(File worldsDir, File serverRoot, Logger logger) {
        if (worldsDir == null || !worldsDir.isDirectory()) {
            return Collections.emptyMap();
        }
        File[] children = worldsDir.listFiles(File::isDirectory);
        if (children == null || children.length == 0) {
            return Collections.emptyMap();
        }

        List<File> dirs = new ArrayList<>(List.of(children));
        dirs.sort(Comparator.comparing(File::getName));

        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, ImportedSgWorld> loaded = new LinkedHashMap<>();
        Path rootPath = serverRoot != null ? serverRoot.toPath().toAbsolutePath().normalize() : null;

        for (File dir : dirs) {
            File mapYml = new File(dir, "map.yml");
            if (!mapYml.isFile()) {
                continue;
            }
            try {
                ImportedSgWorld world = parseOne(dir.toPath().toAbsolutePath().normalize(), mapYml.toPath(), rootPath, yaml, logger);
                if (world != null) {
                    if (loaded.containsKey(world.id())) {
                        logWarn(logger, "Duplicate imported SG map id '" + world.id() + "' in " + mapYml + "; skipping.");
                    } else {
                        loaded.put(world.id(), world);
                    }
                }
            } catch (Exception ex) {
                logWarn(logger, "Failed to load imported SG map from " + mapYml + ": " + ex.getMessage());
            }
        }
        return Collections.unmodifiableMap(loaded);
    }

    private static ImportedSgWorld parseOne(
            Path mapDir,
            Path mapYml,
            Path serverRoot,
            Yaml yaml,
            Logger logger
    ) throws IOException {
        Object raw;
        try (InputStream in = Files.newInputStream(mapYml)) {
            raw = yaml.load(in);
        }
        if (!(raw instanceof Map<?, ?> map)) {
            logWarn(logger, "map.yml is not a YAML mapping: " + mapYml);
            return null;
        }

        String id = asString(map.get("id"));
        if (id == null || !VALID_ID.matcher(id).matches()) {
            logWarn(logger, "Invalid or missing map id in " + mapYml + " (must match [a-z0-9_]{1,32})");
            return null;
        }

        String name = asString(map.get("name"));
        if (name == null || name.isBlank()) {
            name = id;
        }

        Object centerObj = map.get("center");
        if (!(centerObj instanceof List<?> centerList) || centerList.size() != 3) {
            logWarn(logger, "Invalid or missing 'center: [x, y, z]' in " + mapYml);
            return null;
        }
        Integer cx = asInt(centerList.get(0));
        Integer cy = asInt(centerList.get(1));
        Integer cz = asInt(centerList.get(2));
        if (cx == null || cy == null || cz == null) {
            logWarn(logger, "Non-numeric center coordinates in " + mapYml);
            return null;
        }

        Integer radiusObj = asInt(map.get("radius"));
        int radius = radiusObj != null ? Math.max(32, Math.min(1000, radiusObj)) : 220;

        boolean boat = Boolean.TRUE.equals(map.get("boat"));
        Integer minYObj = asInt(map.get("min_y"));
        Integer maxYObj = asInt(map.get("max_y"));
        int minY = minYObj != null ? minYObj : 1;
        int maxY = maxYObj != null ? maxYObj : 200;

        Path templateDir = mapDir;
        String repoWorld = asString(map.get("repo_world"));
        if (repoWorld != null && !repoWorld.isBlank()) {
            if (!VALID_REPO_WORLD.matcher(repoWorld).matches() || repoWorld.contains("..")) {
                logWarn(logger, "Unsafe repo_world '" + repoWorld + "' in " + mapYml);
                return null;
            }
            if (serverRoot == null) {
                logWarn(logger, "Cannot resolve repo_world '" + repoWorld + "' without serverRoot for " + mapYml);
                return null;
            }
            Path resolved = serverRoot.resolve(repoWorld).normalize();
            if (!resolved.startsWith(serverRoot)) {
                logWarn(logger, "Path traversal detected in repo_world '" + repoWorld + "' in " + mapYml);
                return null;
            }
            templateDir = resolved;
        }

        if (!Files.isDirectory(templateDir)
                || !Files.isRegularFile(templateDir.resolve("level.dat"))
                || !Files.isDirectory(templateDir.resolve("region"))) {
            logWarn(logger, "Template world missing level.dat or region/ at " + templateDir + " for map '" + id + "'");
            return null;
        }

        return new ImportedSgWorld(id, name, cx, cy, cz, radius, boat, minY, maxY, templateDir);
    }

    private static File inferServerRoot(File worldsDir) {
        if (worldsDir == null) return new File(".");
        Path p = worldsDir.toPath().toAbsolutePath().normalize();
        // Expecting <root>/plugins/McmSurvivalGames/worlds
        Path parent1 = p.getParent();
        Path parent2 = parent1 != null ? parent1.getParent() : null;
        Path parent3 = parent2 != null ? parent2.getParent() : null;
        return parent3 != null ? parent3.toFile() : new File(".");
    }

    private static String asString(Object o) {
        return o instanceof String s ? s.trim() : null;
    }

    private static Integer asInt(Object o) {
        return o instanceof Number n ? n.intValue() : null;
    }

    private static void logWarn(Logger logger, String msg) {
        if (logger != null) {
            logger.warning(msg);
        }
    }
}
