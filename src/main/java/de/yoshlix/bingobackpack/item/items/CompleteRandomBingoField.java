package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.bingo.BingoBridge;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.ext.ICardEntryView;
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
        var playerTeam = requireTeam(player);
        if (playerTeam == null) {
            return false;
        }

        var cards = BingoBridge.getAllCards();
        if (cards.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cKeine Bingo-Karten vorhanden!"));
            return false;
        }

        // Find all fields this team has not completed yet, across every card
        var incomplete = new java.util.ArrayList<ICardEntryView>();
        for (var card : cards) {
            incomplete.addAll(BingoBridge.getIncompleteEntries(card, playerTeam.getId()));
        }

        if (incomplete.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Alle Felder wurden bereits abgeschlossen!"));
            return false;
        }

        var target = incomplete.get(RANDOM.nextInt(incomplete.size()));

        boolean success = BingoBridge.completeObjective(
                target.getObjectiveId(),
                playerTeam.getId(),
                player.getUUID());

        if (success) {
            player.sendSystemMessage(
                    Component.literal("§a✓ Feld abgeschlossen: §f" + BingoBridge.nameOf(target)));
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
