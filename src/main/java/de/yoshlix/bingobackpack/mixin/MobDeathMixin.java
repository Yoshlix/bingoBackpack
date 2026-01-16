package de.yoshlix.bingobackpack.mixin;

import de.yoshlix.bingobackpack.item.BingoItemManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle mob death events for Bingo Item drops.
 */
@Mixin(LivingEntity.class)
public class MobDeathMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void onDie(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Check if the entity was killed by a player
        if (damageSource.getEntity() instanceof Player player) {
            // Trigger the drop check
            BingoItemManager.getInstance().onMobKilled(self, player);
        }
    }
}
