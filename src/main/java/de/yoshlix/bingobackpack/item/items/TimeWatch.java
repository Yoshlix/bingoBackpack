package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class TimeWatch extends BingoItem {

    @Override
    public String getId() {
        return "time_watch";
    }

    @Override
    public String getName() {
        return "Zeitumkehrer";
    }

    @Override
    public String getDescription() {
        return "Wechselt sofort die Tageszeit (Tag ↔ Nacht).";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        long time = level.getDayTime() % 24000;
        boolean isDay = time < 13000;

        if (isDay) {
            // Switch to Night (13000)
            level.setDayTime(level.getDayTime() - time + 13000);
            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("§b§lDING DONG! §r§6" + player.getName().getString()
                            + " §7hat die Zeit auf §9Nacht §7gestellt."),
                    false);
        } else {
            // Switch to Day (1000)
            level.setDayTime(level.getDayTime() - time + 1000 + 24000); // +24000 to advance day count
            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("§e§lRING RING! §r§6" + player.getName().getString()
                            + " §7hat die Zeit auf §eTag §7gestellt."),
                    false);
        }

        return true;
    }
}
