package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.BingoApi;
import me.jfenn.bingo.api.data.IBingoObjective;
import me.jfenn.bingo.api.data.IBingoTeam;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Münzwurf des Schicksals - 50/50 chance to complete a random field
 * for your team OR for an enemy team!
 */
public class CoinFlipOfFate extends BingoItem {

    private final Random random = new Random();

    @Override
    public String getId() {
        return "coin_flip_of_fate";
    }

    @Override
    public String getName() {
        return "Münzwurf des Schicksals";
    }

    @Override
    public String getDescription() {
        return "50/50 Chance: Completed ein Feld für DICH oder für den GEGNER!";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
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

        var scoringService = BingoApi.getScoringService();
        var game = BingoApi.getGameExtended();

        if (scoringService == null || game == null) {
            player.sendSystemMessage(Component.literal("§cBingo-API nicht verfügbar!"));
            return false;
        }

        var server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();

        // Dramatic coin flip animation in chat
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§6§l🪙 " + player.getName().getString() + " wirft die Münze des Schicksals... 🪙"),
                false);

        // Play suspenseful sound
        for (var p : server.getPlayerList().getPlayers()) {
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
                    SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.MASTER, 1.0f, 0.5f);
        }

        // 50/50 coin flip
        boolean playerWins = random.nextBoolean();

        // Determine which team gets the completion
        IBingoTeam targetTeam;
        if (playerWins) {
            targetTeam = playerTeam;
        } else {
            // Find a random enemy team
            var enemyTeams = new ArrayList<IBingoTeam>();
            for (var team : teams) {
                if (!team.getId().equals(playerTeam.getId())) {
                    enemyTeams.add(team);
                }
            }
            if (enemyTeams.isEmpty()) {
                player.sendSystemMessage(Component.literal("§6Keine Gegnerteams vorhanden - du gewinnst automatisch!"));
                targetTeam = playerTeam;
                playerWins = true;
            } else {
                targetTeam = enemyTeams.get(random.nextInt(enemyTeams.size()));
            }
        }

        // Find incomplete objectives for the target team
        var incompleteObjectives = new ArrayList<IBingoObjective>();
        var cards = game.getAllCards();
        for (var card : cards) {
            for (var objective : card.getObjectives()) {
                if (!objective.hasAchieved(targetTeam.getId())) {
                    incompleteObjectives.add(objective);
                }
            }
        }

        if (incompleteObjectives.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Keine offenen Felder für das Zielteam!"));
            return false;
        }

        // Complete random objective
        var randomObjective = incompleteObjectives.get(random.nextInt(incompleteObjectives.size()));
        boolean success = scoringService.completeObjective(
                randomObjective.getId(),
                targetTeam.getId(),
                player.getUUID());

        if (success) {
            String objectiveName = randomObjective.getDisplayName() != null
                    ? randomObjective.getDisplayName()
                    : randomObjective.getId();

            if (playerWins) {
                // Player wins!
                for (var p : server.getPlayerList().getPlayers()) {
                    p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.0f, 1.2f);
                }
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("§a§l✨ KOPF! ✨ §r§a" + player.getName().getString() +
                                " §7gewinnt! Feld abgeschlossen: §f" + objectiveName),
                        false);
            } else {
                // Enemy wins!
                for (var p : server.getPlayerList().getPlayers()) {
                    p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
                            SoundEvents.WITHER_SPAWN, SoundSource.MASTER, 0.5f, 1.5f);
                }
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("§c§l💀 ZAHL! 💀 §r§cTeam §e" + targetTeam.getId() +
                                " §cgewinnt! Feld abgeschlossen: §f" + objectiveName),
                        false);
                player.sendSystemMessage(Component.literal("§c§lPECH GEHABT! §rDer Gegner profitiert..."));
            }
            return true;
        } else {
            player.sendSystemMessage(Component.literal("§cFehler beim Abschließen!"));
            return false;
        }
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§c§l⚠ HOCHRISKANT! ⚠"),
                Component.literal("§750% Chance: Du gewinnst"),
                Component.literal("§750% Chance: Gegner gewinnt"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
