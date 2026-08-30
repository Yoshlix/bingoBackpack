package de.yoshlix.bingobackpack.momentum;

import de.yoshlix.bingobackpack.ModConfig;
import de.yoshlix.bingobackpack.item.items.TeamShield;
import me.jfenn.bingo.api.data.IBingoTeam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Bonus ability: a guaranteed, longer-duration Team Shield. Reuses
 * {@link TeamShield#activateShield} so it shares the exact same state as the
 * item — every existing shield check keeps working regardless of which path
 * activated it.
 */
public class BollwerkAbility implements MomentumAbility {

    @Override
    public String getId() {
        return "bollwerk";
    }

    @Override
    public String getName() {
        return "Bollwerk";
    }

    @Override
    public String getDescription() {
        return "Bonus: Garantierter Team-Schild für "
                + ModConfig.getInstance().momentumBollwerkDurationSeconds + " Sekunden.";
    }

    @Override
    public boolean isHarmful() {
        return false;
    }

    @Override
    public void apply(MinecraftServer server, ServerPlayer activator, IBingoTeam team) {
        TeamShield.activateShield(team.getId(), server,
                ModConfig.getInstance().momentumBollwerkDurationSeconds * 1000L, activator);
    }
}
