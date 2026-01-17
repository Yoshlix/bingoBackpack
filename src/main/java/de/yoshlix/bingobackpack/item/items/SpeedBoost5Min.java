package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/**
 * Speed boost for 5 minutes.
 */
public class SpeedBoost5Min extends BingoItem {

    @Override
    public String getId() {
        return "speed_boost_5min";
    }

    @Override
    public String getName() {
        return "Schnelligkeit (5 Min)";
    }

    @Override
    public String getDescription() {
        return "Gibt dir Speed II + Haste II für 5 Minuten.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.UNCOMMON;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        int durationTicks = ModConfig.getInstance().speedBoostDuration5Min * 20;

        // Check if player already has speed effect and stack the duration
        var existingEffect = player.getEffect(MobEffects.SPEED);
        if (existingEffect != null) {
            durationTicks += existingEffect.getDuration();
        }

        player.addEffect(new MobEffectInstance(
                MobEffects.SPEED,
                durationTicks,
                ModConfig.getInstance().speedBoostAmplifier5Min,
                false,
                true,
                true));

        // Also add Haste for faster mining
        player.addEffect(new MobEffectInstance(
                MobEffects.HASTE,
                durationTicks,
                ModConfig.getInstance().speedBoostAmplifier5Min,
                false,
                true,
                true));

        int totalSeconds = durationTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String timeStr = seconds > 0 ? minutes + " Min " + seconds + " Sek" : minutes + " Minuten";
        player.sendSystemMessage(Component.literal("§a§lWOOSH! §rSpeed für " + timeStr + "! " +
                (existingEffect != null ? "§e(gestackt!)" : "")));
        return true;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("Speed II + Haste II").withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)),
                Component.literal("Dauer: 5 Minuten").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
