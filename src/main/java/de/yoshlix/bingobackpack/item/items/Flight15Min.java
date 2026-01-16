package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;

import java.util.*;

/**
 * Flight ability for 15 minutes.
 */
public class Flight15Min extends BingoItem {

    private static final int DURATION_SECONDS = 900;

    private static final Map<UUID, Long> flightEndTimes = new HashMap<>();

    @Override
    public String getId() {
        return "flight_15min";
    }

    @Override
    public String getName() {
        return "Flug (15 Min)";
    }

    @Override
    public String getDescription() {
        return "Gibt dir Flugfähigkeit für 15 Minuten.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        Abilities abilities = player.getAbilities();
        abilities.mayfly = true;
        abilities.flying = true;
        player.onUpdateAbilities();

        // Stack flight time if already has flight
        long additionalTime = DURATION_SECONDS * 1000L;
        long newEndTime;
        Long existingEndTime = flightEndTimes.get(player.getUUID());

        // Also check other flight item maps for stacking
        Long flight1EndTime = Flight1Min.getFlightEndTime(player.getUUID());
        Long flight5EndTime = Flight5Min.getFlightEndTime(player.getUUID());

        long currentMaxEndTime = System.currentTimeMillis();
        if (existingEndTime != null && existingEndTime > currentMaxEndTime) {
            currentMaxEndTime = existingEndTime;
        }
        if (flight1EndTime != null && flight1EndTime > currentMaxEndTime) {
            currentMaxEndTime = flight1EndTime;
        }
        if (flight5EndTime != null && flight5EndTime > currentMaxEndTime) {
            currentMaxEndTime = flight5EndTime;
        }

        if (currentMaxEndTime > System.currentTimeMillis()) {
            // Stack time on top of existing flight
            newEndTime = currentMaxEndTime + additionalTime;
        } else {
            newEndTime = System.currentTimeMillis() + additionalTime;
        }

        // Clear other flight maps and consolidate to this one
        Flight1Min.clearFlightTime(player.getUUID());
        Flight5Min.clearFlightTime(player.getUUID());
        flightEndTimes.put(player.getUUID(), newEndTime);

        int totalSeconds = (int) ((newEndTime - System.currentTimeMillis()) / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String timeStr = minutes > 0 ? minutes + " Min " + seconds + " Sek" : totalSeconds + " Sekunden";

        boolean wasStacked = currentMaxEndTime > System.currentTimeMillis();
        player.sendSystemMessage(Component.literal("§b§l✈ §rFlug für " + timeStr + "! " +
                (wasStacked ? "§e(gestackt!)" : "")));
        player.sendSystemMessage(Component.literal("§7Doppelsprung zum Starten!"));

        return true;
    }

    public static void tickFlightExpiry(net.minecraft.server.MinecraftServer server) {
        if (flightEndTimes.isEmpty())
            return;

        long now = System.currentTimeMillis();
        var iterator = flightEndTimes.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now >= entry.getValue()) {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null && !player.isCreative() && !player.isSpectator()) {
                    Abilities abilities = player.getAbilities();
                    abilities.mayfly = false;
                    abilities.flying = false;
                    player.onUpdateAbilities();

                    player.sendSystemMessage(Component.literal("§c§l✈ §rDeine Flugfähigkeit ist abgelaufen!"));
                }
                iterator.remove();
            }
        }
    }

    public static boolean hasTemporaryFlight(UUID playerId) {
        Long endTime = flightEndTimes.get(playerId);
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    /**
     * Get the flight end time for a player.
     */
    public static Long getFlightEndTime(UUID playerId) {
        return flightEndTimes.get(playerId);
    }

    /**
     * Clear flight time for a player (used when consolidating to another flight
     * item).
     */
    public static void clearFlightTime(UUID playerId) {
        flightEndTimes.remove(playerId);
    }

    @Override
    public java.util.List<Component> getExtraLore() {
        return java.util.List.of(
                Component.literal("Dauer: 15 Minuten").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
