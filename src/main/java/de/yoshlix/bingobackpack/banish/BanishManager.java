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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
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
    private boolean eventsRegistered = false;
    
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
        if (!eventsRegistered) {
            registerEvents();
            eventsRegistered = true;
        }
    }
    
    private void registerEvents() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!isBanished(serverPlayer)) return InteractionResult.PASS;

            BanishData data = banished.get(serverPlayer.getUUID());
            if (data == null) return InteractionResult.PASS;
            
            BanishTask task = tasks.get(data.taskIndex);
            BlockPos origin = new BlockPos(data.originX, data.originY, data.originZ);
            
            if (task.isWinCondition((ServerLevel) world, hitResult.getBlockPos(), origin)) {
                unbanish(serverPlayer);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (isBanished(newPlayer)) {
                BanishData data = banished.get(newPlayer.getUUID());
                if (data == null) return;
                ServerLevel endLevel = server.getLevel(Level.END);
                if (endLevel != null) {
                    // Re-give task equipment on respawn
                    BanishTask task = tasks.get(data.taskIndex);
                    task.onRespawn(newPlayer, new BlockPos(data.originX, data.originY, data.originZ));
                    newPlayer.teleportTo(endLevel, data.taskSpawnX, data.taskSpawnY, data.taskSpawnZ, java.util.Set.of(), newPlayer.getYRot(), newPlayer.getXRot(), true);
                }
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(srv -> {
            if (srv.getTickCount() % 20 != 0) return;
            
            ServerLevel endLevel = srv.getLevel(Level.END);
            if (endLevel == null) return;
            
            // Copy key set to avoid ConcurrentModificationException
            List<UUID> uuids = new ArrayList<>(banished.keySet());
            for (UUID uuid : uuids) {
                if (!banished.containsKey(uuid)) continue; // may have been unbanished during iteration
                ServerPlayer player = srv.getPlayerList().getPlayer(uuid);
                if (player != null) {
                    BanishData data = banished.get(uuid);
                    if (data == null) continue;
                    BlockPos origin = new BlockPos(data.originX, data.originY, data.originZ);
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

        int taskIdx = RANDOM.nextInt(tasks.size());
        BanishData data = BanishData.from(player, taskIdx);
        
        // Save and clear the player's inventory
        savePlayerInventory(player, data);
        player.getInventory().clearContent();
        
        // Generate unique origin position for this banish session
        BlockPos origin = generateUniqueOrigin();
        data.originX = origin.getX();
        data.originY = origin.getY();
        data.originZ = origin.getZ();
        
        ServerLevel endLevel = server.getLevel(Level.END);
        
        if (endLevel == null) {
            // Restore inventory if we can't banish
            restorePlayerInventory(player, data);
            player.sendSystemMessage(Component.literal("§cEnd Dimension is not available."));
            return;
        }
        
        // Force load the chunk
        endLevel.getChunk(origin.getX() >> 4, origin.getZ() >> 4);
        
        BanishTask task = tasks.get(taskIdx);
        Vec3 spawnPos = task.generate(endLevel, origin);
        
        data.taskSpawnX = spawnPos.x;
        data.taskSpawnY = spawnPos.y;
        data.taskSpawnZ = spawnPos.z;
        
        banished.put(player.getUUID(), data);
        save();
        
        player.teleportTo(endLevel, spawnPos.x, spawnPos.y, spawnPos.z, java.util.Set.of(), 0, 0, true);
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§5§l══════════════════════════════"));
        player.sendSystemMessage(Component.literal("§5§l   ☠ DIR WURDE BANISH AUFERLEGT! ☠"));
        player.sendSystemMessage(Component.literal("§5§l══════════════════════════════"));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§e§lAufgabe: §r§f" + task.getTaskDescription()));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Erfülle die Aufgabe, um zurückzukehren."));
        player.sendSystemMessage(Component.literal("§7§o/spawn und Items sind gesperrt."));
        player.sendSystemMessage(Component.literal(""));
    }

    public BanishData getBanishData(ServerPlayer player) {
        return banished.get(player.getUUID());
    }

    public void unbanish(ServerPlayer player) {
        unbanish(player,
                Component.literal("§aDu hast die Aufgabe bestanden und bist entkommen!"),
                Component.literal("§cOriginal-Dimension nicht gefunden. Zum Spawn teleportiert."));
    }

    public boolean clearBanish(ServerPlayer player) {
        if (!isBanished(player)) {
            return false;
        }

        unbanish(player,
                Component.literal("§aDein Banish-Status wurde von einem Admin entfernt."),
                Component.literal("§cBanish-Status entfernt, aber Original-Dimension nicht gefunden. Zum Spawn teleportiert."));
        return true;
    }

    private void unbanish(ServerPlayer player, Component successMessage, Component fallbackMessage) {
        BanishData data = banished.remove(player.getUUID());
        save();
        
        if (data != null) {
            // Clear challenge items, restore original inventory
            player.getInventory().clearContent();
            restorePlayerInventory(player, data);
            
            net.minecraft.resources.Identifier originalDimensionLocation = net.minecraft.resources.Identifier.parse(data.originalDimension);
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, originalDimensionLocation);
            ServerLevel level = server.getLevel(dimKey);
            
            if (level != null) {
                player.teleportTo(level, data.originalX, data.originalY, data.originalZ, java.util.Set.of(), data.originalYaw, data.originalPitch, true);
                player.sendSystemMessage(successMessage);
            } else {
                ServerLevel overworld = server.overworld();
                player.teleportTo(overworld, 0, 100, 0, java.util.Set.of(), 0, 0, true);
                player.sendSystemMessage(fallbackMessage);
            }
        }
    }

    private void savePlayerInventory(ServerPlayer player, BanishData data) {
        try {
            CompoundTag rootTag = new CompoundTag();
            net.minecraft.nbt.ListTag listTag = new net.minecraft.nbt.ListTag();
            net.minecraft.core.HolderLookup.Provider registries = server.registryAccess();

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    CompoundTag itemTag = new CompoundTag();
                    itemTag.putInt("Slot", i);
                    CompoundTag itemData = (CompoundTag) ItemStack.OPTIONAL_CODEC
                            .encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack)
                            .getOrThrow();
                    itemTag.put("Item", itemData);
                    listTag.add(itemTag);
                }
            }

            rootTag.put("Items", listTag);

            Path invFile = dataPath.getParent().resolve("banish_inv_" + player.getUUID() + ".dat");
            net.minecraft.nbt.NbtIo.writeCompressed(rootTag, invFile);
        } catch (Exception e) {
            BingoBackpack.LOGGER.error("Failed to save banished player inventory", e);
        }
    }

    private void restorePlayerInventory(ServerPlayer player, BanishData data) {
        try {
            Path invFile = dataPath.getParent().resolve("banish_inv_" + player.getUUID() + ".dat");
            if (!Files.exists(invFile)) return;

            CompoundTag rootTag = net.minecraft.nbt.NbtIo.readCompressed(invFile, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            net.minecraft.nbt.ListTag listTag = rootTag.getList("Items").orElse(new net.minecraft.nbt.ListTag());
            net.minecraft.core.HolderLookup.Provider registries = server.registryAccess();

            for (int i = 0; i < listTag.size(); i++) {
                listTag.getCompound(i).ifPresent(itemTag -> {
                    int slot = itemTag.getInt("Slot").orElse(-1);
                    if (slot >= 0 && slot < player.getInventory().getContainerSize()) {
                        itemTag.getCompound("Item").ifPresent(itemCompound -> {
                            ItemStack stack = ItemStack.OPTIONAL_CODEC
                                    .parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), itemCompound)
                                    .result().orElse(ItemStack.EMPTY);
                            player.getInventory().setItem(slot, stack);
                        });
                    }
                });
            }

            // Delete the temp file after restoring
            Files.deleteIfExists(invFile);
        } catch (Exception e) {
            BingoBackpack.LOGGER.error("Failed to restore banished player inventory", e);
        }
    }
    private static int banishCounter = 0;
    private static final Random RANDOM = new Random();
    
    private BlockPos generateUniqueOrigin() {
        banishCounter++;
        // Spread on a 2D grid: X from 10000-100000, Z from -50000 to 50000
        // Each arena gets a 500x500 block area
        int x = 10000 + (banishCounter * 500) + RANDOM.nextInt(100);
        int z = RANDOM.nextInt(100000) - 50000;
        return new BlockPos(x, 200, z);
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
