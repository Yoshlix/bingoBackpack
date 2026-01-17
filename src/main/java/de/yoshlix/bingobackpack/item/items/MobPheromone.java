package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * Mob-Pheromone - For 2 minutes, hostile mobs spawn much more frequently around
 * you.
 * Perfect for farming mob drops for Bingo objectives!
 */
public class MobPheromone extends BingoItem {

    // Track active pheromone effects per player
    private static final Map<UUID, Long> activePheromones = new HashMap<>();
    private static final int DURATION_SECONDS = 120; // 2 minutes
    private static final int SPAWN_RADIUS = 30;
    private static final int SPAWN_INTERVAL_TICKS = 40; // Every 2 seconds

    @Override
    public String getId() {
        return "mob_pheromone";
    }

    @Override
    public String getName() {
        return "Mob-Pheromone";
    }

    @Override
    public String getDescription() {
        return "Für 2 Minuten: Monster spawnen 3x häufiger um dich herum!";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        // Check if already active
        if (isActive(player.getUUID())) {
            long remaining = getRemainingSeconds(player.getUUID());
            player.sendSystemMessage(
                    Component.literal("§6Pheromone bereits aktiv! Noch §e" + remaining + "s §6übrig."));
            return false;
        }

        // Activate pheromones
        long endTime = System.currentTimeMillis() + (DURATION_SECONDS * 1000L);
        activePheromones.put(player.getUUID(), endTime);

        // Play creepy sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 0.5f, 0.5f);

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§4§l☠ MOB-PHEROMONE AKTIVIERT! ☠"));
        player.sendSystemMessage(Component.literal("§cMonster werden von dir angezogen..."));
        player.sendSystemMessage(Component.literal("§7Dauer: §c" + DURATION_SECONDS + " Sekunden"));
        player.sendSystemMessage(Component.literal(""));

        // Broadcast to others
        var server = ((ServerLevel) player.level()).getServer();
        for (var p : server.getPlayerList().getPlayers()) {
            if (p != player) {
                p.sendSystemMessage(Component.literal("§8[§4☠§8] §7" + player.getName().getString() +
                        " hat Mob-Pheromone aktiviert..."));
            }
        }

        return true;
    }

    /**
     * Called every tick to spawn mobs around players with active pheromones.
     * Should be called from server tick event.
     */
    public static void tickPheromoneEffects(net.minecraft.server.MinecraftServer server) {
        if (activePheromones.isEmpty())
            return;

        long currentTime = System.currentTimeMillis();
        var expiredPlayers = new ArrayList<UUID>();

        for (var entry : activePheromones.entrySet()) {
            UUID playerId = entry.getKey();
            long endTime = entry.getValue();

            // Check if expired
            if (currentTime >= endTime) {
                expiredPlayers.add(playerId);
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player != null) {
                    player.sendSystemMessage(Component.literal("§7Die Mob-Pheromone sind verflogen..."));
                }
                continue;
            }

            // Get player and spawn mobs
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null)
                continue;

            // Only spawn every SPAWN_INTERVAL_TICKS
            if (server.getTickCount() % SPAWN_INTERVAL_TICKS != 0)
                continue;

            spawnMobsAroundPlayer(player);
        }

        // Remove expired
        expiredPlayers.forEach(activePheromones::remove);
    }

    private static void spawnMobsAroundPlayer(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level))
            return;

        Random random = new Random();

        // Spawn 1-3 mobs per interval
        int mobCount = 1 + random.nextInt(3);

        for (int i = 0; i < mobCount; i++) {
            // Random position in radius
            int dx = random.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;
            int dz = random.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;

            // Must be at least 8 blocks away
            if (Math.abs(dx) < 8 && Math.abs(dz) < 8) {
                dx = dx < 0 ? -8 : 8;
            }

            BlockPos spawnPos = player.blockPosition().offset(dx, 0, dz);

            // Find suitable Y level
            spawnPos = findSpawnablePosition(level, spawnPos);
            if (spawnPos == null)
                continue;

            // Choose mob type based on dimension and randomness
            EntityType<? extends Monster> mobType = chooseMobType(level, random);

            // Spawn the mob
            var mob = mobType.spawn(level, spawnPos, EntitySpawnReason.EVENT);
            if (mob != null) {
                // Make mob immediately aware of player
                mob.setTarget(player);
            }
        }
    }

    private static BlockPos findSpawnablePosition(ServerLevel level, BlockPos pos) {
        // Search up and down for a valid spawn position
        for (int y = -5; y <= 10; y++) {
            BlockPos checkPos = pos.offset(0, y, 0);
            if (level.getBlockState(checkPos).isAir() &&
                    level.getBlockState(checkPos.above()).isAir() &&
                    !level.getBlockState(checkPos.below()).isAir()) {
                return checkPos;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends Monster> chooseMobType(ServerLevel level, Random random) {
        // Dimension-specific mob pools
        if (level.dimension() == Level.NETHER) {
            EntityType<?>[] netherMobs = {
                    EntityType.ZOMBIFIED_PIGLIN,
                    EntityType.BLAZE,
                    EntityType.WITHER_SKELETON,
                    EntityType.MAGMA_CUBE,
                    EntityType.GHAST
            };
            return (EntityType<? extends Monster>) netherMobs[random.nextInt(netherMobs.length)];
        } else if (level.dimension() == Level.END) {
            EntityType<?>[] endMobs = {
                    EntityType.ENDERMAN,
                    EntityType.SHULKER
            };
            return (EntityType<? extends Monster>) endMobs[random.nextInt(endMobs.length)];
        } else {
            // Overworld - varied mobs
            EntityType<?>[] overworldMobs = {
                    EntityType.ZOMBIE,
                    EntityType.SKELETON,
                    EntityType.SPIDER,
                    EntityType.CREEPER,
                    EntityType.ENDERMAN,
                    EntityType.WITCH,
                    EntityType.CAVE_SPIDER,
                    EntityType.SLIME
            };
            return (EntityType<? extends Monster>) overworldMobs[random.nextInt(overworldMobs.length)];
        }
    }

    public static boolean isActive(UUID playerId) {
        Long endTime = activePheromones.get(playerId);
        if (endTime == null)
            return false;
        if (System.currentTimeMillis() >= endTime) {
            activePheromones.remove(playerId);
            return false;
        }
        return true;
    }

    public static long getRemainingSeconds(UUID playerId) {
        Long endTime = activePheromones.get(playerId);
        if (endTime == null)
            return 0;
        long remaining = endTime - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000 : 0;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§c☠ Monster spawnen häufiger!"),
                Component.literal("§7Perfekt für Mob-Drop-Objectives"),
                Component.literal("§7Dauer: " + DURATION_SECONDS + " Sekunden"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
