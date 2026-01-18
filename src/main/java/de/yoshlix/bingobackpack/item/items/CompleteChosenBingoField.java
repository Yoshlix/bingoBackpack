package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.BingoApi;
import me.jfenn.bingo.api.data.IBingoObjective;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Allows the player to choose and complete a specific bingo field.
 * Shows a list of incomplete objectives to choose from.
 */
public class CompleteChosenBingoField extends BingoItem {

    // Track pending selections per player
    private static final Map<UUID, PendingSelection> pendingSelections = new HashMap<>();

    @Override
    public String getId() {
        return "complete_chosen_bingo_field";
    }

    @Override
    public String getName() {
        return "Bingo-Feld Wahl";
    }

    @Override
    public String getDescription() {
        return "Wähle ein Bingo-Feld zum Abschließen.";
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

        var game = BingoApi.getGameExtended();
        if (game == null) {
            player.sendSystemMessage(Component.literal("§cBingo-API nicht verfügbar!"));
            return false;
        }

        var card = game.getActiveCard();
        if (card == null) {
            player.sendSystemMessage(Component.literal("§cKeine Bingo-Karte vorhanden!"));
            return false;
        }

        // Get incomplete objectives
        var incompleteObjectives = new ArrayList<IBingoObjective>();
        for (var objective : card.getObjectives()) {
            if (!objective.hasAchieved(playerTeam.getId())) {
                incompleteObjectives.add(objective);
            }
        }

        if (incompleteObjectives.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Alle Felder wurden bereits abgeschlossen!"));
            return false;
        }

        // Store pending selection
        pendingSelections.put(player.getUUID(), new PendingSelection(playerTeam.getId(), incompleteObjectives));

        // Show selection menu in chat
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6§l═══════ Wähle ein Feld ═══════"));
        player.sendSystemMessage(Component.literal(""));

        int index = 1;
        for (var objective : incompleteObjectives) {
            String name = objective.getDisplayName() != null ? objective.getDisplayName() : objective.getId();

            Component message = Component.literal("  §e[" + index + "] ")
                    .append(Component.literal(name).withStyle(Style.EMPTY
                            .withColor(ChatFormatting.WHITE)
                            .withClickEvent(new ClickEvent.RunCommand("/backpack perks select " + objective.getId()))
                            .withHoverEvent(new HoverEvent.ShowText(
                                    Component.literal("Klicke um dieses Feld abzuschließen")))));

            player.sendSystemMessage(message);
            index++;
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(
                Component.literal("§7Klicke auf ein Feld oder schreibe §f/backpack perks select <nummer>"));
        player.sendSystemMessage(Component.literal("§6§l════════════════════════════"));

        // Item is consumed when selection is made
        return false; // Don't consume yet
    }

    /**
     * Process a player's selection.
     * Called from command handler.
     */
    public static boolean processSelection(ServerPlayer player, String objectiveId) {
        PendingSelection pending = pendingSelections.remove(player.getUUID());
        if (pending == null) {
            player.sendSystemMessage(Component.literal("§cKeine ausstehende Auswahl vorhanden!"));
            return false;
        }

        // Find the objective
        IBingoObjective selectedObjective = null;

        // Check if it's a number
        try {
            int index = Integer.parseInt(objectiveId) - 1;
            if (index >= 0 && index < pending.objectives.size()) {
                selectedObjective = pending.objectives.get(index);
            }
        } catch (NumberFormatException e) {
            // Not a number, try to find by ID
            for (var obj : pending.objectives) {
                if (obj.getId().equals(objectiveId)) {
                    selectedObjective = obj;
                    break;
                }
            }
        }

        if (selectedObjective == null) {
            player.sendSystemMessage(Component.literal("§cUngültige Auswahl!"));
            return false;
        }

        var scoringService = BingoApi.getScoringService();
        if (scoringService == null) {
            player.sendSystemMessage(Component.literal("§cBingo-API nicht verfügbar!"));
            return false;
        }

        boolean success = scoringService.completeObjective(
                selectedObjective.getId(),
                pending.teamId,
                player.getUUID());

        if (success) {
            String name = selectedObjective.getDisplayName() != null ? selectedObjective.getDisplayName()
                    : selectedObjective.getId();
            player.sendSystemMessage(Component.literal("§a✓ Feld abgeschlossen: §f" + name));

            // Consume item from inventory (find and remove the item)
            consumeItem(player);
            return true;
        } else {
            player.sendSystemMessage(Component.literal("§cFehler beim Abschließen des Feldes!"));
            return false;
        }
    }

    private static void consumeItem(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            var itemOpt = de.yoshlix.bingobackpack.item.BingoItemRegistry.fromItemStack(stack);
            if (itemOpt.isPresent() && itemOpt.get().getId().equals("complete_chosen_bingo_field")) {
                stack.shrink(1);
                return;
            }
        }
    }

    public static boolean hasPendingSelection(UUID playerId) {
        return pendingSelections.containsKey(playerId);
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("Du wählst das Feld!").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public boolean canDropFromMob() {
        return false; // Too powerful for mob drops
    }

    private static class PendingSelection {
        final String teamId;
        final List<IBingoObjective> objectives;

        PendingSelection(String teamId, List<IBingoObjective> objectives) {
            this.teamId = teamId;
            this.objectives = objectives;
        }
    }
}
