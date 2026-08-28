package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.bingo.BingoBridge;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.ext.ICardEntryView;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Completes one random field the player's team has not finished yet.
 */
public class CompleteRandomBingoField extends BingoItem {

    @Override
    public String getId() {
        return "complete_random_bingo_field";
    }

    @Override
    public String getName() {
        return "Zufälliges Bingo-Feld abschließen";
    }

    @Override
    public String getDescription() {
        return "Lässt dich ein zufälliges Bingo-Feld abschließen.";
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

    @Override
    public List<Component> getExtraLore() {
        return List.of();
    }

    @Override
    public double getDropChanceMultiplier() {
        return 1.0;
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
