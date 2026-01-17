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
 * Deletes a random number of items from a random enemy player's inventory.
 */
public class DeleteEnemyItems extends BingoItem {

    private final Random random = new Random();

    @Override
    public String getId() {
        return "delete_enemy_items";
    }

    @Override
    public String getName() {
        return "Items Zerstören";
    }

    @Override
    public String getDescription() {
        return "Zerstört " + ModConfig.getInstance().deleteEnemyItemsMin + "-"
                + ModConfig.getInstance().deleteEnemyItemsMax + " Items eines zufälligen Gegners.";
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

        // Determine number of items to delete
        int itemsToDelete = ModConfig.getInstance().deleteEnemyItemsMin + random
                .nextInt(ModConfig.getInstance().deleteEnemyItemsMax - ModConfig.getInstance().deleteEnemyItemsMin + 1);

        // Find non-empty slots (excluding unbreakable items from starter kit)
        List<Integer> nonEmptySlots = new ArrayList<>();
        for (int i = 0; i < target.getInventory().getContainerSize(); i++) {
            ItemStack stack = target.getInventory().getItem(i);
            if (!stack.isEmpty() && !isUnbreakable(stack)) {
                nonEmptySlots.add(i);
            }
        }

        if (nonEmptySlots.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Gegner hat keine Items!"));
            return false;
        }

        // Shuffle and delete
        java.util.Collections.shuffle(nonEmptySlots);
        int actualDeletes = Math.min(itemsToDelete, nonEmptySlots.size());

        List<String> deletedItems = new ArrayList<>();

        for (int i = 0; i < actualDeletes; i++) {
            int slot = nonEmptySlots.get(i);
            ItemStack stack = target.getInventory().getItem(slot);
            deletedItems.add(stack.getHoverName().getString());
            target.getInventory().setItem(slot, ItemStack.EMPTY);
        }

        player.sendSystemMessage(Component.literal("§c§l✘ §r" + actualDeletes + " Items von §e" +
                target.getName().getString() + " §rzerstört!"));
        player.sendSystemMessage(Component.literal("§7Zerstört: §f" + String.join(", ", deletedItems)));

        target.sendSystemMessage(Component.literal("§4§l✘ §c" + player.getName().getString() +
                " §chat " + actualDeletes + " deiner Items zerstört!"));
        target.sendSystemMessage(Component.literal("§7Verloren: §f" + String.join(", ", deletedItems)));

        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§4✘ §c" + player.getName().getString() + " §4hat " + actualDeletes +
                        " Items von §e" + target.getName().getString() + " §4zerstört!"),
                false);

        return true;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§c§lBÖSARTIG!"),
                Component.literal("§7Zerstört " + ModConfig.getInstance().deleteEnemyItemsMin + "-"
                        + ModConfig.getInstance().deleteEnemyItemsMax + " Items"));
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
