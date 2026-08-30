package de.yoshlix.bingobackpack.momentum;

import de.yoshlix.bingobackpack.ModConfig;
import me.jfenn.bingo.api.data.IBingoTeam;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.UUID;

/**
 * Bonus ability: Speed III + Haste III for the whole team. Same effect
 * application as {@link de.yoshlix.bingobackpack.item.items.SpeedBoost5Min},
 * just stronger and earned instead of dropped.
 */
public class AnsturmAbility implements MomentumAbility {

    @Override
    public String getId() {
        return "ansturm";
    }

    @Override
    public String getName() {
        return "Ansturm";
    }

    @Override
    public String getDescription() {
        return "Bonus: Speed III + Haste III fürs ganze Team für "
                + ModConfig.getInstance().momentumAnsturmDurationSeconds + " Sekunden.";
    }

    @Override
    public boolean isHarmful() {
        return false;
    }

    @Override
    public void apply(MinecraftServer server, ServerPlayer activator, IBingoTeam team) {
        int durationTicks = ModConfig.getInstance().momentumAnsturmDurationSeconds * 20;

        for (UUID memberId : team.getPlayers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member == null) {
                continue;
            }
            member.addEffect(new MobEffectInstance(MobEffects.SPEED, durationTicks, 2, false, true, true));
            member.addEffect(new MobEffectInstance(MobEffects.HASTE, durationTicks, 2, false, true, true));
            member.sendSystemMessage(Component.literal("§a§lANSTURM! §rSpeed III + Haste III aktiv!"));
        }
    }
}
