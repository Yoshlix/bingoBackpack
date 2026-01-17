package de.yoshlix.bingobackpack.item;

import de.yoshlix.bingobackpack.BingoBackpack;
import de.yoshlix.bingobackpack.ModConfig;
import de.yoshlix.bingobackpack.item.items.Lockdown;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Manager for Bingo Item operations.
 * Handles item usage, drops, and giving items to players.
 */
public class BingoItemManager {

    private static BingoItemManager instance;
    private final Random random = new Random();

    // Config values (can be modified)
    private double globalDropChanceMultiplier = 1.0;
    private boolean dropsEnabled = true;

    // Cooldown tracking for mob drops
    private final Map<UUID, Long> lastDropTime = new HashMap<>();

    public static BingoItemManager getInstance() {
        if (instance == null) {
            instance = new BingoItemManager();
        }
        return instance;
    }

    private BingoItemManager() {
    }

    /**
     * Try to use a Bingo Item from an ItemStack.
     * 
     * @param player The player using the item
     * @param stack  The ItemStack being used
     * @return true if the item was used (and should be consumed)
     */
    public boolean tryUseItem(ServerPlayer player, ItemStack stack) {
        Optional<BingoItem> itemOpt = BingoItemRegistry.fromItemStack(stack);

        if (itemOpt.isEmpty()) {
            return false;
        }

        BingoItem item = itemOpt.get();

        // Check if player is locked down
        if (Lockdown.isLocked(player.getUUID())) {
            int remaining = Lockdown.getRemainingLockdownSeconds(player.getUUID());
            player.sendSystemMessage(
                    Component.literal("§4§l🔒 GESPERRT! §r§cDein Backpack ist noch " + remaining + "s gesperrt!"));
            return false;
        }

        try {
            boolean consumed = item.onUse(player);

            if (consumed) {
                // Play success sound
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.5f);

                // Log usage
                BingoBackpack.LOGGER.debug("Player {} used Bingo Item: {}",
                        player.getName().getString(), item.getName());
            }

            return consumed;
        } catch (Exception e) {
            BingoBackpack.LOGGER.error("Error using Bingo Item {}: {}", item.getId(), e.getMessage());
            player.sendSystemMessage(Component.literal("§cFehler beim Verwenden des Items!"));
            return false;
        }
    }

    /**
     * Called when a mob is killed by a player.
     * Determines if a Bingo Item should drop.
     * 
     * @param killedEntity The entity that was killed
     * @param killer       The player who killed it
     */
    public void onMobKilled(LivingEntity killedEntity, Player killer) {
        if (!dropsEnabled)
            return;
        if (!(killer instanceof ServerPlayer serverPlayer))
            return;
        if (killedEntity instanceof Player)
            return; // Don't drop from players

        // Check cooldown
        UUID playerId = serverPlayer.getUUID();
        Long lastDrop = lastDropTime.get(playerId);
        if (lastDrop != null && System.currentTimeMillis() - lastDrop < ModConfig.getInstance().dropCooldownMs) {
            return; // Still on cooldown
        }

        // Optional: Only drop from monsters
        // if (!(killedEntity instanceof Monster)) return;

        // Get all droppable items and try to drop one
        for (BingoItem item : BingoItemRegistry.getDroppableItems()) {
            double dropChance = item.getDropChance() * globalDropChanceMultiplier;

            // Bonus chance for stronger mobs
            if (killedEntity instanceof Monster monster) {
                double healthBonus = monster.getMaxHealth() / 20.0; // 1.0 for normal mobs
                dropChance *= Math.min(healthBonus, 2.0); // Cap at 2x
            }

            if (random.nextDouble() < dropChance) {
                dropItemAtEntity(killedEntity, item);

                // Set cooldown
                lastDropTime.put(playerId, System.currentTimeMillis());

                // Notify player
                serverPlayer.sendSystemMessage(
                        Component.literal("§6Ein §" + item.getRarity().getColor().getChar() +
                                item.getName() + " §6ist gedroppt!"));

                // Only one item per kill
                return;
            }
        }
    }

    /**
     * Drop a Bingo Item at an entity's location.
     */
    private void dropItemAtEntity(Entity entity, BingoItem item) {
        ItemStack stack = item.createItemStack();

        ItemEntity itemEntity = new ItemEntity(
                entity.level(),
                entity.getX(),
                entity.getY() + 0.5,
                entity.getZ(),
                stack);

        // Add some random motion
        itemEntity.setDeltaMovement(
                (random.nextDouble() - 0.5) * 0.2,
                random.nextDouble() * 0.2 + 0.2,
                (random.nextDouble() - 0.5) * 0.2);

        entity.level().addFreshEntity(itemEntity);
    }

    /**
     * Give a specific Bingo Item to a player.
     */
    public void giveItem(ServerPlayer player, BingoItem item) {
        giveItem(player, item, 1);
    }

    /**
     * Give a specific Bingo Item to a player with a specific count.
     */
    public void giveItem(ServerPlayer player, BingoItem item, int count) {
        ItemStack stack = item.createItemStack(count);

        if (!player.getInventory().add(stack)) {
            // Inventory full, drop at feet
            player.drop(stack, false);
        }

        // Notify player
        player.sendSystemMessage(
                Component.literal("§aDu hast ")
                        .append(Component.literal(item.getName())
                                .withStyle(item.getRarity().getColor()))
                        .append(Component.literal(" §aerhalten!")));

        // Play sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.0f);
    }

    /**
     * Give a random item of a specific rarity to a player.
     */
    public void giveRandomItem(ServerPlayer player, ItemRarity rarity) {
        var items = BingoItemRegistry.getItemsByRarity(rarity);
        if (items.isEmpty()) {
            BingoBackpack.LOGGER.warn("No items registered for rarity: {}", rarity);
            return;
        }

        BingoItem item = items.get(random.nextInt(items.size()));
        giveItem(player, item);
    }

    /**
     * Give a completely random droppable item to a player.
     */
    public void giveRandomItem(ServerPlayer player) {
        BingoItemRegistry.getRandomDroppableItem(random)
                .ifPresent(item -> giveItem(player, item));
    }

    // ========================================
    // Configuration
    // ========================================

    public void setGlobalDropChanceMultiplier(double multiplier) {
        this.globalDropChanceMultiplier = multiplier;
    }

    public double getGlobalDropChanceMultiplier() {
        return globalDropChanceMultiplier;
    }

    public void setDropsEnabled(boolean enabled) {
        this.dropsEnabled = enabled;
    }

    public boolean isDropsEnabled() {
        return dropsEnabled;
    }
}
