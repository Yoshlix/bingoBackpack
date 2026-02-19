package de.yoshlix.bingobackpack;

import me.jfenn.bingo.api.BingoApi;
import me.jfenn.bingo.api.data.BingoGameStatus;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.stream.StreamSupport;

/**
 * Prevents players from using certain items in the lobby (PREGAME).
 * Configurable via ModConfig:
 * - lobbyDisableFishingRod: Blocks fishing rod usage
 * - lobbyDisableLevitationPotions: Blocks splash/lingering potions with
 * levitation effect
 */
public class LobbyRestrictions {

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            // Only restrict during PREGAME (lobby)
            try {
                if (!BingoApi.getGame().getStatus().equals(BingoGameStatus.PREGAME)) {
                    return InteractionResult.PASS;
                }
            } catch (Exception e) {
                // Bingo API not available, skip restrictions
                return InteractionResult.PASS;
            }

            ModConfig config = ModConfig.getInstance();
            ItemStack stack = serverPlayer.getItemInHand(hand);

            // Block fishing rod
            if (config.lobbyDisableFishingRod && stack.is(Items.FISHING_ROD)) {
                serverPlayer.sendSystemMessage(Component.literal("§cDie Angel ist in der Lobby nicht erlaubt!"));
                return InteractionResult.FAIL;
            }

            // Block levitation potions (splash & lingering)
            if (config.lobbyDisableLevitationPotions) {
                if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
                    PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
                    if (contents != null && hasLevitationEffect(contents)) {
                        serverPlayer.sendSystemMessage(
                                Component.literal("§cLevitation-Tränke sind in der Lobby nicht erlaubt!"));
                        return InteractionResult.FAIL;
                    }
                }
            }

            return InteractionResult.PASS;
        });
    }

    private static boolean hasLevitationEffect(PotionContents contents) {
        return StreamSupport.stream(contents.getAllEffects().spliterator(), false)
                .anyMatch(effect -> effect.getEffect().value().equals(MobEffects.LEVITATION.value()));
    }
}
