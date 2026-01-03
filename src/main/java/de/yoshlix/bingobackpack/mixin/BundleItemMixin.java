package de.yoshlix.bingobackpack.mixin;

import de.yoshlix.bingobackpack.BackpackManager;
import de.yoshlix.bingobackpack.TeamManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
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
    private void onUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<ItemInteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Check if this is our special backpack bundle
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            tag.getBoolean("bingobackpack_item").ifPresent(isBackpackItem -> {
                if (isBackpackItem) {
                    // This is our backpack item
                    if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                        tag.getString("team").ifPresent(teamName -> {
                            // Verify player is still in the team
                            String playerTeam = TeamManager.getInstance().getPlayerTeam(player.getUUID());
                            if (playerTeam == null || !playerTeam.equals(teamName)) {
                                serverPlayer.sendSystemMessage(Component.literal("§cYou are no longer in this team!"));
                                cir.setReturnValue(ItemInteractionResult.FAIL);
                                return;
                            }
                            
                            // Open the backpack
                            BackpackManager.getInstance().openBackpack(serverPlayer, teamName);
                            cir.setReturnValue(ItemInteractionResult.SUCCESS);
                        });
                        
                        // If no team tag, still set success
                        if (!tag.contains("team")) {
                            cir.setReturnValue(ItemInteractionResult.SUCCESS);
                        }
                    } else {
                        cir.setReturnValue(ItemInteractionResult.SUCCESS);
                    }
                }
            });
        }
    }
}
