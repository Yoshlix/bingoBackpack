package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.banish.BanishManager;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class BanishLegendaryItem extends BingoItem {
    @Override
    public String getId() {
        return "banish_legendary";
    }

    @Override
    public String getName() {
        return "Banish (Legendary)";
    }

    @Override
    public String getDescription() {
        return "Verbannt alle Gegner ins End.\nSie müssen eine Aufgabe lösen, um zurückzukehren.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        var playerTeam = requireTeam(player);
        if (playerTeam == null) {
            return false;
        }

        var enemies = onlineEnemies(player, playerTeam);
        if (enemies.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Kein erreichbarer Gegner (oder alle geschützt)!"));
            return false;
        }

        for (ServerPlayer target : enemies) {
            BanishManager.getInstance().banish(target);
        }

        player.level().getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§d§l" + player.getName().getString()
                        + " §ehat das gesamte gegnerische Team verbannt!"),
                false);

        return true;
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
