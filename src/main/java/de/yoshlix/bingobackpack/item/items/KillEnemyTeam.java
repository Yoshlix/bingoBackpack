package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.bingo.BingoBridge;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * Kills all members of a random enemy team.
 */
public class KillEnemyTeam extends BingoItem {

    @Override
    public String getId() {
        return "kill_enemy_team";
    }

    @Override
    public String getName() {
        return "Team-Vernichtung";
    }

    @Override
    public String getDescription() {
        return "Tötet alle Mitglieder eines zufälligen gegnerischen Teams.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        var playerTeam = requireTeam(player);
        if (playerTeam == null) {
            return false;
        }

        var teams = BingoBridge.getAllTeams();

        // Find enemy teams (excluding shielded ones)
        var enemyTeams = new java.util.ArrayList<me.jfenn.bingo.api.data.IBingoTeam>();
        for (var team : teams) {
            if (!team.getId().equals(playerTeam.getId()) && !TeamShield.isTeamShielded(team.getId())) {
                enemyTeams.add(team);
            }
        }

        if (enemyTeams.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cKeine gegnerischen Teams! (Oder alle geschützt)"));
            return false;
        }

        // Select random enemy team
        var targetTeam = enemyTeams.get(RANDOM.nextInt(enemyTeams.size()));

        int killCount = 0;
        var server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();

        for (UUID memberId : targetTeam.getPlayers()) {
            ServerPlayer target = server.getPlayerList().getPlayer(memberId);
            if (target != null && target.isAlive()) {
                // Kill the player
                target.kill((net.minecraft.server.level.ServerLevel) target.level());
                killCount++;
            }
        }

        if (killCount > 0) {
            player.sendSystemMessage(Component.literal("§c§l☠ §r§c" + killCount + " Spieler von Team §e" +
                    targetTeam.getId() + " §cwurden getötet!"));

            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§4§l⚔ §c" + player.getName().getString() +
                            " §4hat Team §e" + targetTeam.getId() + " §4vernichtet! §c(" + killCount + " Spieler)"),
                    false);

            return true;
        } else {
            player.sendSystemMessage(Component.literal("§6Keine Spieler des Teams online!"));
            return false;
        }
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§c§lEXTREM MÄCHTIG!"),
                Component.literal("§7Zufälliges gegnerisches Team"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
