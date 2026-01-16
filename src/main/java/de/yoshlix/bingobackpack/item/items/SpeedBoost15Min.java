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
 * Speed boost for 15 minutes.
 */
public class SpeedBoost15Min extends BingoItem {

    private static final int DURATION_SECONDS = 900;
    private static final int AMPLIFIER = 2; // Speed III

    @Override
    public String getId() {
        return "speed_boost_15min";
    }

    @Override
    public String getName() {
        return "Schnelligkeit (15 Min)";
    }

    @Override
    public String getDescription() {
        return "Gibt dir Speed III für 15 Minuten.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
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
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String timeStr = seconds > 0 ? minutes + " Min " + seconds + " Sek" : minutes + " Minuten";
        player.sendSystemMessage(Component.literal("§a§lWOOSH! §rSpeed III für " + timeStr + "! " +
                (existingEffect != null ? "§e(gestackt!)" : "")));
        return true;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("Speed III").withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)),
                Component.literal("Dauer: 15 Minuten").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
