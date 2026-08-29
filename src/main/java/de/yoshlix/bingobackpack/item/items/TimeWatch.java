package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;

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
        return ItemRarity.COMMON;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        // ServerClockManager is SavedData — each ServerLevel keeps its own copy
        // of it, so calling this through player.level() while the player is in
        // the Nether/End would silently flip a clock state nobody ever sees,
        // instead of the actual Overworld day/night cycle the description and
        // broadcast promise. Always target the real Overworld level so the
        // item does the same visible thing no matter where it's used.
        if (!(player.level() instanceof ServerLevel playerLevel)) {
            return false;
        }
        ServerLevel level = playerLevel.getServer().overworld();

        // MC 26.2 moved world time behind ClockManager: time is tracked per WorldClock
        // and set by jumping to a named marker, instead of setDayTime(ticks).
        Registry<WorldClock> clockRegistry = level.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK);
        Holder<WorldClock> overworldClock = clockRegistry.getOrThrow(WorldClocks.OVERWORLD);

        long time = level.getOverworldClockTime() % 24000;
        boolean isDay = time < 13000;

        if (isDay) {
            level.clockManager().moveToTimeMarker(overworldClock, ClockTimeMarkers.NIGHT);
            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("§b§lDING DONG! §r§6" + player.getName().getString()
                            + " §7hat die Zeit auf §9Nacht §7gestellt."),
                    false);
        } else {
            level.clockManager().moveToTimeMarker(overworldClock, ClockTimeMarkers.DAY);
            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("§e§lRING RING! §r§6" + player.getName().getString()
                            + " §7hat die Zeit auf §eTag §7gestellt."),
                    false);
        }

        return true;
    }
}
