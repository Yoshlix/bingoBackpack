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
 * Resets the progress of a specific bingo field.
 * Can target the enemy team's completed fields to uncomplete them.
 */
public class ResetFieldProgress extends BingoItem {

    private static final Map<UUID, PendingReset> pendingResets = new HashMap<>();

    @Override
    public String getId() {
        return "reset_field_progress";
    }

    @Override
    public String getName() {
        return "Fortschritt Zurücksetzen";
    }

    @Override
    public String getDescription() {
        return "Setzt den Fortschritt eines gegnerischen Feldes zurück.";
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
    }

    @Override
    public boolean onUse(ServerPlayer player) {
        var playerTeam = requireTeam(player);
        if (playerTeam == null) {
            return false;
        }

        var teams = BingoBridge.getAllTeams();

        // Find enemy teams and their completed fields. Each team may sit on its own
        // card, so resolve the card per team instead of using a single active card.
        var enemyCompletions = new ArrayList<EnemyCompletion>();

        for (var team : BingoBridge.getEnemyTeams(playerTeam.getId())) {
            var card = BingoBridge.getCardForTeam(team.getId());
            if (card == null) {
                continue;
            }
            for (var entry : BingoBridge.getCompletedEntries(card, team.getId())) {
                enemyCompletions.add(new EnemyCompletion(team.getId(), entry));
            }
        }

        if (enemyCompletions.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6Keine gegnerischen abgeschlossenen Felder!"));
            return false;
        }

        // Store pending reset
        pendingResets.put(player.getUUID(), new PendingReset(enemyCompletions));

        // Show selection menu
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§c§l═══ Gegnerische Felder zurücksetzen ═══"));
        player.sendSystemMessage(Component.literal(""));

        int index = 1;
        for (var completion : enemyCompletions) {
            String name = BingoBridge.nameOf(completion.objective);

            Component message = Component.literal("  §e[" + index + "] §c" + completion.teamId + "§7: ")
                    .append(Component.literal(name).withStyle(Style.EMPTY
                            .withColor(ChatFormatting.WHITE)
                            .withClickEvent(new ClickEvent.RunCommand("/backpack perks reset " + index))
                            .withHoverEvent(new HoverEvent.ShowText(
                                    Component.literal("Klicke um diesen Fortschritt zurückzusetzen")))));

            player.sendSystemMessage(message);
            index++;

            // Limit display: with several enemy teams each sitting on a full
            // card, this list can otherwise flood the chat.
            if (index > 20) {
                player.sendSystemMessage(Component.literal("  §7... und mehr"));
                break;
            }
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7Klicke oder schreibe §f/backpack perks reset <nummer>"));
        player.sendSystemMessage(Component.literal("§c§l════════════════════════════════"));

        return false;
    }

    public static boolean processReset(ServerPlayer player, String selection) {
        // Make sure the item is still there before the effect runs; it may have
        // been moved to the team backpack while the selection was pending.
        if (!requireItemOrWarn(player, "reset_field_progress")) {
            return false;
        }

        PendingReset pending = pendingResets.remove(player.getUUID());
        if (pending == null) {
            player.sendSystemMessage(Component.literal("§cKeine ausstehende Reset-Auswahl!"));
            return false;
        }

        int index;
        try {
            index = Integer.parseInt(selection) - 1;
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.literal("§cUngültige Auswahl!"));
            return false;
        }

        if (index < 0 || index >= pending.completions.size()) {
            player.sendSystemMessage(Component.literal("§cUngültige Auswahl!"));
            return false;
        }

        EnemyCompletion completion = pending.completions.get(index);

        String name = BingoBridge.nameOf(completion.objective);

        boolean success = BingoBridge.uncompleteObjective(
                completion.objective.getObjectiveId(),
                completion.teamId);

        if (success) {
            player.sendSystemMessage(Component.literal("§a✓ Fortschritt zurückgesetzt: §f" + name +
                    " §7(Team: §c" + completion.teamId + "§7)"));

            ((net.minecraft.server.level.ServerLevel) player.level()).getServer().getPlayerList()
                    .broadcastSystemMessage(
                            Component.literal("§c§l⚠ §e" + player.getName().getString() +
                                    " §chat den Fortschritt von §f" + name + " §c(Team: " + completion.teamId
                                    + ") zurückgesetzt!"),
                            false);

            consumeOrWarn(player, "reset_field_progress");
            return true;
        } else {
            player.sendSystemMessage(Component.literal("§cFehler beim Zurücksetzen!"));
            return false;
        }
    }

    public static boolean hasPendingReset(UUID playerId) {
        return pendingResets.containsKey(playerId);
    }

    public static void clearPendingResets() {
        pendingResets.clear();
    }

    @Override
    public List<Component> getExtraLore() {
        return List.of(
                Component.literal("§cSabotiere das gegnerische Team!"));
    }

    @Override
    public boolean canDropFromMob() {
        return true; // Too powerful
    }

    private static class EnemyCompletion {
        final String teamId;
        final ICardEntryView objective;

        EnemyCompletion(String teamId, ICardEntryView objective) {
            this.teamId = teamId;
            this.objective = objective;
        }
    }

    private static class PendingReset {
        final List<EnemyCompletion> completions;

        PendingReset(List<EnemyCompletion> completions) {
            this.completions = completions;
        }
    }
}
