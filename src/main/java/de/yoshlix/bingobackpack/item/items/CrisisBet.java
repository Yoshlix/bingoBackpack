package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.ModConfig;
import de.yoshlix.bingobackpack.bingo.BingoBridge;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.BingoItemRegistry;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/**
 * Krisenwette - a gamble whose odds depend on your team's current standing.
 * Behind the leading team: guaranteed boost. Tied with or ahead of it: a
 * genuine 50/50 between an extra boost and a real setback.
 */
public class CrisisBet extends BingoItem {

    @Override
    public String getId() {
        return "crisis_bet";
    }

    @Override
    public String getName() {
        return "Krisenwette";
    }

    @Override
    public String getDescription() {
        return "Im Rückstand: garantierter Boost. In Führung: Alles-oder-nichts.";
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

        int myScore = playerTeam.getScore().getItems();
        int maxScore = myScore;
        for (var team : BingoBridge.getAllTeams()) {
            maxScore = Math.max(maxScore, team.getScore().getItems());
        }

        boolean behind = myScore < maxScore;

        if (behind) {
            applyBoost(player);
            player.sendSystemMessage(Component.literal(
                    "§b§lAUFHOLJAGD! §rDu liegst zurück — garantierter Boost!"));
        } else if (RANDOM.nextBoolean()) {
            applyBoost(player);
            var bonusPool = BingoItemRegistry.getItemsByRarity(ItemRarity.UNCOMMON);
            if (!bonusPool.isEmpty()) {
                var bonus = bonusPool.get(RANDOM.nextInt(bonusPool.size()));
                player.getInventory().add(bonus.createItemStack());
            }
            player.sendSystemMessage(Component.literal(
                    "§a§lGLÜCK DES FÜHRENDEN! §rDu warst schon vorne und hast trotzdem gewonnen!"));
        } else {
            int punishTicks = ModConfig.getInstance().crisisBetPunishDurationSeconds * 20;
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, punishTicks, 1, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, punishTicks, 1, false, true, true));
            player.sendSystemMessage(Component.literal(
                    "§c§lÜBERMUT! §rDu warst vorne und hast verloren!"));
        }

        ((ServerLevel) player.level()).getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§d✦ §e" + player.getName().getString() + " §7hat die Krisenwette gewagt!"),
                false);

        return true;
    }

    private void applyBoost(ServerPlayer player) {
        int ticks = ModConfig.getInstance().crisisBetBoostDurationSeconds * 20;
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 1, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, ticks, 1, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ticks, 0, false, true, true));
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Im Rückstand: garantierter Speed/Haste/Strength-Boost"),
                Component.literal("§7In Führung: 50/50 Boost oder Debuff"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
