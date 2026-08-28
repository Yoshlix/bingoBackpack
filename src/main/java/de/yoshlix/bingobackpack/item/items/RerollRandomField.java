package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.bingo.BingoBridge;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Rerolls a random bingo field on the card.
 */
public class RerollRandomField extends BingoItem {

    @Override
    public String getId() {
        return "reroll_random_field";
    }

    @Override
    public String getName() {
        return "Zufälliger Feld-Reroll";
    }

    @Override
    public String getDescription() {
        return "Rollt ein zufälliges Feld auf der Bingo-Karte neu.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        if (!BingoBridge.isAvailable()) {
            player.sendSystemMessage(Component.literal("§cBingo-API nicht verfügbar!"));
            return false;
        }

        var playerTeam = BingoBridge.getTeamForPlayer(player.getUUID());
        if (playerTeam == null) {
            player.sendSystemMessage(Component.literal("§cDu bist in keinem Team!"));
            return false;
        }

        var card = BingoBridge.getCardForTeam(playerTeam.getId());
        if (card == null) {
            player.sendSystemMessage(Component.literal("§cKeine Bingo-Karte vorhanden!"));
            return false;
        }

        // Only reroll fields the team has not completed yet
        var incomplete = BingoBridge.getIncompleteEntries(card, playerTeam.getId());
        if (incomplete.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Keine offenen Felder zum Rerolln!"));
            return false;
        }

        var target = incomplete.get(RANDOM.nextInt(incomplete.size()));
        int x = target.getX();
        int y = target.getY();
        String oldName = BingoBridge.nameOf(target);

        String newObjectiveId = BingoBridge.rerollEntry(card, x, y);
        boolean success = newObjectiveId != null;

        if (success) {
            var newCard = BingoBridge.getCardForTeam(playerTeam.getId());
            String newName = newObjectiveId;
            if (newCard != null) {
                for (var entry : BingoBridge.getEntries(newCard)) {
                    if (entry.getX() == x && entry.getY() == y) {
                        newName = BingoBridge.nameOf(entry);
                        break;
                    }
                }
            }

            player.sendSystemMessage(Component.literal("§a✓ Feld geändert: §c" + oldName + " §a→ §e" + newName));

            // Broadcast to server
            ((net.minecraft.server.level.ServerLevel) player.level()).getServer().getPlayerList()
                    .broadcastSystemMessage(
                            Component.literal("§6✦ §e" + player.getName().getString() +
                                    " §6hat ein Feld rerolled: §c" + oldName + " §6→ §e" + newName),
                            false);

            return true;
        } else {
            player.sendSystemMessage(Component.literal("§cFehler beim Rerolln des Feldes!"));
            return false;
        }
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Nur offene Felder werden rerolled."));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }
}
