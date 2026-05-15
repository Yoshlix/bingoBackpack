package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.ModConfig;
import me.jfenn.bingo.api.BingoApi;
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

        String teamId = playerTeam.getId();

        // Check if team is already shielded
        if (isTeamShielded(teamId)) {
            long remaining = getRemainingShieldTime(teamId);
            player.sendSystemMessage(
                    Component.literal("§6Dein Team ist bereits geschützt! (" + remaining + "s verbleibend)"));
            return false;
        }

        // Activate shield
        long expiryTime = System.currentTimeMillis() + (ModConfig.getInstance().teamShieldDurationSeconds * 1000L);
        shieldedTeams.put(teamId, expiryTime);

        // Notify all team members
        var server = ((ServerLevel) player.level()).getServer();
        for (UUID memberId : playerTeam.getPlayers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                member.sendSystemMessage(Component.literal("§a§l🛡 TEAM SCHILD AKTIVIERT! 🛡"));
                member.sendSystemMessage(Component
                        .literal("§7Euer Team ist für §e" + ModConfig.getInstance().teamShieldDurationSeconds
                                + " Sekunden §7geschützt!"));
                member.sendSystemMessage(Component.literal("§7Aktiviert von: §e" + player.getName().getString()));
            }
        }

        // Broadcast to server
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§a§l🛡 §eTeam " + teamId + " §ahat einen Schild aktiviert! §7("
                        + ModConfig.getInstance().teamShieldDurationSeconds + "s)"),
                false);

        // Play shield sound to all team members
        for (UUID memberId : playerTeam.getPlayers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                member.level().playSound(null, member.getX(), member.getY(), member.getZ(),
                        net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,
                        net.minecraft.sounds.SoundSource.PLAYERS,
                        1.0f, 1.5f);
            }
        }

        return true;
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
        var teams = BingoApi.getTeams();
        if (teams == null)
            return false;

        var playerTeam = teams.getTeamForPlayer(playerId);
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
            var teams = BingoApi.getTeams();
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
