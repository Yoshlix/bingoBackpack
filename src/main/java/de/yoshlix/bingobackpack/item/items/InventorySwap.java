package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.BingoApi;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Swaps the entire inventory with a random enemy player.
 */
public class InventorySwap extends BingoItem {

    private final Random random = new Random();

    @Override
    public String getId() {
        return "inventory_swap";
    }

    @Override
    public String getName() {
        return "Inventar-Tausch";
    }

    @Override
    public String getDescription() {
        return "Tauscht dein komplettes Inventar mit einem zufälligen Gegner.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        var teams = BingoApi.getTeams();
        if (teams == null) {
            player.sendSystemMessage(Component.literal("§cKein Bingo-Spiel aktiv!"));
            return false;
        }

        var playerTeam = teams.getTeamForPlayer(player.getUUID());
        if (playerTeam == null) {
            player.sendSystemMessage(Component.literal("§cDu bist in keinem Team!"));
            return false;
        }

        // Find all online enemy players (excluding shielded ones)
        var server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();
        var enemyPlayers = new ArrayList<ServerPlayer>();

        for (var team : teams) {
            if (team.getId().equals(playerTeam.getId()))
                continue;

            // Skip if the entire team is shielded
            if (TeamShield.isTeamShielded(team.getId())) {
                continue;
            }

            for (UUID memberId : team.getPlayers()) {
                ServerPlayer enemy = server.getPlayerList().getPlayer(memberId);
                if (enemy != null && !TeamShield.isPlayerShielded(memberId)) {
                    enemyPlayers.add(enemy);
                }
            }
        }

        if (enemyPlayers.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Keine gegnerischen Spieler online! (Oder alle geschützt)"));
            return false;
        }

        // Select random enemy
        ServerPlayer target = enemyPlayers.get(random.nextInt(enemyPlayers.size()));

        // Store inventories
        List<ItemStack> playerInventory = new ArrayList<>();
        List<ItemStack> targetInventory = new ArrayList<>();

        // Track unbreakable items to restore them after swap
        java.util.Map<Integer, ItemStack> playerUnbreakableItems = new java.util.HashMap<>();
        java.util.Map<Integer, ItemStack> targetUnbreakableItems = new java.util.HashMap<>();

        // Copy player inventory (excluding this item which will be consumed and
        // unbreakable items)
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            // Skip this item (the swap item being used)
            var bingoItem = de.yoshlix.bingobackpack.item.BingoItemRegistry.fromItemStack(stack);
            if (bingoItem.isPresent() && bingoItem.get().getId().equals("inventory_swap")) {
                playerInventory.add(ItemStack.EMPTY);
            } else if (isUnbreakable(stack)) {
                // Save unbreakable items to restore later (starter kit)
                playerUnbreakableItems.put(i, stack.copy());
                playerInventory.add(ItemStack.EMPTY);
            } else {
                playerInventory.add(stack.copy());
            }
        }

        // Copy target inventory (excluding unbreakable items)
        for (int i = 0; i < target.getInventory().getContainerSize(); i++) {
            ItemStack stack = target.getInventory().getItem(i);
            if (isUnbreakable(stack)) {
                // Save unbreakable items to restore later (starter kit)
                targetUnbreakableItems.put(i, stack.copy());
                targetInventory.add(ItemStack.EMPTY);
            } else {
                targetInventory.add(stack.copy());
            }
        }

        // Clear and swap
        player.getInventory().clearContent();
        target.getInventory().clearContent();

        // Give player the target's inventory
        for (int i = 0; i < Math.min(targetInventory.size(), player.getInventory().getContainerSize()); i++) {
            player.getInventory().setItem(i, targetInventory.get(i));
        }

        // Give target the player's inventory
        for (int i = 0; i < Math.min(playerInventory.size(), target.getInventory().getContainerSize()); i++) {
            target.getInventory().setItem(i, playerInventory.get(i));
        }

        // Restore unbreakable items to their original owners
        for (var entry : playerUnbreakableItems.entrySet()) {
            player.getInventory().setItem(entry.getKey(), entry.getValue());
        }
        for (var entry : targetUnbreakableItems.entrySet()) {
            target.getInventory().setItem(entry.getKey(), entry.getValue());
        }

        player.sendSystemMessage(Component.literal("§a§lSWAP! §rDu hast das Inventar mit §e" +
                target.getName().getString() + " §rgetauscht!"));
        target.sendSystemMessage(Component.literal("§c§lSWAP! §r" + player.getName().getString() +
                " §rhat dein Inventar gestohlen!"));

        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§6§l⚡ §e" + player.getName().getString() + " §6und §e" +
                        target.getName().getString() + " §6haben Inventare getauscht!"),
                false);

        return true; // Item already handled in inventory swap
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§c§lEXTREM RISKANT!"),
                Component.literal("§7Dein Inventar geht auch weg..."));
    }

    @Override
    public boolean canDropFromMob() {
        return false; // Too powerful
    }

    /**
     * Check if an item is unbreakable (part of starter kit).
     */
    private boolean isUnbreakable(ItemStack stack) {
        return stack.has(DataComponents.UNBREAKABLE);
    }
}
