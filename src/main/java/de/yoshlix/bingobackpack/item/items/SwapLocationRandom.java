package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.BingoApi;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Swaps the location of two random players.
 */
public class SwapLocationRandom extends BingoItem {

    private final Random random = new Random();

    @Override
    public String getId() {
        return "swap_location_random";
    }

    @Override
    public String getName() {
        return "Zufälliger Positions-Tausch";
    }

    @Override
    public String getDescription() {
        return "Tauscht die Position von dir und einem zufälligen Gegner.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
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

        // Find all online enemy players (excluding shielded ones)
        var server = ((ServerLevel) player.level()).getServer();
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
                if (enemy != null && !TeamShield.isPlayerShielded(memberId)) {
                    enemyPlayers.add(enemy);
                }
            }
        }

        if (enemyPlayers.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Keine gegnerischen Spieler online! (Oder alle geschützt)"));
            return false;
        }

        // Select random enemy
        ServerPlayer target = enemyPlayers.get(random.nextInt(enemyPlayers.size()));

        // Store positions
        ServerLevel playerLevel = (ServerLevel) player.level();
        ServerLevel targetLevel = (ServerLevel) target.level();

        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();
        float playerYaw = player.getYRot();
        float playerPitch = player.getXRot();

        double targetX = target.getX();
        double targetY = target.getY();
        double targetZ = target.getZ();
        float targetYaw = target.getYRot();
        float targetPitch = target.getXRot();

        // Swap positions
        player.teleportTo(targetLevel, targetX, targetY, targetZ,
                java.util.Set.of(), targetYaw, targetPitch, true);
        target.teleportTo(playerLevel, playerX, playerY, playerZ,
                java.util.Set.of(), playerYaw, playerPitch, true);

        player.sendSystemMessage(Component.literal("§a§lSWAP! §rDu hast mit §e" +
                target.getName().getString() + " §rgetauscht!"));
        target.sendSystemMessage(Component.literal("§c§lSWAP! §r" + player.getName().getString() +
                " §rhat dich teleportiert!"));

        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§6✦ §e" + player.getName().getString() + " §6und §e" +
                        target.getName().getString() + " §6haben die Positionen getauscht!"),
                false);

        return true;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Überraschung für deinen Gegner!"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
