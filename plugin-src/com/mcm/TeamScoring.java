package com.mcm;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles authentic Minecraft Monday Team & Holistic Split Scoring:
 * - Supports team sizes 1 (1v1/Solo Teams), 2 (Duos), 3 (Trios), 4 (Squads)
 * - Tracks both Individual Points and Holistic Team Totals
 * - Implements MCM Split Scoring: team survival & win bonuses split evenly across living teammates,
 *   or go 100% to a solo surviving clutch teammate!
 */
public class TeamScoring {

    public static final String[] TEAM_NAMES = {
            "Red", "Orange", "Yellow", "Lime", "Green",
            "Cyan", "Aqua", "Blue", "Purple", "Pink"
    };
    public static final Material[] TEAM_WOOLS = {
            Material.RED_WOOL, Material.ORANGE_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL, Material.GREEN_WOOL,
            Material.CYAN_WOOL, Material.LIGHT_BLUE_WOOL, Material.BLUE_WOOL, Material.PURPLE_WOOL, Material.PINK_WOOL
    };

    private int maxTeamSize = 4;
    /** How many of the {@link #TEAM_NAMES} are in use (team pads, menu slots, auto-balance). */
    private int activeTeams = 8;
    /** Registered team members (online or offline) by player; persisted to teams.yml. */
    private final Map<UUID, String> playerTeams = new HashMap<>();
    private final Map<UUID, Integer> playerPoints = new HashMap<>();
    /** Fraction of a coin owed to each player (multiplied awards such as 3 x 1.5 = 4.5). */
    private final Map<UUID, Double> coinCarry = new HashMap<>();
    /** Last known names of registered players, so offline members can be listed. */
    private final Map<UUID, String> knownNames = new HashMap<>();
    /** Players marked as spectators/staff with /mcm spectate (persisted). */
    private final Set<UUID> spectators = new LinkedHashSet<>();
    /** Registered players per team at the start of the current game (null = use the live count). */
    private final Map<String, Integer> sizeSnapshot = new HashMap<>();
    private boolean shortTeamBonus = true;
    /** Players that never count as a side (staff, unteamed spectators): hidden from standings. */
    private java.util.function.Predicate<UUID> excluded = id -> false;
    private java.io.File file;

    // ------------------------------------------------------------------ configuration / persistence

    /** Applies config (teams.size, teams.count, coins.short-team-bonus); does not move any registered player. */
    public void configure(int size, int count, boolean bonus) {
        maxTeamSize = Math.max(1, Math.min(5, size));
        activeTeams = Math.max(1, Math.min(TEAM_NAMES.length, count));
        shortTeamBonus = bonus;
    }

    public int getActiveTeams() { return activeTeams; }

    public void setActiveTeams(int n) { activeTeams = Math.max(1, Math.min(TEAM_NAMES.length, n)); }

    public boolean isTeamActive(String team) {
        for (int i = 0; i < activeTeams; i++) if (TEAM_NAMES[i].equals(team)) return true;
        return false;
    }

    public int teamIndex(String team) {
        for (int i = 0; i < TEAM_NAMES.length; i++) if (TEAM_NAMES[i].equals(team)) return i;
        return -1;
    }

    public void setExcluded(java.util.function.Predicate<UUID> excluded) {
        this.excluded = excluded == null ? id -> false : excluded;
    }

    public boolean isExcluded(UUID id) { return excluded.test(id); }

    /** Loads plugin-data/teams.yml (teams, last known names, spectators). */
    public void load(java.io.File f) {
        file = f;
        playerTeams.clear();
        knownNames.clear();
        spectators.clear();
        if (f == null || !f.exists()) return;
        org.bukkit.configuration.file.YamlConfiguration y = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
        org.bukkit.configuration.ConfigurationSection t = y.getConfigurationSection("teams");
        if (t != null) {
            for (String k : t.getKeys(false)) {
                try {
                    String team = t.getString(k);
                    if (team != null && teamIndex(team) >= 0) playerTeams.put(UUID.fromString(k), team);
                } catch (IllegalArgumentException ignored) { }
            }
        }
        org.bukkit.configuration.ConfigurationSection n = y.getConfigurationSection("names");
        if (n != null) {
            for (String k : n.getKeys(false)) {
                try { knownNames.put(UUID.fromString(k), n.getString(k, "")); } catch (IllegalArgumentException ignored) { }
            }
        }
        for (String s : y.getStringList("spectators")) {
            try { spectators.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) { }
        }
    }

