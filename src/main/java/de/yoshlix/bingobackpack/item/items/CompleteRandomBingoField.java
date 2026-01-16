package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.BingoApi;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * TEMPLATE: Copy this file to create a new Bingo Item
 * 
 * Steps:
 * 1. Copy this file and rename it (e.g., MyAwesomeItem.java)
 * 2. Change the class name
 * 3. Fill in all the methods
 * 4. Register in BingoItemRegistry.init():
 * register(new MyAwesomeItem());
 */
public class CompleteRandomBingoField extends BingoItem {

    // Optional: Define constants for your item
    // private static final int BASE_VALUE = 10;

    @Override
    public String getId() {
        // Unique ID - lowercase with underscores
        // Example: "my_awesome_item"
        return "complete_random_bingo_field";
    }

    @Override
    public String getName() {
        // Display name (will be colored by rarity)
        // Example: "Magischer Kristall"
        return "Zufälliges Bingo-Feld abschließen";
    }

    @Override
    public String getDescription() {
        // Short description for tooltip
        // Example: "Gibt dir magische Kräfte."
        return "Lässt dich ein zufälliges Bingo-Feld abschließen.";
    }

    @Override
    public ItemRarity getRarity() {
        // Choose one:
        // - ItemRarity.COMMON (15% base drop, white)
        // - ItemRarity.UNCOMMON (8% base drop, green)
        // - ItemRarity.RARE (4% base drop, blue)
        // - ItemRarity.EPIC (1.5% base drop, purple)
        // - ItemRarity.LEGENDARY (0.5% base drop, gold)
        return ItemRarity.EPIC;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        // Get the player's team
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

        // Get card service for scoring manipulation
        var scoringService = BingoApi.getScoringService();
        var game = BingoApi.getGameExtended(); // Use extended game interface

        if (scoringService == null || game == null) {
            player.sendSystemMessage(Component.literal("§cBingo-API nicht verfügbar!"));
            return false;
        }

        // Get all incomplete objectives from the cards
        var cards = game.getAllCards();
        if (cards.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cKeine Bingo-Karten vorhanden!"));
            return false;
        }

        // Find a random incomplete objective for this team
        var incompleteObjectives = new java.util.ArrayList<me.jfenn.bingo.api.data.IBingoObjective>();
        for (var card : cards) {
            for (var objective : card.getObjectives()) {
                if (!objective.hasAchieved(playerTeam.getId())) {
                    incompleteObjectives.add(objective);
                }
            }
        }

        if (incompleteObjectives.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Alle Felder wurden bereits abgeschlossen!"));
            return false;
        }

        // Select a random objective and complete it
        var random = new java.util.Random();
        var randomObjective = incompleteObjectives.get(random.nextInt(incompleteObjectives.size()));

        boolean success = scoringService.completeObjective(
                randomObjective.getId(),
                playerTeam.getId(),
                player.getUUID());

        if (success) {
            String objectiveName = randomObjective.getDisplayName() != null
                    ? randomObjective.getDisplayName()
                    : randomObjective.getId();
            player.sendSystemMessage(Component.literal("§a✓ Feld abgeschlossen: §f" + objectiveName));
        } else {
            player.sendSystemMessage(Component.literal("§cFehler beim Abschließen des Feldes!"));
        }

        return success;
    }

    // ========================================
    // OPTIONAL OVERRIDES (delete if not needed)
    // ========================================

    @Override
    public List<Component> getExtraLore() {
        // Add extra tooltip lines
        // return List.of(
        // Component.literal("Extra Info").withStyle(ChatFormatting.AQUA)
        // );
        return List.of();
    }

    @Override
    public double getDropChanceMultiplier() {
        // Modify drop chance (1.0 = normal, 2.0 = double, 0.5 = half)
        return 1.0;
    }

    @Override
    public boolean canDropFromMob() {
        // Set to false if item should only come from bingo rows
        return true;
    }
}
