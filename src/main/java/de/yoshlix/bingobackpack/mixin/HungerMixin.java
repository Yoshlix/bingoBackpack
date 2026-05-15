package de.yoshlix.bingobackpack.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.yoshlix.bingobackpack.ModConfig;
import de.yoshlix.bingobackpack.banish.BanishManager;
import net.minecraft.server.level.ServerPlayer;

@Mixin(FoodData.class)
public class HungerMixin {
    @Shadow
    private int foodLevel;

    @Shadow
    private float saturationLevel;

    @Shadow
    private float exhaustionLevel;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(Player player, CallbackInfo ci) {

        if (!ModConfig.getInstance().hungerMixinEnabled)
            return;

        // Don't restore hunger for banished players — they should feel hunger in the arena
        if (player instanceof ServerPlayer serverPlayer) {
            if (BanishManager.getInstance().isBanished(serverPlayer)) {
                return; // Skip hunger restoration, let natural hunger work
            }
        }

        // Only update if values are not already at max to avoid unnecessary operations
        if (this.foodLevel != 20) {
            this.foodLevel = 20;
        }
        if (this.saturationLevel != 20.0F) {
            this.saturationLevel = 20.0F;
        }
        if (this.exhaustionLevel != 0.0F) {
            this.exhaustionLevel = 0.0F;
        }
    }
}