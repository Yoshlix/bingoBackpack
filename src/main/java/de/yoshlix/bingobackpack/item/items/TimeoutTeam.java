package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.BingoApi;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.*;

/**
 * Timeouts an entire enemy team - they can't move or interact for a duration.
 */
public class TimeoutTeam extends BingoItem {

    private static final int TIMEOUT_DURATION_SECONDS = 300;

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
        return "Friert ein zufälliges gegnerisches Team für " + TIMEOUT_DURATION_SECONDS + " Sekunden ein.";
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

        // Find enemy teams (excluding shielded ones)
        var enemyTeams = new ArrayList<me.jfenn.bingo.api.data.IBingoTeam>();
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
        var random = new Random();
        var targetTeam = enemyTeams.get(random.nextInt(enemyTeams.size()));

        var server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();
        int frozenCount = 0;

        for (UUID memberId : targetTeam.getPlayers()) {
            ServerPlayer target = server.getPlayerList().getPlayer(memberId);
            if (target != null) {
                applyTimeout(target);

                target.sendSystemMessage(Component.literal("§c§l❄ DEIN TEAM WURDE EINGEFROREN! ❄"));
                target.sendSystemMessage(Component.literal("§7Von: §e" + player.getName().getString()));
                target.sendSystemMessage(Component.literal("§7Dauer: §c" + TIMEOUT_DURATION_SECONDS + " Sekunden"));

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
                                    "\n§7(" + frozenCount + " Spieler für " + TIMEOUT_DURATION_SECONDS + " Sekunden)")),
                    false);

            return true;
        } else {
            player.sendSystemMessage(Component.literal("§6Keine Spieler des Teams online!"));
            return false;
        }
    }

    private void applyTimeout(ServerPlayer target) {
        int durationTicks = TIMEOUT_DURATION_SECONDS * 20;

        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, durationTicks, 255, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, durationTicks, 255, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, 255, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, durationTicks, 0, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, durationTicks, 128, false, false, true));
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§c§lEXTREM MÄCHTIG!"),
                Component.literal("§c❄ Friert ALLE Teammitglieder ein"),
                Component.literal("§7Dauer: " + TIMEOUT_DURATION_SECONDS + " Sekunden"));
    }

    @Override
    public boolean canDropFromMob() {
        return false; // Too powerful
    }
}
