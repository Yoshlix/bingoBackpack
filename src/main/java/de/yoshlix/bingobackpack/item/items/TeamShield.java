package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.bingo.BingoBridge;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.ModConfig;
import me.jfenn.bingo.api.data.IBingoTeam;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Team Shield - Protects your team from enemy items for 30 seconds.
 * While active, enemy PvP items (Kill, Swap, Timeout, etc.) have no effect on
 * shielded players.
 */
public class TeamShield extends BingoItem {

    // Map of team ID -> shield expiry time
    private static final Map<String, Long> shieldedTeams = new HashMap<>();

    @Override
    public String getId() {
        return "team_shield";
    }

    @Override
    public String getName() {
        return "Team Schild";
    }

    @Override
    public String getDescription() {
        return "Schützt dein Team " + ModConfig.getInstance().teamShieldDurationSeconds
                + " Sekunden vor feindlichen Items.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        var playerTeam = requireTeam(player);
        if (playerTeam == null) {
            return false;
        }

        String teamId = playerTeam.getId();

        // Check if team is already shielded
        if (isTeamShielded(teamId)) {
            long remaining = getRemainingShieldTime(teamId);
            player.sendSystemMessage(
                    Component.literal("§6Dein Team ist bereits geschützt! (" + remaining + "s verbleibend)"));
            return false;
        }

        var server = ((ServerLevel) player.level()).getServer();
        activateShield(teamId, server, ModConfig.getInstance().teamShieldDurationSeconds * 1000L, player);
        return true;
    }

    /**
     * Activates the team shield directly, bypassing the item/inventory flow.
     * Used by the item's own {@link #onUse} above and reused by the Momentum
     * "Bollwerk" ability, which grants the same shield for a longer duration
     * as an earned reward instead of an item pickup — both share this one
     * {@code shieldedTeams} map, so every existing shield check keeps working
     * no matter which path activated it.
     */
    public static void activateShield(String teamId, MinecraftServer server, long durationMs,
            ServerPlayer activatedBy) {
        long expiryTime = System.currentTimeMillis() + durationMs;
        shieldedTeams.put(teamId, expiryTime);
        int durationSeconds = (int) (durationMs / 1000L);

        var teams = BingoBridge.getAllTeams();
        IBingoTeam team = null;
        for (var t : teams) {
            if (t.getId().equals(teamId)) {
                team = t;
                break;
            }
        }

        if (team != null) {
            for (UUID memberId : team.getPlayers()) {
                ServerPlayer member = server.getPlayerList().getPlayer(memberId);
                if (member != null) {
                    member.sendSystemMessage(Component.literal("§a§l🛡 TEAM SCHILD AKTIVIERT! 🛡"));
                    member.sendSystemMessage(Component
                            .literal("§7Euer Team ist für §e" + durationSeconds + " Sekunden §7geschützt!"));
                    member.sendSystemMessage(
                            Component.literal("§7Aktiviert von: §e" + activatedBy.getName().getString()));
                    member.level().playSound(null, member.getX(), member.getY(), member.getZ(),
                            net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,
                            net.minecraft.sounds.SoundSource.PLAYERS,
                            1.0f, 1.5f);
                }
            }
        }

        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§a§l🛡 §eTeam " + teamId + " §ahat einen Schild aktiviert! §7("
                        + durationSeconds + "s)"),
                false);
    }

    /**
     * Check if a team is currently shielded.
     */
    public static boolean isTeamShielded(String teamId) {
        Long expiryTime = shieldedTeams.get(teamId);
        if (expiryTime == null)
            return false;

        if (System.currentTimeMillis() >= expiryTime) {
            shieldedTeams.remove(teamId);
            return false;
        }

        return true;
    }

    /**
     * Check if a player is protected by a team shield.
     */
    public static boolean isPlayerShielded(UUID playerId) {
        var teams = BingoBridge.getAllTeams();
        if (teams == null)
            return false;

        var playerTeam = BingoBridge.getTeamForPlayer(playerId);
        if (playerTeam == null)
            return false;

        return isTeamShielded(playerTeam.getId());
    }

    /**
     * Get remaining shield time for a team in seconds.
     */
    public static long getRemainingShieldTime(String teamId) {
        Long expiryTime = shieldedTeams.get(teamId);
        if (expiryTime == null)
            return 0;

        long remaining = (expiryTime - System.currentTimeMillis()) / 1000L;
        return Math.max(0, remaining);
    }

    /**
     * Clean up expired shields (call periodically from tick handler).
     */
    public static void tickShieldExpiry(MinecraftServer server) {
        long now = System.currentTimeMillis();

        var expired = new ArrayList<String>();
        for (var entry : shieldedTeams.entrySet()) {
            if (now >= entry.getValue()) {
                expired.add(entry.getKey());
            }
        }

        for (String teamId : expired) {
            shieldedTeams.remove(teamId);

            // Notify team that shield expired
            var teams = BingoBridge.getAllTeams();
            if (teams != null) {
                for (var team : teams) {
                    if (team.getId().equals(teamId)) {
                        for (UUID memberId : team.getPlayers()) {
                            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
                            if (member != null) {
                                member.sendSystemMessage(Component
                                        .literal("§c§l⚠ SCHILD ABGELAUFEN! §7Euer Team ist nicht mehr geschützt."));
                            }
                        }

                        server.getPlayerList().broadcastSystemMessage(
                                Component.literal("§7Der Schild von Team §e" + teamId + " §7ist abgelaufen."),
                                false);
                        break;
                    }
                }
            }
        }
    }

    public static void clearAllShields() {
        shieldedTeams.clear();
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Schützt vor:"),
                Component.literal("§c• Kill-Items"),
                Component.literal("§c• Swap-Items"),
                Component.literal("§c• Timeout-Items"),
                Component.literal("§7Dauer: §e" + ModConfig.getInstance().teamShieldDurationSeconds + " Sekunden"));
    }
}
