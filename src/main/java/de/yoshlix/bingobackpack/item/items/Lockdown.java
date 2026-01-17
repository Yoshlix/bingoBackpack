package de.yoshlix.bingobackpack.item.items;

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

import java.util.*;

/**
 * Locks a chosen enemy player's backpack for 2 minutes.
 * They cannot use any Bingo Items during this time.
 */
public class Lockdown extends BingoItem {

    private static final Map<UUID, List<ServerPlayer>> pendingLockdowns = new HashMap<>();
    private static final Map<UUID, Long> lockedPlayers = new HashMap<>();

    @Override
    public String getId() {
        return "lockdown";
    }

    @Override
    public String getName() {
        return "Lockdown";
    }

    @Override
    public String getDescription() {
        return "Sperrt den Backpack eines Gegners für " + ModConfig.getInstance().lockdownDurationSeconds
                + " Sekunden.";
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

        // Find all online enemy players (excluding already locked ones)
        var server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();
        var enemyPlayers = new ArrayList<ServerPlayer>();

        for (var team : teams) {
            if (team.getId().equals(playerTeam.getId()))
                continue;

            // Skip shielded teams
            if (TeamShield.isTeamShielded(team.getId())) {
                continue;
            }

            for (UUID memberId : team.getPlayers()) {
                ServerPlayer enemy = server.getPlayerList().getPlayer(memberId);
                if (enemy != null && !isLocked(enemy.getUUID()) && !TeamShield.isPlayerShielded(memberId)) {
                    enemyPlayers.add(enemy);
                }
            }
        }

        if (enemyPlayers.isEmpty()) {
            player.sendSystemMessage(
                    Component.literal("§6Keine gegnerischen Spieler verfügbar! (Oder alle geschützt/gesperrt)"));
            return false;
        }

        pendingLockdowns.put(player.getUUID(), enemyPlayers);

        // Show selection menu
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§4§l═══════ Lockdown: Wähle Ziel ═══════"));
        player.sendSystemMessage(Component.literal(""));

        int index = 1;
        for (var enemy : enemyPlayers) {
            Component message = Component.literal("  §e[" + index + "] ")
                    .append(Component.literal(enemy.getName().getString()).withStyle(Style.EMPTY
                            .withColor(ChatFormatting.RED)
                            .withBold(true)
                            .withClickEvent(new ClickEvent.RunCommand("/bingobackpack lockdown " + index))
                            .withHoverEvent(new HoverEvent.ShowText(
                                    Component.literal("§c§lKlicke um " + enemy.getName().getString()
                                            + " zu sperren!\n§7Backpack gesperrt für 2 Minuten")))));
            player.sendSystemMessage(message);
            index++;
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Klicke auf einen Namen zum Auswählen"));
        player.sendSystemMessage(Component.literal("§4§l══════════════════════════════════"));

        return false; // Don't consume yet - wait for selection
    }

    /**
     * Check if player has a pending lockdown selection.
     */
    public static boolean hasPendingLockdown(UUID playerId) {
        return pendingLockdowns.containsKey(playerId);
    }

    /**
     * Process lockdown selection by index.
     */
    public static boolean processLockdown(ServerPlayer user, String selection) {
        List<ServerPlayer> validTargets = pendingLockdowns.get(user.getUUID());
        if (validTargets == null) {
            user.sendSystemMessage(Component.literal("§cKeine ausstehende Lockdown-Auswahl!"));
            return false;
        }

        try {
            int index = Integer.parseInt(selection) - 1;
            if (index < 0 || index >= validTargets.size()) {
                user.sendSystemMessage(Component.literal("§cUngültige Nummer! Wähle 1-" + validTargets.size()));
                return false;
            }

            ServerPlayer target = validTargets.get(index);

            // Apply lockdown
            applyLockdown(target);

            // Notify both players
            user.sendSystemMessage(Component.literal("§4§l🔒 LOCKDOWN! §r§c" + target.getName().getString()
                    + " §rkann 2 Minuten keine Items benutzen!"));

            target.sendSystemMessage(Component.literal(""));
            target.sendSystemMessage(Component.literal("§4§l🔒 LOCKDOWN! §r§cDein Backpack wurde gesperrt!"));
            target.sendSystemMessage(Component.literal("§7Du kannst 2 Minuten keine Bingo-Items benutzen!"));
            target.sendSystemMessage(Component.literal(""));

            // Consume the Lockdown item
            consumeItem(user);

            // Clean up
            pendingLockdowns.remove(user.getUUID());
            return true;

        } catch (NumberFormatException e) {
            user.sendSystemMessage(Component.literal("§cBitte gib eine Nummer ein!"));
            return false;
        }
    }

    /**
     * Called when a player is selected for lockdown (by UUID - legacy).
     */
    public static boolean selectTarget(ServerPlayer user, UUID targetId) {
        List<ServerPlayer> validTargets = pendingLockdowns.get(user.getUUID());
        if (validTargets == null) {
            user.sendSystemMessage(Component.literal("§cKeine ausstehende Lockdown-Auswahl!"));
            return false;
        }

        ServerPlayer target = null;
        for (ServerPlayer p : validTargets) {
            if (p.getUUID().equals(targetId)) {
                target = p;
                break;
            }
        }

        if (target == null) {
            user.sendSystemMessage(Component.literal("§cUngültiges Ziel!"));
            return false;
        }

        // Apply lockdown
        applyLockdown(target);

        // Notify both players
        user.sendSystemMessage(Component.literal("§4§l🔒 LOCKDOWN! §r§c" + target.getName().getString()
                + " §rkann 2 Minuten keine Items benutzen!"));

        target.sendSystemMessage(Component.literal(""));
        target.sendSystemMessage(Component.literal("§4§l🔒 LOCKDOWN! §r§cDein Backpack wurde gesperrt!"));
        target.sendSystemMessage(Component.literal("§7Du kannst 2 Minuten keine Bingo-Items benutzen!"));
        target.sendSystemMessage(Component.literal(""));

        // Clean up
        pendingLockdowns.remove(user.getUUID());

        return true;
    }

    private static void applyLockdown(ServerPlayer player) {
        long endTime = System.currentTimeMillis() + (ModConfig.getInstance().lockdownDurationSeconds * 1000L);
        lockedPlayers.put(player.getUUID(), endTime);
        player.closeContainer();
    }

    private static void consumeItem(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            var itemOpt = de.yoshlix.bingobackpack.item.BingoItemRegistry.fromItemStack(stack);
            if (itemOpt.isPresent() && itemOpt.get().getId().equals("lockdown")) {
                stack.shrink(1);
                return;
            }
        }
    }

    /**
     * Check if a player is currently locked.
     */
    public static boolean isLocked(UUID playerId) {
        Long endTime = lockedPlayers.get(playerId);
        if (endTime == null)
            return false;

        if (System.currentTimeMillis() >= endTime) {
            lockedPlayers.remove(playerId);
            return false;
        }
        return true;
    }

    /**
     * Get remaining lockdown time in seconds.
     */
    public static int getRemainingLockdownSeconds(UUID playerId) {
        Long endTime = lockedPlayers.get(playerId);
        if (endTime == null)
            return 0;

        long remaining = endTime - System.currentTimeMillis();
        return remaining > 0 ? (int) (remaining / 1000) : 0;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Sperrt Gegner-Backpack"),
                Component.literal("§7Dauer: §c" + ModConfig.getInstance().lockdownDurationSeconds + " Sekunden"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
