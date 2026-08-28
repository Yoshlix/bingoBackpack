package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.net.PcPrankPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Shows a fake "Windows is shutting down" full-screen overlay on a random
 * enemy's client. Purely cosmetic — the target can click or press ESC to
 * dismiss it; nothing touches their actual machine.
 */
public class FakeShutdownItem extends BingoItem {

    @Override
    public String getId() {
        return "fake_shutdown";
    }

    @Override
    public String getName() {
        return "Blauer Bildschirm des Schicksals";
    }

    @Override
    public String getDescription() {
        return "Täuscht auf dem PC eines zufälligen Gegners ein Herunterfahren vor.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        ServerPlayer target = firePcPrank(player, PcPrankPayload.ACTION_SHUTDOWN_SCREEN, "", 0);
        if (target == null) {
            return false;
        }

        player.sendSystemMessage(Component.literal("§8§l⏻ §r§7" + target.getName().getString()
                + " §7glaubt gerade, sein PC fährt herunter..."));

        ((ServerLevel) player.level()).getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§8§l⏻ §e" + player.getName().getString()
                        + " §7hat bei §e" + target.getName().getString()
                        + " §7ein Herunterfahren vorgetäuscht!"),
                false);

        return true;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§8Nur Show — der Gegner klickt es weg."),
                Component.literal("§7Braucht den Client-Mod beim Ziel."));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