    public void save() {
        if (file == null) return;
        org.bukkit.configuration.file.YamlConfiguration y = new org.bukkit.configuration.file.YamlConfiguration();
        for (Map.Entry<UUID, String> e : playerTeams.entrySet()) y.set("teams." + e.getKey(), e.getValue());
        for (Map.Entry<UUID, String> e : knownNames.entrySet()) {
            if (playerTeams.containsKey(e.getKey()) || spectators.contains(e.getKey())) y.set("names." + e.getKey(), e.getValue());
        }
        List<String> sp = new ArrayList<>();
        for (UUID u : spectators) sp.add(u.toString());
        y.set("spectators", sp);
        try {
            file.getParentFile().mkdirs();
            y.save(file);
        } catch (java.io.IOException ex) {
            Bukkit.getLogger().warning("[MCM] could not save " + file + ": " + ex);
        }
    }

    public void rememberName(UUID id, String name) {
        if (name == null || name.isBlank()) return;
        if (!name.equals(knownNames.put(id, name)) && (playerTeams.containsKey(id) || spectators.contains(id))) save();
    }

    public boolean isMarkedSpectator(UUID id) { return spectators.contains(id); }

    public void setMarkedSpectator(UUID id, boolean on) {
        if (on ? spectators.add(id) : spectators.remove(id)) save();
    }

    public int getMaxTeamSize() {
        return maxTeamSize;
    }

    public void setMaxTeamSize(int size) {
        maxTeamSize = Math.max(1, Math.min(5, size));
        for (String tName : TEAM_NAMES) {
            List<UUID> members = registeredMembers(tName);
            // offline members go first, so online players keep their team as long as possible
            members.sort(Comparator.comparing((UUID u) -> Bukkit.getPlayer(u) != null));
            while (members.size() > maxTeamSize) {
                UUID removed = members.remove(0);
                playerTeams.remove(removed);
                Player rp = Bukkit.getPlayer(removed);
                if (rp != null) {
                    rp.sendMessage(ChatColor.YELLOW + "Moved to Solo because team size was reduced to " + maxTeamSize + ".");
                }
            }
        }
        save();
    }

    // ------------------------------------------------------------------ registered membership

    /** Registered members of a team, online or not. */
    public List<UUID> registeredMembers(String team) {
        List<UUID> out = new ArrayList<>();
        for (Map.Entry<UUID, String> e : playerTeams.entrySet()) if (team.equals(e.getValue())) out.add(e.getKey());
        return out;
    }

    public int registeredTotal() { return playerTeams.size(); }

    /** Copy of every registered player -> team (online or not). */
    public Map<UUID, String> registeredTeams() { return new HashMap<>(playerTeams); }

    public int registeredCount(String team) {
        int n = 0;
        for (String t : playerTeams.values()) if (team.equals(t)) n++;
        return n;
    }

    /** True if {@code id} cannot join {@code team}: it already holds {@code teams.size} other registered members. */
    public boolean isFullFor(UUID id, String team) {
        int n = 0;
        for (Map.Entry<UUID, String> e : playerTeams.entrySet()) if (team.equals(e.getValue()) && !e.getKey().equals(id)) n++;
        return n >= maxTeamSize;
    }

    /** Joins a team unless it is full (registered members, online or not). Returns false if refused. */
    public boolean trySetTeam(UUID id, String team) {
        if (team != null && isFullFor(id, team)) return false;
        setTeam(id, team);
        return true;
    }

    /** Online player, registered player or cached offline player with that name; null if unknown. */
    public UUID findByName(String name) {
        if (name == null) return null;
        Player p = Bukkit.getPlayerExact(name);
        if (p != null) return p.getUniqueId();
        for (Map.Entry<UUID, String> e : knownNames.entrySet()) if (e.getValue().equalsIgnoreCase(name)) return e.getKey();
        OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(name);
        return op == null ? null : op.getUniqueId();
    }

