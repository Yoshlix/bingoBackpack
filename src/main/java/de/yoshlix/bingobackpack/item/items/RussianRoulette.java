package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.ModConfig;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Russisches Roulette (Team) - 1 in 6, a random online team member (yourself
 * included) takes a harsh hit. The other 5 in 6, the whole team gets a
 * consolation buff instead of nothing.
 */
public class RussianRoulette extends BingoItem {

    @Override
    public String getId() {
        return "russian_roulette";
    }

    @Override
    public String getName() {
        return "Russisches Roulette";
    }

    @Override
    public String getDescription() {
        return "1 von 6: ein zufälliges Teammitglied trifft's hart. Sonst: Team-Buff für alle.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        var playerTeam = requireTeam(player);
        if (playerTeam == null) {
            return false;
        }

        var server = ((ServerLevel) player.level()).getServer();
        List<ServerPlayer> teammates = new ArrayList<>();
        for (UUID memberId : playerTeam.getPlayers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null && member.isAlive()) {
                teammates.add(member);
            }
        }

        if (teammates.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cKein Teammitglied online!"));
            return false;
        }

        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§4§l🔫 " + player.getName().getString()
                        + " §7lädt die Trommel und drückt ab..."),
                false);

        if (RANDOM.nextInt(6) == 0) {
            ServerPlayer victim = teammates.get(RANDOM.nextInt(teammates.size()));
            int ticks = ModConfig.getInstance().russianRouletteHitDurationSeconds * 20;
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 2, false, true, true));
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 2, false, true, true));
            victim.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, ticks, 1, false, true, true));

            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§4§l💥 TREFFER! §r§c" + victim.getName().getString()
                            + " §7hat's erwischt!"),
                    false);
        } else {
            int ticks = ModConfig.getInstance().russianRouletteMissDurationSeconds * 20;
            for (ServerPlayer member : teammates) {
                member.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 0, false, true, true));
            }
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§a§l*klick* §7Leere Kammer — Team bekommt einen kleinen Schub."),
                    false);
        }

        return true;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§71/6: zufälliges Teammitglied hart debufft"),
                Component.literal("§75/6: ganzes Team bekommt Speed-Boost"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
