package de.yoshlix.bingobackpack.mixin;

import de.yoshlix.bingobackpack.BingoIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into Yet Another Bingo's TeamService to trigger a backpack sync
 * whenever
 * players join or leave a team. Uses string targets to avoid a hard compile
 * dependency; the descriptor matches TeamService#joinTeam(IPlayerHandle,
 * BingoTeam) and joinSpectators(IPlayerHandle).
 */
@Mixin(targets = "me.jfenn.bingo.common.team.TeamService")
public class BingoTeamServiceMixin {

    @Inject(method = "joinTeam(Lme/jfenn/bingo/platform/IPlayerHandle;Lme/jfenn/bingo/common/team/BingoTeam;)V", at = @At("TAIL"))
    private void bingoBackpack$afterJoinTeam(Object playerHandle, Object bingoTeam, CallbackInfo ci) {
        BingoIntegration.getInstance().manualSync();
    }

    @Inject(method = "joinSpectators(Lme/jfenn/bingo/platform/IPlayerHandle;)V", at = @At("TAIL"))
    private void bingoBackpack$afterJoinSpectators(Object playerHandle, CallbackInfo ci) {
        BingoIntegration.getInstance().manualSync();
    }
}