    /** Name of a registered or known player ("Player" if never seen). */
    public String nameOf(UUID id) {
        Player p = Bukkit.getServer() == null ? null : Bukkit.getPlayer(id);
        if (p != null) return p.getName();
        String k = knownNames.get(id);
        if (k != null && !k.isBlank()) return k;
        return playerName(id);
    }

    /**
     * Auto-balance: re-registers the given online players across the first {@link #getActiveTeams()} teams as evenly as
     * possible (teams of size N and N-1, never above {@code teams.size}). Players registered but offline stay where they
     * are and count towards their team's size. Players who do not fit stay unteamed; returns how many.
     */
    public int autoBalance(Collection<? extends Player> eligible) {
        List<Player> list = new ArrayList<>(eligible);
        Set<UUID> ids = new HashSet<>();
        for (Player p : list) ids.add(p.getUniqueId());
        playerTeams.keySet().removeAll(ids);
        Collections.shuffle(list);
        int[] count = new int[activeTeams];
        for (String t : playerTeams.values()) {
            int i = teamIndex(t);
            if (i >= 0 && i < activeTeams) count[i]++;
        }
        int left = 0;
        for (Player p : list) {
            int best = -1;
            for (int i = 0; i < activeTeams; i++) {
                if (count[i] < maxTeamSize && (best < 0 || count[i] < count[best])) best = i;
            }
            if (best < 0) { left++; continue; }
            count[best]++;
            playerTeams.put(p.getUniqueId(), TEAM_NAMES[best]);
            rememberNameQuiet(p.getUniqueId(), p.getName());
        }
        save();
        return left;
    }

    private void rememberNameQuiet(UUID id, String name) { if (name != null) knownNames.put(id, name); }

    // ------------------------------------------------------------------ short-team coin bonus

    /** Records how many players each team has registered; call when a game or event starts. */
    public void snapshotTeamSizes() {
        sizeSnapshot.clear();
        for (String t : playerTeams.values()) sizeSnapshot.merge(t, 1, Integer::sum);
    }

    public void clearTeamSizeSnapshot() { sizeSnapshot.clear(); }

    public boolean isShortTeamBonus() { return shortTeamBonus; }

    /** Registered size of a team for the current game (snapshot taken at its start, else live). */
    public int teamSizeNow(String team) {
        if (team == null) return 0;
        Integer s = sizeSnapshot.get(team);
        return s != null ? s : registeredCount(team);
    }

    /** Registered players of a side: team size for "TEAM:x", 1 for "SOLO:x". */
    public int registeredSizeOfSide(String sideKey) {
        if (sideKey == null) return 0;
        if (sideKey.startsWith("TEAM:")) return teamSizeNow(sideKey.substring(5));
        return 1;
    }

    /**
     * Coin factor of a player: {@code teams.size / registered team size} (a trio on a team size of 4 = x4/3, a duo x2),
     * 1.0 for a full team, a solo player or when {@code coins.short-team-bonus} is off. Never below 1.
     */
    public double teamCoinFactor(UUID id) {
        if (!shortTeamBonus || id == null) return 1.0;
        String t = playerTeams.get(id);
        if (t == null) return 1.0;
        int n = teamSizeNow(t);
        if (n <= 0) return 1.0;
        return Math.max(1.0, maxTeamSize / (double) n);
    }

