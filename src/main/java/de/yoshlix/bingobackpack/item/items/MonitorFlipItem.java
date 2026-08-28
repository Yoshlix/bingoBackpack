package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.ModConfig;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.net.PcPrankPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Rotates all displays on a random enemy's client 180° for a set time, after
 * which the client reverts them. Windows only — on other platforms the client
 * just ignores it.
 */
public class MonitorFlipItem extends BingoItem {

    private int durationSeconds() {
        return ModConfig.getInstance().monitorFlipDurationSeconds;
    }

    @Override
    public String getId() {
        return "monitor_flip";
    }

    @Override
    public String getName() {
        return "Kopfstand";
    }

    @Override
    public String getDescription() {
        return "Dreht den Bildschirm eines zufälligen Gegners für 2 Minuten um 180°.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        ServerPlayer target = firePcPrank(player, PcPrankPayload.ACTION_FLIP_MONITOR, "", durationSeconds());
        if (target == null) {
            return false;
        }

        player.sendSystemMessage(Component.literal("§d§l⟳ §r§7Der Bildschirm von §e"
                + target.getName().getString() + " §7steht jetzt Kopf."));

        ((ServerLevel) player.level()).getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§d§l⟳ §e" + player.getName().getString()
                        + " §7hat §e" + target.getName().getString()
                        + " §7den Monitor umgedreht!"),
                false);

        return true;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§d180° für 2 Minuten, dann zurück."),
                Component.literal("§7Nur Windows, braucht den Client-Mod."));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
