package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.banish.BanishManager;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class BanishEpicItem extends BingoItem {
    @Override
    public String getId() {
        return "banish_epic";
    }

    @Override
    public String getName() {
        return "Banish (Epic)";
    }

    @Override
    public String getDescription() {
        return "Verbannt einen zufälligen Gegner ins End.\nEr muss eine Aufgabe lösen, um zurückzukehren.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
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

        ServerPlayer target = enemies.get(RANDOM.nextInt(enemies.size()));
        BanishManager.getInstance().banish(target);

        player.level().getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§d§l" + target.getName().getString() + " §evon §a"
                        + player.getName().getString() + " §everbannt!"),
                false);

        return true;
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
