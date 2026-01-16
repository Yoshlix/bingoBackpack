package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.BingoApi;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Random;

/**
 * Rerolls a random bingo field on the card.
 */
public class RerollRandomField extends BingoItem {

    private final Random random = new Random();

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
        var cardService = BingoApi.getCardService();
        var game = BingoApi.getGameExtended();
        var teams = BingoApi.getTeams();

        if (cardService == null || game == null || teams == null) {
            player.sendSystemMessage(Component.literal("§cBingo-API nicht verfügbar!"));
            return false;
        }

        var playerTeam = teams.getTeamForPlayer(player.getUUID());
        if (playerTeam == null) {
            player.sendSystemMessage(Component.literal("§cDu bist in keinem Team!"));
            return false;
        }

        var card = game.getActiveCard();
        if (card == null) {
            player.sendSystemMessage(Component.literal("§cKeine Bingo-Karte vorhanden!"));
            return false;
        }

        // Find incomplete objectives (only reroll incomplete ones)
        var incompletePositions = new java.util.ArrayList<int[]>();
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                var objective = card.getObjective(x, y);
                if (objective != null && !objective.hasAchieved(playerTeam.getId())) {
                    incompletePositions.add(new int[] { x, y });
                }
            }
        }

        if (incompletePositions.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Keine offenen Felder zum Rerolln!"));
            return false;
        }

        // Select random position
        int[] pos = incompletePositions.get(random.nextInt(incompletePositions.size()));
        int x = pos[0];
        int y = pos[1];

        // Get current objective name before reroll
        var oldObjective = card.getObjective(x, y);
        String oldName = oldObjective != null && oldObjective.getDisplayName() != null ? oldObjective.getDisplayName()
                : "Unbekannt";

        // Reroll the tile
        boolean success = cardService.rerollTile(null, x, y, java.util.Set.of());

        if (success) {
            // Get new objective name
            var newCard = game.getActiveCard();
            var newObjective = newCard != null ? newCard.getObjective(x, y) : null;
            String newName = newObjective != null && newObjective.getDisplayName() != null
                    ? newObjective.getDisplayName()
                    : "Neues Feld";

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
