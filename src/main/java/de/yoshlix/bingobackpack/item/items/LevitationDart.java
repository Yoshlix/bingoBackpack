package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class LevitationDart extends BingoItem {

    @Override
    public String getId() {
        return "levitation_dart";
    }

    @Override
    public String getName() {
        return "Schwerkraft-Umkehrer";
    }

    @Override
    public String getDescription() {
        return "Lässt einen zufälligen Gegner schweben.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        var playerTeam = requireTeam(player);
        if (playerTeam == null) {
            return false;
        }

        // Also filters out anyone mid-death/respawn, unlike the manual loop
        // this replaced — that let the effect land on a dead target and fizzle
        // for nothing while still consuming the item.
        var enemies = onlineEnemies(player, playerTeam);

        if (enemies.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cKeine angreifbaren Gegner gefunden!"));
            return false;
        }

        // Pick random enemy
        ServerPlayer target = enemies.get(RANDOM.nextInt(enemies.size()));

        // Apply Levitation
        int duration = (int) (ModConfig.getInstance().levitationDurationSeconds * 20 * getDurationMultiplier());
        int amplifier = ModConfig.getInstance().levitationAmplifier;

        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, duration, amplifier));

        // Notify
        player.sendSystemMessage(Component.literal("§aDu hast " + target.getName().getString() + " schweben lassen!"));
        target.sendSystemMessage(Component.literal("§cUaaahhh! Du wurdest von einem Schwerkraft-Umkehrer getroffen!"));

        return true;
    }
}
