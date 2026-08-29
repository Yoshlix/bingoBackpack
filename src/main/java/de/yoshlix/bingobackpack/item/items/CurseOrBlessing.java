package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.ModConfig;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fluchlos - a coin flip on yourself, except fate keeps score: each curse in
 * a row nudges the odds back toward a blessing, instead of being a plain
 * memoryless 50/50.
 */
public class CurseOrBlessing extends BingoItem {

    private static final double BASE_BLESSING_CHANCE = 0.5;
    private static final double STREAK_BONUS_PER_CURSE = 0.15;
    private static final double MAX_STREAK_BONUS = 0.45;

    // Consecutive curses for a player; reset to 0 on a blessing.
    private static final Map<UUID, Integer> curseStreaks = new HashMap<>();

    @Override
    public String getId() {
        return "curse_or_blessing";
    }

    @Override
    public String getName() {
        return "Fluchlos";
    }

    @Override
    public String getDescription() {
        return "Segen oder Fluch — das Schicksal gleicht sich mit der Zeit aus.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        UUID id = player.getUUID();
        int streak = curseStreaks.getOrDefault(id, 0);
        double blessingChance = Math.min(1.0, BASE_BLESSING_CHANCE + Math.min(MAX_STREAK_BONUS, streak * STREAK_BONUS_PER_CURSE));
        boolean blessed = RANDOM.nextDouble() < blessingChance;

        if (blessed) {
            curseStreaks.remove(id);
            int ticks = ModConfig.getInstance().curseOrBlessingBlessingDurationSeconds * 20;
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, ticks, 1, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 0, false, true, true));
            player.sendSystemMessage(Component.literal("§a§l✦ SEGEN! §rGlück, Geschwindigkeit und Regeneration."));
        } else {
            curseStreaks.merge(id, 1, Integer::sum);
            int ticks = ModConfig.getInstance().curseOrBlessingCurseDurationSeconds * 20;
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, ticks, 1, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, ticks, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, ticks, 0, false, true, true));
            player.sendSystemMessage(Component.literal("§5§l☠ FLUCH! §rDu leuchtest, hast Pech und Übelkeit."));
        }

        return true;
    }

    /** Clear all streaks (e.g. on a new round). */
    public static void clearAllStreaks() {
        curseStreaks.clear();
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§750/50, aber Pech in Folge erhöht deine Segen-Chance"),
                Component.literal("§7Segen: Glück + Speed + Regeneration"),
                Component.literal("§7Fluch: Leuchten + Pech + Übelkeit + Hunger"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
