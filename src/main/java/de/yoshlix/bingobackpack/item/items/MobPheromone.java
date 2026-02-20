package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.BingoBackpack;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.BingoItemRegistry;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.BingoApi;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
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
 * a target player (self or enemy).
 * Perfect for farming mob drops or sabotaging enemies!
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
        return "Für 2 Minuten: Monster spawnen 3x häufiger um dich oder einen Gegner!";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        // Show selection menu
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6§l═══════ Wähle ein Ziel ═══════"));
        player.sendSystemMessage(Component.literal(""));

        // Option 1: Self
        Component selfButton = Component.literal("  [MIR SELBST]  ")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/backpack perks pheromone self"))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("§aMonster spawnen bei dir!\n§7Gut zum Farmen von Items."))));

        // Option 2: Enemy
        Component enemyButton = Component.literal("  [GEGNER TEAM]  ")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.RED)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/backpack perks pheromone enemy"))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("§cMonster spawnen bei einem Gegner!\n§7Gut zur Sabotage."))));

        player.sendSystemMessage(Component.empty().append(selfButton).append(Component.literal("   ")).append(enemyButton));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6§l══════════════════════════════"));

        return false; // Don't consume yet, wait for selection
    }

    /**
     * Activates the pheromone effect based on player choice.
     * @param user The player who used the item
     * @param targetSelf True if targeting self, false if targeting random enemy
     * @return True if successful (and item should be consumed)
     */
    public static boolean activatePheromones(ServerPlayer user, boolean targetSelf) {
        ServerPlayer targetPlayer = user;
        boolean isSabotage = false;

        if (!targetSelf) {
            // Find a random enemy
            targetPlayer = findRandomEnemy(user);
            if (targetPlayer == null) {
                user.sendSystemMessage(Component.literal("§cKein geeigneter Gegner gefunden (oder alle geschützt)!"));
                return false;
            }
            isSabotage = true;
        }

        // Check if already active on target
        if (isActive(targetPlayer.getUUID())) {
            if (isSabotage) {
                user.sendSystemMessage(Component.literal("§cDieser Spieler hat bereits Pheromone!"));
            } else {
                long remaining = getRemainingSeconds(targetPlayer.getUUID());
                user.sendSystemMessage(Component.literal("§6Pheromone bereits aktiv! Noch §e" + remaining + "s §6übrig."));
            }
            return false;
        }

        // Activate pheromones
        long endTime = System.currentTimeMillis() + (DURATION_SECONDS * 1000L);
        activePheromones.put(targetPlayer.getUUID(), endTime);

        // Effects
        targetPlayer.level().playSound(null, targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(),
                SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 0.5f, 0.5f);

        // Messages
        if (isSabotage) {
            // Message to User
            user.sendSystemMessage(Component.literal("§aDu hast Mob-Pheromone auf §e" + targetPlayer.getName().getString() + " §aangesetzt!"));
            
            // Message to Target
            targetPlayer.sendSystemMessage(Component.literal(""));
            targetPlayer.sendSystemMessage(Component.literal("§4§l☠ JEMAND HAT MOB-PHEROMONE AUF DICH ANGESETZT! ☠"));
            targetPlayer.sendSystemMessage(Component.literal("§cMonster werden von dir angezogen..."));
            targetPlayer.sendSystemMessage(Component.literal("§7Dauer: §c" + DURATION_SECONDS + " Sekunden"));
            targetPlayer.sendSystemMessage(Component.literal(""));
            
            // Broadcast
            broadcastToOthers(targetPlayer, user, "§8[§4☠§8] §7" + user.getName().getString() + " hat Mob-Pheromone auf " + targetPlayer.getName().getString() + " hetzt!");
        } else {
            // Self usage
            user.sendSystemMessage(Component.literal(""));
            user.sendSystemMessage(Component.literal("§2§l☠ MOB-PHEROMONE AKTIVIERT! ☠"));
            user.sendSystemMessage(Component.literal("§aMonster werden von dir angezogen (Farm-Modus)..."));
            user.sendSystemMessage(Component.literal("§7Dauer: §a" + DURATION_SECONDS + " Sekunden"));
            user.sendSystemMessage(Component.literal(""));
            
            broadcastToOthers(user, user, "§8[§4☠§8] §7" + user.getName().getString() + " hat Mob-Pheromone aktiviert...");
        }

        // Consume item
        consumeItem(user);
        return true;
    }

    private static ServerPlayer findRandomEnemy(ServerPlayer player) {
        var teams = BingoApi.getTeams();
        if (teams == null) return null;

        var playerTeam = teams.getTeamForPlayer(player.getUUID());
        if (playerTeam == null) return null;

        var server = ((ServerLevel) player.level()).getServer();
        if (server == null) return null;

        List<ServerPlayer> enemies = new ArrayList<>();

        for (var team : teams) {
            if (team.getId().equals(playerTeam.getId())) continue;

            // Check Team Shield
            if (TeamShield.isTeamShielded(team.getId())) continue;

            for (UUID memberId : team.getPlayers()) {
                if (TeamShield.isPlayerShielded(memberId)) continue;
                
                ServerPlayer enemy = server.getPlayerList().getPlayer(memberId);
                if (enemy != null && enemy.isAlive() && !enemy.isSpectator()) {
                    enemies.add(enemy);
                }
            }
        }

        if (enemies.isEmpty()) return null;
        return enemies.get(new Random().nextInt(enemies.size()));
    }

    private static void broadcastToOthers(ServerPlayer target, ServerPlayer source, String message) {
        var server = ((ServerLevel) target.level()).getServer();
        if (server == null) return;
        
        for (var p : server.getPlayerList().getPlayers()) {
            if (p != target && p != source) {
                p.sendSystemMessage(Component.literal(message));
            }
        }
    }

    private static void consumeItem(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            var itemOpt = BingoItemRegistry.fromItemStack(stack);
            if (itemOpt.isPresent() && itemOpt.get().getId().equals("mob_pheromone")) {
                stack.shrink(1);
                return;
            }
        }
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
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        Random random = new Random();

        // Spawn 1-3 mobs per interval
        int mobCount = 1 + random.nextInt(3);

        for (int i = 0; i < mobCount; i++) {
            try {
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
                if (spawnPos == null) {
                    continue;
                }

                // Choose mob type based on dimension and randomness
                EntityType<? extends Monster> mobType = chooseMobType(level, random);
                if (mobType == null) {
                    continue;
                }

                // Validate spawn position is loaded
                if (!level.isLoaded(spawnPos)) {
                    continue;
                }

                // Spawn the mob with error handling
                try {
                    var mob = mobType.spawn(level, spawnPos, EntitySpawnReason.EVENT);
                    if (mob != null && mob.isAlive()) {
                        // Make mob immediately aware of player
                        if (mob instanceof net.minecraft.world.entity.monster.Monster monster) {
                            monster.setTarget(player);
                        }
                    }
                } catch (Exception e) {
                    // Log spawn failures but don't crash - some mobs may fail to spawn
                    // due to dimension restrictions or spawn conditions
                    BingoBackpack.LOGGER.debug("Failed to spawn mob {} at {}: {}", mobType, spawnPos, e.getMessage());
                }
            } catch (Exception e) {
                // Catch any unexpected errors during spawn attempt
                BingoBackpack.LOGGER.warn("Error during mob spawn attempt for player {}: {}", player.getName().getString(), e.getMessage());
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
        // Removed problematic mobs:
        // - GHAST: Requires large spawn space and special conditions, can crash if spawned incorrectly
        // - SHULKER: Only spawns naturally in End Cities, can cause issues if spawned elsewhere
        if (level.dimension() == Level.NETHER) {
            EntityType<?>[] netherMobs = {
                    EntityType.ZOMBIFIED_PIGLIN,
                    EntityType.BLAZE,
                    EntityType.WITHER_SKELETON,
                    EntityType.MAGMA_CUBE,
                    EntityType.PIGLIN,
                    EntityType.HOGLIN
            };
            return (EntityType<? extends Monster>) netherMobs[random.nextInt(netherMobs.length)];
        } else if (level.dimension() == Level.END) {
            // Only Enderman in End - Shulker requires End City structure
            EntityType<?>[] endMobs = {
                    EntityType.ENDERMAN
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
                    EntityType.SLIME,
                    EntityType.DROWNED,
                    EntityType.HUSK
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
                Component.literal("§7Wähle: Bei DIR oder einem GEGNER!"),
                Component.literal("§7Dauer: " + DURATION_SECONDS + " Sekunden"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
