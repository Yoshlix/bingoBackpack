package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.BingoBackpack;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.ModConfig;
import me.jfenn.bingo.api.BingoApi;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Abilities;

import java.util.*;

/**
 * Timeouts a chosen player - they can't move or interact for a duration.
 */
public class TimeoutPlayer extends BingoItem {

    private static final Map<UUID, List<ServerPlayer>> pendingTimeouts = new HashMap<>();
    private static final Map<UUID, Long> timedOutPlayers = new HashMap<>();

    @Override
    public String getId() {
        return "timeout_player";
    }

    @Override
    public String getName() {
        return "Spieler Timeout";
    }

    @Override
    public String getDescription() {
        return "Friert einen gewählten Gegner für " + ModConfig.getInstance().timeoutPlayerDurationSeconds
                + " Sekunden ein.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        var teams = BingoApi.getTeams();
        if (teams == null) {
            player.sendSystemMessage(Component.literal("§cKein Bingo-Spiel aktiv!"));
            return false;
        }

        var playerTeam = teams.getTeamForPlayer(player.getUUID());
        if (playerTeam == null) {
            player.sendSystemMessage(Component.literal("§cDu bist in keinem Team!"));
            return false;
        }

        // Find all online enemy players (excluding shielded and already timed out ones)
        var server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();
        var enemyPlayers = new ArrayList<ServerPlayer>();

        for (var team : teams) {
            if (team.getId().equals(playerTeam.getId()))
                continue;

            // Skip if the entire team is shielded
            if (TeamShield.isTeamShielded(team.getId())) {
                continue;
            }

            for (UUID memberId : team.getPlayers()) {
                ServerPlayer enemy = server.getPlayerList().getPlayer(memberId);
                if (enemy != null && !isTimedOut(enemy.getUUID()) && !TeamShield.isPlayerShielded(memberId)) {
                    enemyPlayers.add(enemy);
                }
            }
        }

        if (enemyPlayers.isEmpty()) {
            player.sendSystemMessage(
                    Component.literal("§6Keine gegnerischen Spieler verfügbar! (Oder alle geschützt)"));
            return false;
        }

        pendingTimeouts.put(player.getUUID(), enemyPlayers);

        // Show selection menu
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§c§l═══════ Wähle einen Spieler ═══════"));
        player.sendSystemMessage(Component.literal(""));

        int index = 1;
        for (var enemy : enemyPlayers) {
            Component message = Component.literal("  §e[" + index + "] ")
                    .append(Component.literal(enemy.getName().getString()).withStyle(Style.EMPTY
                            .withColor(ChatFormatting.RED)
                            .withClickEvent(new ClickEvent.RunCommand("/backpack perks timeout " + index))
                            .withHoverEvent(new HoverEvent.ShowText(
                                    Component.literal("§cKlicke um einzufrieren")))));

            player.sendSystemMessage(message);
            index++;
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Klicke oder schreibe §f/backpack perks timeout <nummer>"));
        player.sendSystemMessage(Component.literal("§c§l══════════════════════════════"));

        return false;
    }

    public static boolean processTimeout(ServerPlayer player, String selection) {
        List<ServerPlayer> enemies = pendingTimeouts.remove(player.getUUID());
        if (enemies == null) {
            player.sendSystemMessage(Component.literal("§cKeine ausstehende Auswahl!"));
            return false;
        }

        int index;
        try {
            index = Integer.parseInt(selection) - 1;
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.literal("§cUngültige Auswahl!"));
            return false;
        }

        if (index < 0 || index >= enemies.size()) {
            player.sendSystemMessage(Component.literal("§cUngültige Auswahl!"));
            return false;
        }

        ServerPlayer target = enemies.get(index);

        if (target == null || target.isRemoved()) {
            player.sendSystemMessage(Component.literal("§cSpieler nicht mehr verfügbar!"));
            return false;
        }

        // Apply timeout effects
        applyTimeout(target);

        player.sendSystemMessage(Component.literal("§c§l❄ §r" + target.getName().getString() +
                " §7wurde für " + ModConfig.getInstance().timeoutPlayerDurationSeconds + " Sekunden eingefroren!"));

        target.sendSystemMessage(Component.literal("§c§l❄ DU WURDEST EINGEFROREN! ❄"));
        target.sendSystemMessage(Component.literal("§7Von: §e" + player.getName().getString()));
        target.sendSystemMessage(
                Component.literal("§7Dauer: §c" + ModConfig.getInstance().timeoutPlayerDurationSeconds + " Sekunden"));

        ((net.minecraft.server.level.ServerLevel) player.level()).getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§c§l❄ §e" + target.getName().getString() +
                        " §cwurde von §e" + player.getName().getString() + " §ceingefroren!"),
                false);

        consumeItem(player);
        return true;
    }

    private static void applyTimeout(ServerPlayer target) {
        // Apply slowness, mining fatigue, weakness, blindness
        int durationTicks = ModConfig.getInstance().timeoutPlayerDurationSeconds * 20;

        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, durationTicks, 255, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, durationTicks, 255, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, 255, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, durationTicks, 0, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, durationTicks, 128, false, false, true)); // Negative
                                                                                                                // jump

        // Disable flight if the player has temporary flight
        if (!target.isCreative() && !target.isSpectator()) {
            Abilities abilities = target.getAbilities();
            if (abilities.mayfly) {
                abilities.mayfly = false;
                abilities.flying = false;
                target.onUpdateAbilities();
                target.sendSystemMessage(Component.literal("§c§l✈ §rDeine Flugfähigkeit wurde eingefroren!"));
            }
            // Clear any temporary flight times
            Flight1Min.clearFlightTime(target.getUUID());
            Flight5Min.clearFlightTime(target.getUUID());
            Flight15Min.clearFlightTime(target.getUUID());
        }

        timedOutPlayers.put(target.getUUID(),
                System.currentTimeMillis() + (ModConfig.getInstance().timeoutPlayerDurationSeconds * 1000L));
    }

    public static boolean isTimedOut(UUID playerId) {
        Long endTime = timedOutPlayers.get(playerId);
        if (endTime == null)
            return false;
        if (System.currentTimeMillis() >= endTime) {
            timedOutPlayers.remove(playerId);
            return false;
        }
        return true;
    }

    public static void tickTimeoutExpiry(net.minecraft.server.MinecraftServer server) {
        if (timedOutPlayers.isEmpty())
            return;

        long now = System.currentTimeMillis();
        timedOutPlayers.entrySet().removeIf(entry -> {
            if (now >= entry.getValue()) {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    player.sendSystemMessage(Component.literal("§a§l❄ §rDu bist wieder frei!"));
                }
                return true;
            }
            return false;
        });
    }

    private static void consumeItem(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            var itemOpt = de.yoshlix.bingobackpack.item.BingoItemRegistry.fromItemStack(stack);
            if (itemOpt.isPresent() && itemOpt.get().getId().equals("timeout_player")) {
                stack.shrink(1);
                return;
            }
        }
    }

    public static boolean hasPendingTimeout(UUID playerId) {
        return pendingTimeouts.containsKey(playerId);
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal(
                        "§c❄ Einfrieren für " + ModConfig.getInstance().timeoutPlayerDurationSeconds + " Sekunden"),
                Component.literal("§7Kann nicht bewegen oder interagieren"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
