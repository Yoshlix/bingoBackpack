package de.yoshlix.bingobackpack.momentum;

import de.yoshlix.bingobackpack.ModConfig;
import de.yoshlix.bingobackpack.bingo.BingoBridge;
import de.yoshlix.bingobackpack.item.items.TeamShield;
import de.yoshlix.bingobackpack.item.items.TimeoutPlayer;
import me.jfenn.bingo.api.data.IBingoTeam;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Harmful ability: freezes a random enemy team. Mirrors
 * {@link de.yoshlix.bingobackpack.item.items.TimeoutTeam}, reusing the same
 * public {@link TimeoutPlayer#applyTimeout}.
 */
public class StillstandAbility implements MomentumAbility {

    private final Random random = new Random();

    @Override
    public String getId() {
        return "stillstand";
    }

    @Override
    public String getName() {
        return "Stillstand";
    }

    @Override
    public String getDescription() {
        return "Schaden: Friert ein zufälliges Gegnerteam für "
                + ModConfig.getInstance().momentumStillstandDurationSeconds + " Sekunden ein.";
    }

    @Override
    public boolean isHarmful() {
        return true;
    }

    @Override
    public void apply(MinecraftServer server, ServerPlayer activator, IBingoTeam team) {
        List<IBingoTeam> enemyTeams = new ArrayList<>();
        for (IBingoTeam candidate : BingoBridge.getEnemyTeams(team.getId())) {
            if (!TeamShield.isTeamShielded(candidate.getId())) {
                enemyTeams.add(candidate);
            }
        }

        if (enemyTeams.isEmpty()) {
            activator.sendSystemMessage(Component.literal("§6Kein Gegnerteam erreichbar (oder alle geschützt)!"));
            return;
        }

        IBingoTeam target = enemyTeams.get(random.nextInt(enemyTeams.size()));
        int durationSeconds = ModConfig.getInstance().momentumStillstandDurationSeconds;
        int frozenCount = 0;

        for (UUID memberId : target.getPlayers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                TimeoutPlayer.applyTimeout(member, durationSeconds);
                member.sendSystemMessage(Component.literal("§c§l❄ STILLSTAND! ❄ §rEuer Team wurde eingefroren!"));
                frozenCount++;
            }
        }

        if (frozenCount > 0) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§c❄ §e" + activator.getName().getString() + " §chat Team §e" + target.getId()
                            + " §cmit Stillstand eingefroren! §7(" + durationSeconds + "s)"),
                    false);
        }
    }
}
