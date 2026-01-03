package de.yoshlix.bingobackpack.mixin;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public class HungerMixin {
    @Shadow
    private int foodLevel;

    @Shadow
    private float saturationLevel;

    @Shadow
    private float exhaustionLevel;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        // Keep food level always at max (20)
        this.foodLevel = 20;
        // Keep saturation at max
        this.saturationLevel = 20.0F;
        // Reset exhaustion to prevent any hunger drain
        this.exhaustionLevel = 0.0F;
    }
}