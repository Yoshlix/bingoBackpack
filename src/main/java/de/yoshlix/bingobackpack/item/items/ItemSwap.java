package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.ModConfig;
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
 * Swaps a random number of items with a random enemy player.
 */
public class ItemSwap extends BingoItem {

    private final Random random = new Random();

    @Override
    public String getId() {
        return "item_swap";
    }

    @Override
    public String getName() {
        return "Item-Tausch";
    }

    @Override
    public String getDescription() {
        return "Tauscht " + ModConfig.getInstance().itemSwapMin + "-" + ModConfig.getInstance().itemSwapMax
                + " zufällige Items mit einem Gegner.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
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

        // Determine number of items to swap
        int itemsToSwap = ModConfig.getInstance().itemSwapMin
                + random.nextInt(ModConfig.getInstance().itemSwapMax - ModConfig.getInstance().itemSwapMin + 1);

        // Find non-empty slots from both players
        List<Integer> playerNonEmptySlots = new ArrayList<>();
        List<Integer> targetNonEmptySlots = new ArrayList<>();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            // Skip this item and unbreakable items (starter kit)
            var bingoItem = de.yoshlix.bingobackpack.item.BingoItemRegistry.fromItemStack(stack);
            if (!stack.isEmpty() && !isUnbreakable(stack)
                    && (bingoItem.isEmpty() || !bingoItem.get().getId().equals("item_swap"))) {
                playerNonEmptySlots.add(i);
            }
        }

        for (int i = 0; i < target.getInventory().getContainerSize(); i++) {
            ItemStack stack = target.getInventory().getItem(i);
            // Skip unbreakable items (starter kit)
            if (!stack.isEmpty() && !isUnbreakable(stack)) {
                targetNonEmptySlots.add(i);
            }
        }

        // Swap up to itemsToSwap items
        int actualSwaps = Math.min(itemsToSwap,
                Math.min(playerNonEmptySlots.size(), targetNonEmptySlots.size()));

        if (actualSwaps == 0) {
            player.sendSystemMessage(Component.literal("§6Keine Items zum Tauschen!"));
            return false;
        }

        // Shuffle slots
        java.util.Collections.shuffle(playerNonEmptySlots);
        java.util.Collections.shuffle(targetNonEmptySlots);

        List<String> swappedFromPlayer = new ArrayList<>();
        List<String> swappedFromTarget = new ArrayList<>();

        for (int i = 0; i < actualSwaps; i++) {
            int playerSlot = playerNonEmptySlots.get(i);
            int targetSlot = targetNonEmptySlots.get(i);

            ItemStack playerItem = player.getInventory().getItem(playerSlot).copy();
            ItemStack targetItem = target.getInventory().getItem(targetSlot).copy();

            swappedFromPlayer.add(playerItem.getHoverName().getString());
            swappedFromTarget.add(targetItem.getHoverName().getString());

            player.getInventory().setItem(playerSlot, targetItem);
            target.getInventory().setItem(targetSlot, playerItem);
        }

        player.sendSystemMessage(Component.literal("§a§lSWAP! §r" + actualSwaps + " Items mit §e" +
                target.getName().getString() + " §rgetauscht!"));
        player.sendSystemMessage(Component.literal("§7Erhalten: §f" + String.join(", ", swappedFromTarget)));

        target.sendSystemMessage(Component.literal("§c§lSWAP! §r" + player.getName().getString() +
                " §rhat " + actualSwaps + " Items getauscht!"));
        target.sendSystemMessage(Component.literal("§7Erhalten: §f" + String.join(", ", swappedFromPlayer)));

        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§6✦ §e" + player.getName().getString() + " §6hat " + actualSwaps +
                        " Items mit §e" + target.getName().getString() + " §6getauscht!"),
                false);

        return true;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Tauscht " + ModConfig.getInstance().itemSwapMin + "-"
                        + ModConfig.getInstance().itemSwapMax + " Items"),
                Component.literal("§7Zufälliger gegnerischer Spieler"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }

    /**
     * Check if an item is unbreakable (part of starter kit).
     */
    private boolean isUnbreakable(ItemStack stack) {
        return stack.has(DataComponents.UNBREAKABLE);
    }
}
