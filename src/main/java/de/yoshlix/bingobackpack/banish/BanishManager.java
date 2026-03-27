package de.yoshlix.bingobackpack.banish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.yoshlix.bingobackpack.BingoBackpack;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class BanishManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String BANISH_FILE = "bingobackpack_banished.json";

    private Map<UUID, BanishData> banished = new HashMap<>();
    private Path dataPath;
    private MinecraftServer server;
    
    private final List<BanishTask> tasks = new ArrayList<>();

    private static BanishManager instance;

    public static BanishManager getInstance() {
        if (instance == null) {
            instance = new BanishManager();
        }
        return instance;
    }

    private BanishManager() {
        tasks.add(new TaskParkour());
        tasks.add(new TaskArena());
        tasks.add(new TaskMaze());
        tasks.add(new TaskEscapeRoom());
        tasks.add(new TaskMining());
        tasks.add(new TaskEpsteinIsland());
    }

    public void init(MinecraftServer server) {
        this.server = server;
        this.dataPath = server.getWorldPath(LevelResource.ROOT).resolve(BANISH_FILE);
        load();
        registerEvents();
    }
    
    private void registerEvents() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (isBanished((ServerPlayer) player)) {
                BanishData data = banished.get(player.getUUID());
                BanishTask task = tasks.get(data.taskIndex);
                BlockPos origin = getOriginForPlayer(player.getUUID());
                
                if (task.isWinCondition((ServerLevel) world, hitResult.getBlockPos(), origin)) {
                    unbanish((ServerPlayer) player);
                    return InteractionResult.SUCCESS; // intercept the physical click
                }
            }
            return InteractionResult.PASS;
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (isBanished(newPlayer)) {
                BanishData data = banished.get(newPlayer.getUUID());
                ServerLevel endLevel = server.getLevel(Level.END);
                if (endLevel != null) {
                    newPlayer.teleportTo(endLevel, data.taskSpawnX, data.taskSpawnY, data.taskSpawnZ, java.util.Set.of(), newPlayer.getYRot(), newPlayer.getXRot(), true);
                }
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            
            ServerLevel endLevel = server.getLevel(Level.END);
            if (endLevel == null) return;
            
            for (UUID uuid : banished.keySet()) {
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null && isBanished(player)) {
                    BanishData data = banished.get(uuid);
                    BlockPos origin = getOriginForPlayer(uuid);
                    tasks.get(data.taskIndex).tick(player, origin);
                }
            }
        });
    }

    public boolean isBanished(ServerPlayer player) {
        return banished.containsKey(player.getUUID());
    }

    public void banish(ServerPlayer player) {
        if (isBanished(player)) return;

        int taskIdx = new Random().nextInt(tasks.size());
        BanishData data = BanishData.from(player, taskIdx);
        
        BlockPos origin = getOriginForPlayer(player.getUUID());
        ServerLevel endLevel = server.getLevel(Level.END);
        
        if (endLevel == null) {
            player.sendSystemMessage(Component.literal("§cEnd Dimension is not available."));
            return;
        }
        
        endLevel.getChunk(origin.getX() >> 4, origin.getZ() >> 4);
        
        BanishTask task = tasks.get(taskIdx);
        Vec3 spawnPos = task.generate(endLevel, origin);
        
        data.taskSpawnX = spawnPos.x;
        data.taskSpawnY = spawnPos.y;
        data.taskSpawnZ = spawnPos.z;
        
        banished.put(player.getUUID(), data);
        save();
        
        player.teleportTo(endLevel, spawnPos.x, spawnPos.y, spawnPos.z, java.util.Set.of(), 0, 0, true);
        player.sendSystemMessage(Component.literal("§5§lDIR WURDE BANISH AUFERLEGT!"));
        player.sendSystemMessage(Component.literal("§eErfülle die Aufgabe, um in die reale Welt zurückzukehren. Viel Erfolg..."));
    }

    public BanishData getBanishData(ServerPlayer player) {
        return banished.get(player.getUUID());
    }

    public void unbanish(ServerPlayer player) {
        BanishData data = banished.remove(player.getUUID());
        save();
        
        if (data != null) {
            net.minecraft.resources.Identifier originalDimensionLocation = net.minecraft.resources.Identifier.parse(data.originalDimension);
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, originalDimensionLocation);
            ServerLevel level = server.getLevel(dimKey);
            
            if (level != null) {
                player.teleportTo(level, data.originalX, data.originalY, data.originalZ, java.util.Set.of(), data.originalYaw, data.originalPitch, true);
                player.sendSystemMessage(Component.literal("§aDu hast die Aufgabe bestanden und bist entkommen!"));
            } else {
                player.sendSystemMessage(Component.literal("§cFehler beim Zurückkehren, Dimension nicht gefunden!"));
            }
        }
    }
    
    private BlockPos getOriginForPlayer(UUID uuid) {
        int idx = Math.abs(uuid.hashCode() % 100000);
        return new BlockPos(1000000 + (idx * 2000), 200, 0); // 2000 blocks spaced out
    }

    private void save() {
        if (dataPath == null) return;
        try {
            String json = GSON.toJson(banished);
            Files.writeString(dataPath, json);
        } catch (IOException e) {
            BingoBackpack.LOGGER.error("Failed to save banished list", e);
        }
    }

    private void load() {
        if (dataPath == null || !Files.exists(dataPath)) return;
        try {
            String json = Files.readString(dataPath);
            Map<UUID, BanishData> loaded = GSON.fromJson(json, new TypeToken<Map<UUID, BanishData>>(){}.getType());
            if (loaded != null) {
                this.banished = loaded;
            }
        } catch (IOException e) {
            BingoBackpack.LOGGER.error("Failed to load banished list", e);
        }
    }
}
