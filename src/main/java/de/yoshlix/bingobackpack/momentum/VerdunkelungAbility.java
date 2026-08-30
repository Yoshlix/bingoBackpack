package de.yoshlix.bingobackpack.momentum;

import de.yoshlix.bingobackpack.ModConfig;
import de.yoshlix.bingobackpack.bingo.BingoBridge;
import de.yoshlix.bingobackpack.item.items.TeamShield;
import me.jfenn.bingo.api.data.IBingoTeam;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Harmful ability: a random enemy team (respecting Team Shield) gets
 * Blindness + Slowness. Enemy-team selection mirrors
 * {@link de.yoshlix.bingobackpack.item.items.TimeoutTeam}.
 */
public class VerdunkelungAbility implements MomentumAbility {

    private final Random random = new Random();

    @Override
    public String getId() {
        return "verdunkelung";
    }

    @Override
    public String getName() {
        return "Verdunkelung";
    }

    @Override
    public String getDescription() {
        return "Schaden: Ein zufälliges Gegnerteam erhält Blindheit + Langsamkeit für "
                + ModConfig.getInstance().momentumVerdunkelungDurationSeconds + " Sekunden.";
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
        int durationTicks = ModConfig.getInstance().momentumVerdunkelungDurationSeconds * 20;

        for (UUID memberId : target.getPlayers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                member.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, durationTicks, 0, false, true, true));
                member.addEffect(
                        new MobEffectInstance(MobEffects.SLOWNESS, durationTicks, 1, false, true, true));
                member.sendSystemMessage(Component.literal("§4§lVERDUNKELUNG! §cIhr seid geblendet und verlangsamt!"));
            }
        }

        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§4🌑 §e" + activator.getName().getString() + " §chat Team §e" + target.getId()
                        + " §cmit Verdunkelung getroffen!"),
                false);
    }
}