    /** "x1.33", "x2", "x1.5". */
    public static String formatFactor(double f) {
        double r = Math.round(f * 100.0) / 100.0;
        String s = String.format(Locale.ROOT, "%.2f", r);
        if (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
        return "x" + s;
    }

    public String getTeamSizeLabel() {
        return switch (maxTeamSize) {
            case 1 -> "1 (1v1 / Solo Teams)";
            case 2 -> "2 (Duos)";
            case 3 -> "3 (Trios)";
            case 4 -> "4 (Squads)";
            default -> "5 (5-Player Teams)";
        };
    }

    public String getTeam(UUID id) {
        return playerTeams.get(id);
    }

    /** Registers (or, with null, unregisters) a player; saved to teams.yml. Does not check the team size, see {@link #trySetTeam}. */
    public void setTeam(UUID id, String team) {
        if (team == null) playerTeams.remove(id);
        else {
            playerTeams.put(id, team);
            Player p = Bukkit.getServer() == null ? null : Bukkit.getPlayer(id);
            if (p != null) knownNames.put(id, p.getName());
        }
        save();
    }

    /** Forgets every registered team member (new event). */
    public void clearTeams() {
        playerTeams.clear();
        save();
    }

    public boolean areTeammates(UUID a, UUID b) {
        if (a.equals(b)) return true;
        String ta = playerTeams.get(a);
        String tb = playerTeams.get(b);
        return ta != null && ta.equals(tb);
    }

    /** Returns side key ("TEAM:Red", "TEAM:Blue", or player's UUID string if Solo/unassigned). */
    public String getSideKey(UUID id) {
        String t = playerTeams.get(id);
        return t != null ? "TEAM:" + t : "SOLO:" + id;
    }

    public boolean hasActiveTeams() {
        return !playerTeams.isEmpty();
    }

    public List<String> getOnlineMembers(String team) {
        List<String> list = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (team.equals(playerTeams.get(p.getUniqueId()))) list.add(p.getName());
        }
        return list;
    }

    public int getTeamPoints(String team) {
        int sum = 0;
        for (Map.Entry<UUID, String> e : playerTeams.entrySet()) {
            if (team.equals(e.getValue())) {
                sum += playerPoints.getOrDefault(e.getKey(), 0);
            }
        }
        return sum;
    }

    public void ensurePlayer(UUID id) {
        playerPoints.putIfAbsent(id, 0);
    }

    public void resetScores() {
        playerPoints.clear();
        coinCarry.clear();
    }

    public int getPlayerPoints(UUID id) {
        return playerPoints.getOrDefault(id, 0);
    }

    /**
     * Awards MCC coins that are already multiplied (may be fractional or negative). Fractions are carried per
     * player so nothing is lost to rounding (two awards of 7.5 credit 15); a player's total never drops below 0.
     * Prints the MCC chat line "[+N coin] reason" ("[-N coin]" for negatives) and plays the ding (see CoinFeedback).
     * Returns the whole coins credited now.
     */
    public int addCoins(UUID id, double coins, String reason) {
        return addCoins(id, coins, reason, true);
    }

    /**
     * Same as {@link #addCoins(UUID, double, String)}; {@code teamBonus} false skips the short-team factor, for awards
     * that are already split per member (a team pool paid as pool / online members, side points).
     */
    public int addCoins(UUID id, double coins, String reason, boolean teamBonus) {
        if (coins == 0 || Double.isNaN(coins) || Double.isInfinite(coins)) return 0;
        if (teamBonus && coins > 0) coins *= teamCoinFactor(id);
        double exact = coinCarry.getOrDefault(id, 0.0) + coins;
        int whole = com.mcm.api.McmCoins.whole(exact);
        coinCarry.put(id, exact - whole);
        int before = playerPoints.getOrDefault(id, 0);
        int after = Math.max(0, before + whole);
        playerPoints.put(id, after);
        Player p = Bukkit.getServer() == null ? null : Bukkit.getPlayer(id);
        if (p != null) CoinFeedback.award(p, coins, reason);
        return after - before;
    }

    /** Awards individual points (automatically rolls into that player's team total!). */
    public void addIndividualPoints(UUID id, int pts, String reason) {
        addIndividualPoints(id, pts, reason, true);
    }

    public void addIndividualPoints(UUID id, int pts, String reason, boolean teamBonus) {
        if (pts <= 0) return;
        if (teamBonus && teamCoinFactor(id) != 1.0) { addCoins(id, pts, reason, true); return; }
        playerPoints.merge(id, pts, Integer::sum);
        Player p = Bukkit.getServer() == null ? null : Bukkit.getPlayer(id);
        if (p != null) CoinFeedback.award(p, pts, reason);
    }

