package de.yoshlix.bingobackpack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TeamManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String TEAMS_FILE = "bingobackpack_teams.json";

    // Map of team name to list of player UUIDs
    private final Map<String, Set<UUID>> teams = new HashMap<>();
    // Map of player UUID to team name (for quick lookup)
    private final Map<UUID, String> playerTeams = new HashMap<>();

    private Path dataPath;

    public void init(MinecraftServer server) {
        this.dataPath = server.getWorldPath(LevelResource.ROOT).resolve(TEAMS_FILE);
        load();
    }

    public boolean createTeam(String teamName) {
        if (teams.containsKey(teamName)) {
            return false;
        }
        teams.put(teamName, new HashSet<>());
        save();
        return true;
    }

    public boolean deleteTeam(String teamName) {
        if (!teams.containsKey(teamName)) {
            return false;
        }
        Set<UUID> members = teams.remove(teamName);
        for (UUID uuid : members) {
            playerTeams.remove(uuid);
        }
        // Also clear the backpack for this team
        BackpackManager.getInstance().clearBackpack(teamName);
        save();
        return true;
    }

    public boolean addPlayerToTeam(String teamName, UUID playerUUID) {
        if (!teams.containsKey(teamName)) {
            return false;
        }
        // Remove from old team if exists
        String oldTeam = playerTeams.get(playerUUID);
        if (oldTeam != null) {
            teams.get(oldTeam).remove(playerUUID);
        }
        teams.get(teamName).add(playerUUID);
        playerTeams.put(playerUUID, teamName);
        save();
        return true;
    }

    public boolean removePlayerFromTeam(String teamName, UUID playerUUID) {
        if (!teams.containsKey(teamName)) {
            return false;
        }
        if (!teams.get(teamName).contains(playerUUID)) {
            return false;
        }
        teams.get(teamName).remove(playerUUID);
        playerTeams.remove(playerUUID);
        save();
        return true;
    }

    public String getPlayerTeam(UUID playerUUID) {
        return playerTeams.get(playerUUID);
    }

    public Set<String> getAllTeams() {
        return Collections.unmodifiableSet(teams.keySet());
    }

    public Set<UUID> getTeamMembers(String teamName) {
        Set<UUID> members = teams.get(teamName);
        return members != null ? Collections.unmodifiableSet(members) : Collections.emptySet();
    }

    public boolean teamExists(String teamName) {
        return teams.containsKey(teamName);
    }

    private void save() {
        if (dataPath == null) return;
        try {
            // Convert UUID sets to String sets for JSON serialization
            Map<String, Set<String>> serializableTeams = new HashMap<>();
            for (Map.Entry<String, Set<UUID>> entry : teams.entrySet()) {
                Set<String> uuidStrings = new HashSet<>();
                for (UUID uuid : entry.getValue()) {
                    uuidStrings.add(uuid.toString());
                }
                serializableTeams.put(entry.getKey(), uuidStrings);
            }
            String json = GSON.toJson(serializableTeams);
            Files.writeString(dataPath, json);
        } catch (IOException e) {
            BingoBackpack.LOGGER.error("Failed to save teams", e);
        }
    }

    private void load() {
        if (dataPath == null || !Files.exists(dataPath)) return;
        try {
            String json = Files.readString(dataPath);
            Map<String, Set<String>> loadedTeams = GSON.fromJson(json,
                    new TypeToken<Map<String, Set<String>>>() {}.getType());
            if (loadedTeams != null) {
                teams.clear();
                playerTeams.clear();
                for (Map.Entry<String, Set<String>> entry : loadedTeams.entrySet()) {
                    Set<UUID> uuids = new HashSet<>();
                    for (String uuidStr : entry.getValue()) {
                        UUID uuid = UUID.fromString(uuidStr);
                        uuids.add(uuid);
                        playerTeams.put(uuid, entry.getKey());
                    }
                    teams.put(entry.getKey(), uuids);
                }
            }
        } catch (IOException e) {
            BingoBackpack.LOGGER.error("Failed to load teams", e);
        }
    }

    // Singleton instance
    private static TeamManager instance;

    public static TeamManager getInstance() {
        if (instance == null) {
            instance = new TeamManager();
        }
        return instance;
    }

    private TeamManager() {}
}
