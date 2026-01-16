package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;

import java.util.*;

/**
 * Flight ability for 5 minutes.
 */
public class Flight5Min extends BingoItem {

    private static final int DURATION_SECONDS = 300;

    private static final Map<UUID, Long> flightEndTimes = new HashMap<>();

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

        long endTime = System.currentTimeMillis() + (DURATION_SECONDS * 1000L);
        flightEndTimes.put(player.getUUID(), endTime);

        player.sendSystemMessage(Component.literal("§b§l✈ §rDu kannst jetzt fliegen für 5 Minuten!"));
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
