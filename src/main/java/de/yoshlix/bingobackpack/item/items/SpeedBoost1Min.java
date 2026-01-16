package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/**
 * Speed boost for 1 minute.
 */
public class SpeedBoost1Min extends BingoItem {

    private static final int DURATION_SECONDS = 60;
    private static final int AMPLIFIER = 1; // Speed II

    @Override
    public String getId() {
        return "speed_boost_1min";
    }

    @Override
    public String getName() {
        return "Schnelligkeit (1 Min)";
    }

    @Override
    public String getDescription() {
        return "Gibt dir Speed II für 1 Minute.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.COMMON;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        int durationTicks = DURATION_SECONDS * 20;

        // Check if player already has speed effect and stack the duration
        var existingEffect = player.getEffect(MobEffects.SPEED);
        if (existingEffect != null) {
            durationTicks += existingEffect.getDuration();
        }

        player.addEffect(new MobEffectInstance(
                MobEffects.SPEED,
                durationTicks,
                AMPLIFIER,
                false,
                true,
                true));

        int totalSeconds = durationTicks / 20;
        player.sendSystemMessage(
                Component.literal("§a§lWOOSH! §rSpeed für " + totalSeconds + " Sekunden! " +
                        (existingEffect != null ? "§e(gestackt!)" : "")));
        return true;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("Speed II").withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)),
                Component.literal("Dauer: " + DURATION_SECONDS + " Sekunden").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
