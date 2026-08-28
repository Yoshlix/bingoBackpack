package de.yoshlix.bingobackpack.item.items;

import de.yoshlix.bingobackpack.bingo.BingoBridge;
import de.yoshlix.bingobackpack.item.BingoItem;
import de.yoshlix.bingobackpack.item.ItemRarity;
import me.jfenn.bingo.api.ext.ICardEntryView;
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
        var teams = BingoBridge.getAllTeams();
        if (!BingoBridge.isAvailable()) {
            player.sendSystemMessage(Component.literal("§cKein Bingo-Spiel aktiv!"));
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

        // Only fields the team has not completed yet can be rerolled
        var rerollableFields = new ArrayList<>(
                BingoBridge.getIncompleteEntries(card, playerTeam.getId()));

        if (rerollableFields.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Keine Felder zum Rerolln!"));
            return false;
        }

        // Store pending reroll
        pendingRerolls.put(player.getUUID(), new PendingReroll(playerTeam.getId(), rerollableFields));

        // Show selection menu
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6§l═══════ Wähle ein Feld ═══════"));
        player.sendSystemMessage(Component.literal(""));

        int index = 1;
        for (var field : rerollableFields) {
            String name = BingoBridge.nameOf(field);

            Component message = Component.literal("  §e[" + index + "] ")
                    .append(Component.literal(name).withStyle(Style.EMPTY
                            .withColor(ChatFormatting.WHITE)
                            .withClickEvent(new ClickEvent.RunCommand("/backpack perks reroll " + index))
                            .withHoverEvent(new HoverEvent.ShowText(
                                    Component.literal("Klicke um dieses Feld zu rerolln")))));

            player.sendSystemMessage(message);
            index++;
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(
                Component.literal("§7Klicke auf ein Feld oder schreibe §f/backpack perks reroll <nummer>"));
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

        ICardEntryView field = pending.fields.get(index);

        var card = BingoBridge.getCardForTeam(pending.teamId);
        if (card == null) {
            player.sendSystemMessage(Component.literal("§cBingo-API nicht verfügbar!"));
            return false;
        }

        String oldName = BingoBridge.nameOf(field);

        String newObjectiveId = BingoBridge.rerollEntry(card, field.getX(), field.getY());
        boolean success = newObjectiveId != null;

        if (success) {
            var newCard = BingoBridge.getCardForTeam(pending.teamId);
            String newName = newObjectiveId;
            if (newCard != null) {
                for (var entry : BingoBridge.getEntries(newCard)) {
                    if (entry.getX() == field.getX() && entry.getY() == field.getY()) {
                        newName = BingoBridge.nameOf(entry);
                        break;
                    }
                }
            }

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

    public static void clearPendingRerolls() {
        pendingRerolls.clear();
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

    private static class PendingReroll {
        final String teamId;
        final List<ICardEntryView> fields;

        PendingReroll(String teamId, List<ICardEntryView> fields) {
            this.teamId = teamId;
            this.fields = fields;
        }
    }
}
