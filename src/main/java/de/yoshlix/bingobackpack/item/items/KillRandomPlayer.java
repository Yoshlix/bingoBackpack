package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.BingoApi;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Kills a random player from any enemy team.
 */
public class KillRandomPlayer extends BingoItem {

    private final Random random = new Random();

    @Override
    public String getId() {
        return "kill_random_player";
    }

    @Override
    public String getName() {
        return "Zufälliger Kill";
    }

    @Override
    public String getDescription() {
        return "Tötet einen zufälligen gegnerischen Spieler.";
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

        // Find all online enemy players (excluding shielded ones)
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
                if (enemy != null && enemy.isAlive() && !TeamShield.isPlayerShielded(memberId)) {
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

        // Kill the target
        target.kill((net.minecraft.server.level.ServerLevel) target.level());

        player.sendSystemMessage(Component.literal("§c§l☠ §r§c" + target.getName().getString() + " §7wurde getötet!"));

        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§4§l⚔ §c" + player.getName().getString() +
                        " §4hat §e" + target.getName().getString() + " §4durch Magie getötet!"),
                false);

        return true;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Zufälliger gegnerischer Spieler"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