    /**
     * Authentic Minecraft Monday Split Scoring:
     * Awards a team-level bonus (survival tick or round win) split evenly among living teammates.
     * If only 1 teammate is alive (clutch!), they receive the full bonus.
     */
    public void awardSplitTeamBonus(Set<UUID> alivePlayers, int teamPts, String reason) {
        Map<String, List<UUID>> aliveBySide = new LinkedHashMap<>();
        for (UUID u : alivePlayers) {
            aliveBySide.computeIfAbsent(getSideKey(u), k -> new ArrayList<>()).add(u);
        }
        for (Map.Entry<String, List<UUID>> entry : aliveBySide.entrySet()) {
            List<UUID> living = entry.getValue();
            int perPlayer = Math.max(1, teamPts / living.size());
            int remainder = Math.max(0, teamPts - (perPlayer * living.size()));
            for (int i = 0; i < living.size(); i++) {
                int share = perPlayer + (i == 0 ? remainder : 0);
                addIndividualPoints(living.get(i), share, reason, false);   // already split per living teammate
            }
        }
    }

    /** Holistic Team Standings: maps Display Name -> Total Team Points (sum of all members). */
    public List<Map.Entry<String, Integer>> getStandings() {
        Map<String, Integer> map = new HashMap<>();
        Set<UUID> allIds = new HashSet<>(playerPoints.keySet());
        for (Player p : Bukkit.getOnlinePlayers()) allIds.add(p.getUniqueId());
        allIds.removeIf(excluded);

        if (hasActiveTeams()) {
            for (UUID id : allIds) {
                int pts = playerPoints.getOrDefault(id, 0);
                String t = playerTeams.get(id);
                String label = (t != null) ? (teamColor(t) + t + " Team") : (ChatColor.WHITE + playerName(id));
                map.merge(label, pts, Integer::sum);
            }
        } else {
            for (UUID id : allIds) {
                map.put(ChatColor.WHITE + playerName(id), playerPoints.getOrDefault(id, 0));
            }
        }
        return map.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toList());
    }

    public List<Map.Entry<UUID, Integer>> getIndividualStandings() {
        return playerPoints.entrySet().stream()
                .filter(e -> !excluded.test(e.getKey()))
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toList());
    }

    public boolean isOneSideRemaining(Set<UUID> alivePlayers) {
        if (alivePlayers.size() <= 1) return true;
        Set<String> sides = new HashSet<>();
        for (UUID u : alivePlayers) sides.add(getSideKey(u));
        return sides.size() <= 1;
    }

    public void applyTabNames() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            String t = playerTeams.get(p.getUniqueId());
            p.setPlayerListName(t == null ? p.getName() : teamColor(t) + "[" + t + "] " + ChatColor.WHITE + p.getName());
        }
    }

    public String playerName(UUID id) {
        String k = knownNames.get(id);
        if (k != null && !k.isBlank()) return k;
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : "Player";
    }

    public ChatColor teamColor(String t) {
        if (t == null) return ChatColor.WHITE;
        return switch (t) {
            case "Red" -> ChatColor.RED;
            case "Orange" -> ChatColor.GOLD;
            case "Yellow" -> ChatColor.YELLOW;
            case "Lime" -> ChatColor.GREEN;
            case "Green" -> ChatColor.DARK_GREEN;
            case "Cyan" -> ChatColor.DARK_AQUA;
            case "Aqua" -> ChatColor.AQUA;
            case "Blue" -> ChatColor.BLUE;
            case "Purple" -> ChatColor.DARK_PURPLE;
            case "Pink" -> ChatColor.LIGHT_PURPLE;
            default -> ChatColor.WHITE;
        };
    }

    public Material teamWool(String t) {
        if (t == null) return Material.WHITE_WOOL;
        for (int i = 0; i < TEAM_NAMES.length; i++) {
            if (TEAM_NAMES[i].equals(t)) return TEAM_WOOLS[i];
        }
        return Material.WHITE_WOOL;
    }
}
