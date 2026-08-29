package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.bingo.BingoBridge;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Timeouts an entire enemy team - they can't move or interact for a duration.
 */
public class TimeoutTeam extends BingoItem {

    @Override
    public String getId() {
        return "timeout_team";
    }

    @Override
    public String getName() {
        return "Team Timeout";
    }

    @Override
    public String getDescription() {
        return "Friert ein zufälliges gegnerisches Team für " + ModConfig.getInstance().timeoutTeamDurationSeconds
                + " Sekunden ein.";
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

        // Find enemy teams (excluding shielded ones)
        var enemyTeams = new ArrayList<me.jfenn.bingo.api.data.IBingoTeam>();
        for (var team : BingoBridge.getEnemyTeams(playerTeam.getId())) {
            if (!TeamShield.isTeamShielded(team.getId())) {
                enemyTeams.add(team);
            }
        }

        if (enemyTeams.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cKeine gegnerischen Teams! (Oder alle geschützt)"));
            return false;
        }

        // Select random enemy team
        var targetTeam = enemyTeams.get(RANDOM.nextInt(enemyTeams.size()));

        var server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();
        int frozenCount = 0;

        int durationSeconds = ModConfig.getInstance().timeoutTeamDurationSeconds;
        for (UUID memberId : targetTeam.getPlayers()) {
            ServerPlayer target = server.getPlayerList().getPlayer(memberId);
            if (target != null) {
                TimeoutPlayer.applyTimeout(target, durationSeconds);

                target.sendSystemMessage(Component.literal("§c§l❄ DEIN TEAM WURDE EINGEFROREN! ❄"));
                target.sendSystemMessage(Component.literal("§7Von: §e" + player.getName().getString()));
                target.sendSystemMessage(Component
                        .literal("§7Dauer: §c" + ModConfig.getInstance().timeoutTeamDurationSeconds + " Sekunden"));

                frozenCount++;
            }
        }

        if (frozenCount > 0) {
            player.sendSystemMessage(Component.literal("§c§l❄ §rTeam §e" + targetTeam.getId() +
                    " §7wurde eingefroren! (" + frozenCount + " Spieler)"));

            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§c§l❄ TEAM FREEZE! ❄")
                            .append(Component.literal("\n§e" + player.getName().getString()))
                            .append(Component.literal(" §chat Team §e" + targetTeam.getId() + " §ceingefroren!"))
                            .append(Component.literal(
                                    "\n§7(" + frozenCount + " Spieler für "
                                            + ModConfig.getInstance().timeoutTeamDurationSeconds + " Sekunden)")),
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
                Component.literal("§c❄ Friert ALLE Teammitglieder ein"),
                Component.literal("§7Dauer: " + ModConfig.getInstance().timeoutTeamDurationSeconds + " Sekunden"));
    }

    @Override
    public boolean canDropFromMob() {
        return true; // Too powerful
    }
}
