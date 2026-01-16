package de.yoshlix.bingobackpack.mixin;

import de.yoshlix.bingobackpack.item.BingoItemManager;
import de.yoshlix.bingobackpack.item.BingoItemRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to handle right-click usage of Bingo Items (paper items).
 */
@Mixin(ServerPlayerGameMode.class)
public class BingoItemUseMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onUseItemOn(ServerPlayer player, Level level, ItemStack stack,
            InteractionHand hand, BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir) {
        handleBingoItemUse(player, stack, hand, cir);
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void onUseItem(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        handleBingoItemUse(player, stack, hand, cir);
    }

    private void handleBingoItemUse(ServerPlayer player, ItemStack stack, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        // Check if it's a Bingo Item
        if (!BingoItemRegistry.isBingoItem(stack)) {
            return;
        }

        // Try to use the item
        boolean consumed = BingoItemManager.getInstance().tryUseItem(player, stack);

        if (consumed) {
            // Consume one item from the stack
            stack.shrink(1);
            cir.setReturnValue(InteractionResult.SUCCESS);
        } else {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
