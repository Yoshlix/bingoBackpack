package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;

import java.util.*;

/**
 * Flight ability for 5 minutes.
 */
public class Flight5Min extends BingoItem {

    private static final Map<UUID, Long> flightEndTimes = new HashMap<>();
    private static final Set<UUID> flightWarningSent = new HashSet<>();

    @Override
    public String getId() {
        return "flight_5min";
    }

    @Override
    public String getName() {
        return "Flug (5 Min)";
    }

    @Override
    public String getDescription() {
        return "Gibt dir Flugfähigkeit für 5 Minuten.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        Abilities abilities = player.getAbilities();
        abilities.mayfly = true;
        abilities.flying = true;
        player.onUpdateAbilities();

        // Stack flight time if already has flight
        long additionalTime = ModConfig.getInstance().flightDuration5Min * 1000L;
        long newEndTime;
        Long existingEndTime = flightEndTimes.get(player.getUUID());

        // Also check other flight item maps for stacking
        Long flight1EndTime = Flight1Min.getFlightEndTime(player.getUUID());
        Long flight15EndTime = Flight15Min.getFlightEndTime(player.getUUID());

        long currentMaxEndTime = System.currentTimeMillis();
        if (existingEndTime != null && existingEndTime > currentMaxEndTime) {
            currentMaxEndTime = existingEndTime;
        }
        if (flight1EndTime != null && flight1EndTime > currentMaxEndTime) {
            currentMaxEndTime = flight1EndTime;
        }
        if (flight15EndTime != null && flight15EndTime > currentMaxEndTime) {
            currentMaxEndTime = flight15EndTime;
        }

        if (currentMaxEndTime > System.currentTimeMillis()) {
            // Stack time on top of existing flight
            newEndTime = currentMaxEndTime + additionalTime;
        } else {
            newEndTime = System.currentTimeMillis() + additionalTime;
        }

        // Clear other flight maps and consolidate to this one
        Flight1Min.clearFlightTime(player.getUUID());
        Flight15Min.clearFlightTime(player.getUUID());
        flightEndTimes.put(player.getUUID(), newEndTime);
        flightWarningSent.remove(player.getUUID());

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
            long remainingMillis = entry.getValue() - now;
            if (remainingMillis <= 10_000L && remainingMillis > 0 && flightWarningSent.add(entry.getKey())) {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    sendFlightWarning(player);
                }
            }

            if (now >= entry.getValue()) {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null && !player.isCreative() && !player.isSpectator()) {
                    Abilities abilities = player.getAbilities();
                    abilities.mayfly = false;
                    abilities.flying = false;
                    player.onUpdateAbilities();

                    player.sendSystemMessage(Component.literal("§c§l✈ §rDeine Flugfähigkeit ist abgelaufen!"));
                }
                flightWarningSent.remove(entry.getKey());
                iterator.remove();
            }
        }
    }

    private static void sendFlightWarning(ServerPlayer player) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 10));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§c§lNUR NOCH 10 SEKUNDEN FLUG!")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§7Lande jetzt besser sicher.")));
        player.sendSystemMessage(Component.literal("§c§l✈ WARNUNG: §r§cNur noch 10 Sekunden Flugzeit!"));
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
        flightWarningSent.remove(playerId);
    }

    public static void clearAllFlightTimes() {
        flightEndTimes.clear();
        flightWarningSent.clear();
    }

    @Override
    public java.util.List<Component> getExtraLore() {
        return java.util.List.of(
                Component.literal("Dauer: 5 Minuten").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
