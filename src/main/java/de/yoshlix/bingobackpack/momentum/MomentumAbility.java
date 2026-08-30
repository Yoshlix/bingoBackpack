package de.yoshlix.bingobackpack.momentum;

import me.jfenn.bingo.api.data.IBingoTeam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * One entry in the Momentum ability pool. Implementations mirror
 * {@link de.yoshlix.bingobackpack.item.BingoItem} in shape, but these are
 * earned via the team's Momentum meter rather than picked up as an item.
 */
public interface MomentumAbility {
    String getId();

    String getName();

    String getDescription();

    /** true = targets an enemy team, false = benefits the activating team only. */
    boolean isHarmful();

    /**
     * Applies the ability. {@code activator} is whichever team member ran
     * the activation command; {@code team} is their team.
     */
    void apply(MinecraftServer server, ServerPlayer activator, IBingoTeam team);

    /** Optional per-second upkeep for abilities with an ongoing effect (e.g. Spürsinn). */
    default void tick(MinecraftServer server) {
    }
}
