package de.yoshlix.bingobackpack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

public class SpawnManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SPAWN_FILE = "bingobackpack_spawns.json";

    private Map<UUID, SpawnPoint> spawns = new HashMap<>();
    private Path dataPath;
    private MinecraftServer server;

    public void setSpawn(ServerPlayer player) {
        spawns.put(player.getUUID(), SpawnPoint.from(player));
        save();
    }

    public boolean teleportToSpawn(ServerPlayer player) {
        SpawnPoint s = spawns.get(player.getUUID());
        if (s == null || server == null)
            return false;

        var levelKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(s.dimension));
        ServerLevel level = server.getLevel(levelKey);
        if (level == null)
            return false;

        player.connection.teleport(s.x, s.y, s.z, s.yaw, s.pitch);
        return true;
    }

    public void init(MinecraftServer server) {
        this.server = server;
        this.dataPath = server.getWorldPath(LevelResource.ROOT).resolve(SPAWN_FILE);
        load();
    }

    private void save() {
        if (dataPath == null)
            return;
        try {
            String json = GSON.toJson(spawns);
            Files.writeString(dataPath, json);
        } catch (IOException e) {
            BingoBackpack.LOGGER.error("Failed to save teams", e);
        }
    }

    private void load() {
        if (dataPath == null || !Files.exists(dataPath))
            return;
        try {
            String json = Files.readString(dataPath);
            Map<UUID, SpawnPoint> loadedSpawns = GSON.fromJson(json,
                    new TypeToken<Map<UUID, SpawnPoint>>() {
                    }.getType());
            this.spawns = loadedSpawns;
        } catch (IOException e) {
            BingoBackpack.LOGGER.error("Failed to load spawns", e);
        }
    }

    private static SpawnManager instance;

    public static SpawnManager getInstance() {
        if (instance == null) {
            instance = new SpawnManager();
        }
        return instance;
    }

    private SpawnManager() {
    }

    public static class SpawnPoint {
        public String dimension;
        public double x, y, z;
        public float yaw, pitch;

        public static SpawnPoint from(ServerPlayer p) {
            SpawnPoint s = new SpawnPoint();
            s.dimension = p.level().dimension().location().toString();
            s.x = p.getX();
            s.y = p.getY();
            s.z = p.getZ();
            s.yaw = p.getYRot();
            s.pitch = p.getXRot();
            return s;
        }
    }
}
