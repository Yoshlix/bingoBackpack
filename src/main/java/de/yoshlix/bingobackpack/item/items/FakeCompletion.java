package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.BingoApi;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Shows a fake "Bingo Field Complete!" message to all enemy teams.
 * Pure psychological warfare!
 */
public class FakeCompletion extends BingoItem {

    private final Random random = new Random();

    @Override
    public String getId() {
        return "fake_completion";
    }

    @Override
    public String getName() {
        return "Fake Completion";
    }

    @Override
    public String getDescription() {
        return "Zeigt Gegnern eine falsche 'Feld abgeschlossen'-Nachricht.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
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

        // Get the game to access objectives
        var game = BingoApi.getGameExtended();
        if (game == null) {
            player.sendSystemMessage(Component.literal("§cBingo-API nicht verfügbar!"));
            return false;
        }

        // Get a random objective name to make the fake message believable
        var cards = game.getAllCards();
        String fakeItemName = "Diamant-Schwert"; // Default fallback

        if (!cards.isEmpty()) {
            var allObjectives = new java.util.ArrayList<me.jfenn.bingo.api.data.IBingoObjective>();
            for (var card : cards) {
                allObjectives.addAll(card.getObjectives());
            }
            if (!allObjectives.isEmpty()) {
                var randomObjective = allObjectives.get(random.nextInt(allObjectives.size()));
                String displayName = randomObjective.getDisplayName();
                if (displayName != null) {
                    fakeItemName = displayName;
                }
            }
        }

        // Get server to send messages to all enemy players
        var server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();
        int enemiesNotified = 0;

        // Send fake completion message to all enemy team members
        for (var team : teams) {
            if (team.getId().equals(playerTeam.getId()))
                continue;

            for (UUID memberId : team.getPlayers()) {
                ServerPlayer enemy = server.getPlayerList().getPlayer(memberId);
                if (enemy != null) {
                    // Send fake completion message (styled like real Bingo messages)
                    enemy.sendSystemMessage(Component.literal(""));
                    enemy.sendSystemMessage(Component.literal("§a§l✓ §r§a" + player.getName().getString()
                            + " §7hat §e" + fakeItemName + " §7abgeschlossen!"));
                    enemy.sendSystemMessage(Component.literal(""));

                    // Play the bingo sound to make it more convincing
                    enemy.level().playSound(null, enemy.getX(), enemy.getY(), enemy.getZ(),
                            SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0f, 1.0f);

                    enemiesNotified++;
                }
            }
        }

        if (enemiesNotified == 0) {
            player.sendSystemMessage(Component.literal("§6Keine Gegner online!"));
            return false;
        }

        // Notify the user
        player.sendSystemMessage(Component.literal("§d§l🎭 FAKE! §r§d" + enemiesNotified
                + " Gegner denken jetzt, du hast \"" + fakeItemName + "\" abgeschlossen!"));

        return true;
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Psychologische Kriegsführung!"),
                Component.literal("§7Gegner sehen eine falsche Nachricht"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
