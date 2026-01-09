package de.yoshlix.bingobackpack.mixin;

import de.yoshlix.bingobackpack.BackpackManager;
import de.yoshlix.bingobackpack.TeamManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BundleItem.class)
public class BundleItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);

        // Check if this is our special backpack bundle
        if (isBackpackBundle(stack)) {
            // This is our backpack item
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    CompoundTag tag = customData.copyTag();
                    if (tag.contains("team")) {
                        String teamName = tag.getString("team").orElse("");
                        // Verify player is still in the team
                        String playerTeam = TeamManager.getInstance().getPlayerTeam(player.getUUID());
                        if (playerTeam == null || !playerTeam.equals(teamName)) {
                            serverPlayer.sendSystemMessage(Component.literal("§cYou are no longer in this team!"));
                            cir.setReturnValue(InteractionResult.FAIL);
                            return;
                        }

                        // Open the backpack
                        BackpackManager.getInstance().openBackpack(serverPlayer, teamName);
                        cir.setReturnValue(InteractionResult.SUCCESS);
                    } else {
                        cir.setReturnValue(InteractionResult.SUCCESS);
                    }
                }
            } else {
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }

    // Prevent adding items to the backpack bundle via clicking in inventory
    @Inject(method = "overrideStackedOnOther", at = @At("HEAD"), cancellable = true)
    private void onOverrideStackedOnOther(ItemStack bundle, Slot slot, ClickAction action, Player player,
            CallbackInfoReturnable<Boolean> cir) {
        if (isBackpackBundle(bundle)) {
            // Cancel the default bundle behavior - don't allow adding items
            cir.setReturnValue(false);
        }
    }

    // Prevent adding items to the backpack bundle via clicking on the bundle
    @Inject(method = "overrideOtherStackedOnMe", at = @At("HEAD"), cancellable = true)
    private void onOverrideOtherStackedOnMe(ItemStack bundle, ItemStack other, Slot slot, ClickAction action,
            Player player, SlotAccess access, CallbackInfoReturnable<Boolean> cir) {
        if (isBackpackBundle(bundle)) {
            // Cancel the default bundle behavior - don't allow adding items
            cir.setReturnValue(false);
        }
    }

    private static boolean isBackpackBundle(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return tag.contains("bingobackpack_item") && tag.getBoolean("bingobackpack_item").orElse(false);
        }
        return false;
    }
}
