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
 * Allows the player to choose and reroll a specific bingo field.
 */
public class RerollChosenField extends BingoItem {

    private static final Map<UUID, PendingReroll> pendingRerolls = new HashMap<>();

    @Override
    public String getId() {
        return "reroll_chosen_field";
    }

    @Override
    public String getName() {
        return "Gezielter Feld-Reroll";
    }

    @Override
    public String getDescription() {
        return "Wähle ein Feld zum Rerolln.";
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

        // Get rerollable objectives (not completed)
        var rerollableFields = new ArrayList<FieldInfo>();
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                var objective = card.getObjective(x, y);
                if (objective != null && !objective.hasAchieved(playerTeam.getId())) {
                    rerollableFields.add(new FieldInfo(x, y, objective));
                }
            }
        }

        if (rerollableFields.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Keine Felder zum Rerolln!"));
            return false;
        }

        // Store pending reroll
        pendingRerolls.put(player.getUUID(), new PendingReroll(rerollableFields));

        // Show selection menu
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6§l═══════ Wähle ein Feld ═══════"));
        player.sendSystemMessage(Component.literal(""));

        int index = 1;
        for (var field : rerollableFields) {
            String name = field.objective.getDisplayName() != null ? field.objective.getDisplayName()
                    : field.objective.getId();

            Component message = Component.literal("  §e[" + index + "] ")
                    .append(Component.literal(name).withStyle(Style.EMPTY
                            .withColor(ChatFormatting.WHITE)
                            .withClickEvent(new ClickEvent.RunCommand("/bingobackpack reroll " + index))
                            .withHoverEvent(new HoverEvent.ShowText(
                                    Component.literal("Klicke um dieses Feld zu rerolln")))));

            player.sendSystemMessage(message);
            index++;
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(
                Component.literal("§7Klicke auf ein Feld oder schreibe §f/bingobackpack reroll <nummer>"));
        player.sendSystemMessage(Component.literal("§6§l════════════════════════════"));

        return false; // Don't consume until selection
    }

    public static boolean processReroll(ServerPlayer player, String selection) {
        PendingReroll pending = pendingRerolls.remove(player.getUUID());
        if (pending == null) {
            player.sendSystemMessage(Component.literal("§cKeine ausstehende Reroll-Auswahl!"));
            return false;
        }

        int index;
        try {
            index = Integer.parseInt(selection) - 1;
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.literal("§cUngültige Auswahl!"));
            return false;
        }

        if (index < 0 || index >= pending.fields.size()) {
            player.sendSystemMessage(Component.literal("§cUngültige Auswahl!"));
            return false;
        }

        FieldInfo field = pending.fields.get(index);

        var cardService = BingoApi.getCardService();
        var game = BingoApi.getGameExtended();

        if (cardService == null || game == null) {
            player.sendSystemMessage(Component.literal("§cBingo-API nicht verfügbar!"));
            return false;
        }

        String oldName = field.objective.getDisplayName() != null ? field.objective.getDisplayName()
                : field.objective.getId();

        boolean success = cardService.rerollTile(null, field.x, field.y, java.util.Set.of());

        if (success) {
            var newCard = game.getActiveCard();
            var newObjective = newCard != null ? newCard.getObjective(field.x, field.y) : null;
            String newName = newObjective != null && newObjective.getDisplayName() != null
                    ? newObjective.getDisplayName()
                    : "Neues Feld";

            player.sendSystemMessage(Component.literal("§a✓ Feld geändert: §c" + oldName + " §a→ §e" + newName));

            ((net.minecraft.server.level.ServerLevel) player.level()).getServer().getPlayerList()
                    .broadcastSystemMessage(
                            Component.literal("§6✦ §e" + player.getName().getString() +
                                    " §6hat ein Feld rerolled: §c" + oldName + " §6→ §e" + newName),
                            false);

            consumeItem(player);
            return true;
        } else {
            player.sendSystemMessage(Component.literal("§cFehler beim Rerolln!"));
            return false;
        }
    }

    private static void consumeItem(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            var itemOpt = de.yoshlix.bingobackpack.item.BingoItemRegistry.fromItemStack(stack);
            if (itemOpt.isPresent() && itemOpt.get().getId().equals("reroll_chosen_field")) {
                stack.shrink(1);
                return;
            }
        }
    }

    public static boolean hasPendingReroll(UUID playerId) {
        return pendingRerolls.containsKey(playerId);
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§7Du wählst welches Feld!"));
    }

    @Override
    public boolean canDropFromMob() {
        return true;
    }

    private static class FieldInfo {
        final int x, y;
        final IBingoObjective objective;

        FieldInfo(int x, int y, IBingoObjective objective) {
            this.x = x;
            this.y = y;
            this.objective = objective;
        }
    }

    private static class PendingReroll {
        final List<FieldInfo> fields;

        PendingReroll(List<FieldInfo> fields) {
            this.fields = fields;
        }
    }
}
