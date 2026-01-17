package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import de.yoshlix.bingobackpack.ModConfig;
import de.yoshlix.bingobackpack.BingoBackpack;
import me.jfenn.bingo.api.BingoApi;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class LevitationDart extends BingoItem {

    private final Random random = new Random();

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
        var teams = BingoApi.getTeams();
        if (teams == null) {
            player.sendSystemMessage(Component.literal("§cKein Bingo-Spiel aktiv!"));
            return false;
        }

        var playerTeam = teams.getTeamForPlayer(player.getUUID());
        if (playerTeam == null) {
            player.sendSystemMessage(Component.literal("§cDu bist in keinem Team!"));
            return false;
        }

        // Find targets
        List<ServerPlayer> enemies = new ArrayList<>();
        var server = player.getServer();
        if (server == null)
            return false;

        for (var team : teams) {
            if (team.getId().equals(playerTeam.getId()))
                continue;

            if (TeamShield.isTeamShielded(team.getId()))
                continue;

            for (UUID memberId : team.getPlayers()) {
                if (TeamShield.isPlayerShielded(memberId))
                    continue;

                ServerPlayer enemy = server.getPlayerList().getPlayer(memberId);
                if (enemy != null) {
                    enemies.add(enemy);
                }
            }
        }

        if (enemies.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cKeine angreifbaren Gegner gefunden!"));
            return false;
        }

        // Pick random enemy
        ServerPlayer target = enemies.get(random.nextInt(enemies.size()));

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
