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

    private int maxTeamSize = 2;
    private final Map<UUID, String> playerTeams = new HashMap<>();
    private final Map<UUID, Integer> playerPoints = new HashMap<>();

    public int getMaxTeamSize() {
        return maxTeamSize;
    }

    public void setMaxTeamSize(int size) {
        maxTeamSize = Math.max(1, Math.min(5, size));
        for (String tName : TEAM_NAMES) {
            List<UUID> members = new ArrayList<>();
            for (Map.Entry<UUID, String> e : playerTeams.entrySet()) {
                if (tName.equals(e.getValue())) members.add(e.getKey());
            }
            while (members.size() > maxTeamSize) {
                UUID removed = members.remove(members.size() - 1);
                playerTeams.remove(removed);
                Player rp = Bukkit.getPlayer(removed);
                if (rp != null) {
                    rp.sendMessage(ChatColor.YELLOW + "Moved to Solo because team size was reduced to " + maxTeamSize + ".");
                }
            }
        }
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

    public void setTeam(UUID id, String team) {
        if (team == null) playerTeams.remove(id);
        else playerTeams.put(id, team);
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

    public void randomizeOnlinePlayers() {
        playerTeams.clear();
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(online);
        for (int i = 0; i < online.size(); i++) {
            int idx = (i / maxTeamSize) % TEAM_NAMES.length;
            playerTeams.put(online.get(i).getUniqueId(), TEAM_NAMES[idx]);
        }
    }

    public void ensurePlayer(UUID id) {
        playerPoints.putIfAbsent(id, 0);
    }

    public void resetScores() {
        playerPoints.clear();
    }

    public int getPlayerPoints(UUID id) {
        return playerPoints.getOrDefault(id, 0);
    }

    /** Awards individual points (automatically rolls into that player's team total!). */
    public void addIndividualPoints(UUID id, int pts, String reason) {
        if (pts <= 0) return;
        playerPoints.merge(id, pts, Integer::sum);
        Player p = Bukkit.getPlayer(id);
        if (p != null) {
            p.sendMessage(ChatColor.GOLD + "+" + pts + " pts " + ChatColor.GRAY + "(" + reason + ")");
        }
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
                addIndividualPoints(living.get(i), share, reason);
            }
        }
    }

    /** Holistic Team Standings: maps Display Name -> Total Team Points (sum of all members). */
    public List<Map.Entry<String, Integer>> getStandings() {
        Map<String, Integer> map = new HashMap<>();
        Set<UUID> allIds = new HashSet<>(playerPoints.keySet());
        for (Player p : Bukkit.getOnlinePlayers()) allIds.add(p.getUniqueId());

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
