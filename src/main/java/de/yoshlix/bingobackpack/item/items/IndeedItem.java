package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.net.PcPrankPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Opens indeed.com in the default browser on a random enemy's client — the
 * "you'll be needing a new job" gag. The client only opens http/https URLs.
 */
public class IndeedItem extends BingoItem {

    private static final String URL = "https://de.indeed.com";

    @Override
    public String getId() {
        return "indeed_application";
    }

    @Override
    public String getName() {
        return "Bewerbungshilfe";
    }

    @Override
    public String getDescription() {
        return "Öffnet indeed.com auf dem PC eines zufälligen Gegners.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        ServerPlayer target = firePcPrank(player, PcPrankPayload.ACTION_OPEN_URL, URL, 0);
        if (target == null) {
            return false;
        }

        player.sendSystemMessage(Component.literal("§9§l🔍 §r§7" + target.getName().getString()
                + " §7darf sich schon mal neu bewerben."));

        ((ServerLevel) player.level()).getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§9§l🔍 §e" + player.getName().getString()
                        + " §7hat §e" + target.getName().getString()
                        + " §7auf Jobsuche geschickt!"),
                false);

        return true;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§9Öffnet den Browser beim Ziel."),
                Component.literal("§7Braucht den Client-Mod beim Ziel."));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
